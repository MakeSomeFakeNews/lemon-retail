package com.lemon.erp.modules.channel.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.Queries;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataPermissionRule;
import com.lemon.erp.framework.security.datascope.DataScopes;
import com.lemon.erp.modules.channel.client.WxShopClient;
import com.lemon.erp.modules.channel.dto.WxAftersaleQuery;
import com.lemon.erp.modules.channel.dto.WxSyncResult;
import com.lemon.erp.modules.channel.entity.WxAftersale;
import com.lemon.erp.modules.channel.entity.WxOrder;
import com.lemon.erp.modules.channel.entity.WxOrderItem;
import com.lemon.erp.modules.channel.entity.WxShop;
import com.lemon.erp.modules.channel.mapper.WxAftersaleMapper;
import com.lemon.erp.modules.channel.mapper.WxOrderItemMapper;
import com.lemon.erp.modules.channel.mapper.WxOrderMapper;
import com.lemon.erp.modules.channel.model.WxAftersaleErpStatus;
import com.lemon.erp.modules.channel.model.WxAftersaleStatus;
import com.lemon.erp.modules.channel.model.WxAftersaleType;
import com.lemon.erp.modules.channel.model.WxErpStatus;
import com.lemon.erp.modules.scm.dto.TradeDocItemRequest;
import com.lemon.erp.modules.scm.dto.TradeDocRequest;
import com.lemon.erp.modules.scm.model.TradeKind;
import com.lemon.erp.modules.scm.service.ScmTradeDocService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 售后单：同步、同意 / 拒绝 / 确认收货（调用小店接口后重新拉取），以及退款完成后在 ERP 中的处理（{@link #process}）：
 * <ul>
 *   <li>ERP 还没有出库：退款的数量计入渠道订单明细的「已退款」，不再出库；全部商品都退款时关闭销售订单；</li>
 *   <li>ERP 已出库的退货退款：按退货数量生成销售退货（退款，不补发），退回店铺的发货仓库；</li>
 *   <li>ERP 已出库的仅退款：只记录（不影响库存）；</li>
 *   <li>线上已发货而 ERP 还没有出库：等待出库后重试（同步时自动重试）。</li>
 * </ul>
 * 退款金额只记录在售后单上，退款资金由小店结算，不生成收付款
 */
@Slf4j
@Service
public class WxAftersaleService {

    static final DataPermissionRule RULE = new DataPermissionRule("ch_wx_aftersale", null, null, "store_id", null);
    private static final int INITIAL_DAYS = 30;
    private static final int OVERLAP_MINUTES = 10;
    private static final int REFRESH_LIMIT = 200;

    private final WxAftersaleMapper aftersaleMapper;
    private final WxOrderMapper orderMapper;
    private final WxOrderItemMapper itemMapper;
    private final WxShopService shopService;
    private final WxOrderService orderService;
    private final ScmTradeDocService tradeDocService;
    private final TransactionTemplate tx;
    private final TransactionTemplate txNew;

    public WxAftersaleService(WxAftersaleMapper aftersaleMapper, WxOrderMapper orderMapper, WxOrderItemMapper itemMapper,
                              WxShopService shopService, WxOrderService orderService, ScmTradeDocService tradeDocService,
                              PlatformTransactionManager transactionManager) {
        this.aftersaleMapper = aftersaleMapper;
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.shopService = shopService;
        this.orderService = orderService;
        this.tradeDocService = tradeDocService;
        this.tx = new TransactionTemplate(transactionManager);
        this.txNew = new TransactionTemplate(transactionManager);
        this.txNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @DataPermission(table = "ch_wx_aftersale", deptColumn = "", userColumn = "", storeColumn = "store_id")
    public PageResult<WxAftersale> page(long current, long size, WxAftersaleQuery query) {
        Page<WxAftersale> page = Page.of(current, size);
        aftersaleMapper.selectPageList(page, query);
        return PageResult.of(page);
    }

    public WxAftersale detail(Long id) {
        return DataScopes.require(RULE, () -> aftersaleMapper.selectDetail(id), "channel.error.aftersaleNotFound");
    }

    private WxAftersale getAccessible(Long id) {
        return DataScopes.require(RULE, () -> aftersaleMapper.selectById(id), "channel.error.aftersaleNotFound");
    }

    // ---------------------------------------------------------------- 同步

    /**
     * 增量同步：按申请时间拉取新的售后单，并刷新本地未结束的售后单（状态变化不一定在时间范围内）和处理失败的售后单
     */
    WxSyncResult syncAftersales(WxShop shop) {
        WxShopClient client = shopService.client(shop);
        WxShopClient.Credentials c = WxShopService.credentials(shop);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = shop.getAftersaleSyncedTo() == null ? now.minusDays(INITIAL_DAYS)
                : shop.getAftersaleSyncedTo().minusMinutes(OVERLAP_MINUTES);
        Set<String> ids = new LinkedHashSet<>();
        try {
            for (LocalDateTime a = from; a.isBefore(now); a = a.plusDays(1)) {
                LocalDateTime b = a.plusDays(1).isBefore(now) ? a.plusDays(1) : now;
                String next = null;
                do {
                    WxShopClient.Page<String> page = client.listAftersaleIds(c, a, b, next);
                    ids.addAll(page.items());
                    next = page.hasMore() ? page.nextKey() : null;
                } while (next != null);
            }
        } catch (RuntimeException e) {
            log.warn("微信小店 {} 售后单列表拉取失败", shop.getId(), e);
            return new WxSyncResult(0, 0, 0, false, WxErrors.text(e));
        }
        Queries.limit(aftersaleMapper, Wrappers.<WxAftersale>lambdaQuery().select(WxAftersale::getAftersaleId)
                        .eq(WxAftersale::getShopId, shop.getId())
                        .and(w -> w.notIn(WxAftersale::getStatus, WxAftersaleStatus.REFUNDED, WxAftersaleStatus.REJECTED, WxAftersaleStatus.CANCELLED)
                                .or().eq(WxAftersale::getErpStatus, WxAftersaleErpStatus.FAILED))
                        .orderByAsc(WxAftersale::getId), REFRESH_LIMIT)
                .forEach(a -> ids.add(a.getAftersaleId()));
        int failed = 0;
        String firstError = null;
        for (String id : ids) {
            try {
                syncOne(shop, id);
            } catch (RuntimeException e) {
                failed++;
                firstError = firstError == null ? WxErrors.text(e) : firstError;
                log.warn("微信小店售后单 {} 同步失败", id, e);
            }
        }
        if (failed == 0) {
            shopService.markAftersaleSynced(shop.getId(), now);
        }
        return new WxSyncResult(ids.size(), ids.size() - failed, failed, true, firstError);
    }

    /** 拉取单个售后单并处理（关联的订单本地没有时先同步订单），返回售后单 ID */
    Long syncOne(WxShop shop, String aftersaleId) {
        WxShopClient.Aftersale src = shopService.client(shop).getAftersale(WxShopService.credentials(shop), aftersaleId);
        WxOrder order = src.orderId() == null ? null : orderService.findByOrderId(shop.getId(), src.orderId());
        if (order == null && src.orderId() != null) {
            orderService.syncOne(shop, src.orderId());
            order = orderService.findByOrderId(shop.getId(), src.orderId());
        }
        WxOrder linked = order;
        Long id;
        try {
            id = tx.execute(s -> save(shop, src, linked));
        } catch (DuplicateKeyException e) {
            id = tx.execute(s -> save(shop, src, linked));
        }
        process(id, false);
        return id;
    }

    private Long save(WxShop shop, WxShopClient.Aftersale src, WxOrder order) {
        WxAftersale old = aftersaleMapper.selectOne(Wrappers.<WxAftersale>lambdaQuery().eq(WxAftersale::getShopId, shop.getId())
                .eq(WxAftersale::getAftersaleId, src.aftersaleId()));
        WxAftersale a = old != null ? old : new WxAftersale();
        WxAftersaleStatus status = WxAftersaleStatus.ofPlatform(src.status());
        a.setStatus(status != null ? status : old != null ? old.getStatus() : WxAftersaleStatus.PENDING);
        a.setPlatformStatus(cut(src.status(), 64));
        a.setType(WxAftersaleType.ofPlatform(src.type()));
        a.setOrderId(src.orderId());
        a.setWxOrderId(order == null ? null : order.getId());
        a.setProductId(src.productId());
        a.setSkuId(src.skuId());
        a.setQty(src.qty());
        a.setRefundAmount(Decimals.nz(src.refundAmount()));
        a.setReason(cut(src.reason(), 255));
        a.setApplyTime(src.createTime());
        a.setPlatformUpdateTime(src.updateTime());
        a.setSyncTime(LocalDateTime.now());
        if (order != null && src.skuId() != null) {
            itemMapper.selectList(Wrappers.<WxOrderItem>lambdaQuery().eq(WxOrderItem::getWxOrderId, order.getId())
                    .eq(WxOrderItem::getSkuId, src.skuId())).stream().findFirst().ifPresent(i -> a.setTitle(i.getTitle()));
        }
        if (old == null) {
            a.setShopId(shop.getId());
            a.setStoreId(shop.getStoreId());
            a.setAftersaleId(src.aftersaleId());
            a.setErpStatus(WxAftersaleErpStatus.NONE);
            aftersaleMapper.insert(a);
        } else {
            aftersaleMapper.updateById(a);
        }
        return a.getId();
    }

    // ---------------------------------------------------------------- ERP 处理

    /**
     * 退款完成的售后在 ERP 中处理一次，失败时记录错误（可重试）
     *
     * @param manual 手工重试：失败时抛出
     */
    void process(Long id, boolean manual) {
        try {
            WxSystemIdentity.run(() -> tx.executeWithoutResult(s -> processInTx(id)));
        } catch (RuntimeException e) {
            log.warn("售后单 {} 处理失败", id, e);
            String error = WxErrors.text(e);
            txNew.executeWithoutResult(s -> aftersaleMapper.update(Wrappers.<WxAftersale>lambdaUpdate()
                    .set(WxAftersale::getErpStatus, WxAftersaleErpStatus.FAILED).set(WxAftersale::getErrorMsg, error)
                    .eq(WxAftersale::getId, id)));
            if (manual) {
                throw e;
            }
        }
    }

    private void processInTx(Long id) {
        WxAftersale a = aftersaleMapper.selectById(id);
        if (a.getStatus() != WxAftersaleStatus.REFUNDED || a.getErpStatus() == WxAftersaleErpStatus.DONE
                || a.getErpStatus() == WxAftersaleErpStatus.SKIPPED) {
            return;
        }
        if (a.getWxOrderId() == null) {
            throw new BizException("channel.error.aftersaleOrderMissing", a.getAftersaleId());
        }
        // 与订单的同步、发货串行
        orderMapper.lockById(a.getWxOrderId());
        a = aftersaleMapper.selectById(id);
        if (a.getErpStatus() == WxAftersaleErpStatus.DONE || a.getErpStatus() == WxAftersaleErpStatus.SKIPPED) {
            return;
        }
        WxOrder order = orderMapper.selectById(a.getWxOrderId());
        String skuId = a.getSkuId();
        WxOrderItem item = orderService.items(order.getId()).stream().filter(i -> i.getSkuId().equals(skuId)).findFirst()
                .orElseThrow(() -> new BizException("channel.error.aftersaleItemMissing", skuId));
        if (order.getSalesOutId() != null) {
            if (a.getType() == WxAftersaleType.RETURN) {
                salesReturn(a, order, item);
            } else {
                finish(id, WxAftersaleErpStatus.SKIPPED, Messages.get(Messages.DEFAULT_LOCALE, "channel.text.refundAfterShipRecorded"));
            }
            return;
        }
        if (order.getStatus().shipped()) {
            // 线上已发货，ERP 出库后再处理（同步时自动重试）
            throw new BizException("channel.error.aftersaleWaitOutbound", order.getOrderId());
        }
        // 发货前退款：不再出库这部分数量，全部退款时关闭销售订单
        int refunded = Math.min(item.getQty(), (item.getRefundedQty() == null ? 0 : item.getRefundedQty()) + a.getQty());
        itemMapper.update(Wrappers.<WxOrderItem>lambdaUpdate().set(WxOrderItem::getRefundedQty, refunded).eq(WxOrderItem::getId, item.getId()));
        boolean all = orderService.items(order.getId()).stream().allMatch(i -> WxOrderService.remaining(i) == 0);
        if (all && order.getErpStatus() != WxErpStatus.CLOSED) {
            orderService.closeErpOrder(order);
            orderMapper.update(Wrappers.<WxOrder>lambdaUpdate().set(WxOrder::getErpStatus, WxErpStatus.CLOSED).set(WxOrder::getErrorMsg, null)
                    .eq(WxOrder::getId, order.getId()));
        }
        finish(id, WxAftersaleErpStatus.DONE, null);
    }

    /** 已出库的退货退款：生成销售退货（退款，不补发） */
    private void salesReturn(WxAftersale a, WxOrder order, WxOrderItem item) {
        if (item.getErpOrderItemId() == null) {
            throw new BizException("channel.error.aftersaleItemMissing", item.getSkuId());
        }
        WxShop shop = shopService.get(order.getShopId());
        BigDecimal qty = Decimals.qty(BigDecimal.valueOf(a.getQty()).multiply(WxOrderService.ratio(item.getUnitRatio())));
        String remark = cut(Messages.get(Messages.DEFAULT_LOCALE, "channel.text.returnRemark", a.getAftersaleId(), order.getOrderId()), 255);
        Long docId = tradeDocService.create(TradeKind.SALES_RETURN, new TradeDocRequest(order.getErpOrderId(), shop.getWarehouseId(),
                LocalDate.now(), remark, false, List.of(new TradeDocItemRequest(item.getErpOrderItemId(), null, null, null, null, qty, null, null))));
        String docNo = tradeDocService.detail(TradeKind.SALES_RETURN, docId).getDocNo();
        aftersaleMapper.update(Wrappers.<WxAftersale>lambdaUpdate()
                .set(WxAftersale::getSalesReturnId, docId)
                .set(WxAftersale::getSalesReturnNo, docNo)
                .set(WxAftersale::getErpStatus, WxAftersaleErpStatus.DONE)
                .set(WxAftersale::getErrorMsg, null)
                .eq(WxAftersale::getId, a.getId()));
    }

    private void finish(Long id, WxAftersaleErpStatus status, String note) {
        aftersaleMapper.update(Wrappers.<WxAftersale>lambdaUpdate().set(WxAftersale::getErpStatus, status).set(WxAftersale::getErrorMsg, note)
                .eq(WxAftersale::getId, id));
    }

    // ---------------------------------------------------------------- 手工操作（调用小店接口后重新拉取）

    /** 同意：仅退款即退款，退货退款为同意退货 */
    public void accept(Long id) {
        WxAftersale a = getAccessible(id);
        if (a.getStatus() != WxAftersaleStatus.PENDING) {
            throw new BizException("channel.error.aftersaleNotPending", a.getAftersaleId());
        }
        WxShop shop = shopService.get(a.getShopId());
        shopService.client(shop).acceptAftersale(WxShopService.credentials(shop), a.getAftersaleId());
        syncOne(shop, a.getAftersaleId());
    }

    public void reject(Long id, String reason) {
        WxAftersale a = getAccessible(id);
        if (a.getStatus() != WxAftersaleStatus.PENDING) {
            throw new BizException("channel.error.aftersaleNotPending", a.getAftersaleId());
        }
        WxShop shop = shopService.get(a.getShopId());
        shopService.client(shop).rejectAftersale(WxShopService.credentials(shop), a.getAftersaleId(), reason.trim());
        syncOne(shop, a.getAftersaleId());
    }

    /** 退货退款：确认收到退货并退款，退款完成后生成销售退货 */
    public void confirmReceived(Long id) {
        WxAftersale a = getAccessible(id);
        if (a.getType() != WxAftersaleType.RETURN || a.getStatus() != WxAftersaleStatus.WAIT_RECEIPT) {
            throw new BizException("channel.error.aftersaleNotWaitReceipt", a.getAftersaleId());
        }
        WxShop shop = shopService.get(a.getShopId());
        shopService.client(shop).confirmReturnReceived(WxShopService.credentials(shop), a.getAftersaleId());
        syncOne(shop, a.getAftersaleId());
    }

    /** 从小店重新拉取 */
    public void refresh(Long id) {
        WxAftersale a = getAccessible(id);
        syncOne(shopService.get(a.getShopId()), a.getAftersaleId());
    }

    /** 重新处理失败的售后 */
    public void retry(Long id) {
        WxAftersale a = getAccessible(id);
        if (a.getErpStatus() != WxAftersaleErpStatus.FAILED) {
            throw new BizException("channel.error.aftersaleNotRetryable", a.getAftersaleId());
        }
        process(id, true);
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
