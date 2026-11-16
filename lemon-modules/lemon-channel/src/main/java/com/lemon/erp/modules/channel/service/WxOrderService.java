package com.lemon.erp.modules.channel.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.Queries;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataPermissionRule;
import com.lemon.erp.framework.security.datascope.DataScopes;
import com.lemon.erp.modules.channel.client.WxShopClient;
import com.lemon.erp.modules.channel.dto.WxOrderQuery;
import com.lemon.erp.modules.channel.dto.WxShopRequests;
import com.lemon.erp.modules.channel.dto.WxSyncResult;
import com.lemon.erp.modules.channel.entity.WxAftersale;
import com.lemon.erp.modules.channel.entity.WxOrder;
import com.lemon.erp.modules.channel.entity.WxOrderItem;
import com.lemon.erp.modules.channel.entity.WxProduct;
import com.lemon.erp.modules.channel.entity.WxShop;
import com.lemon.erp.modules.channel.mapper.WxAftersaleMapper;
import com.lemon.erp.modules.channel.mapper.WxOrderItemMapper;
import com.lemon.erp.modules.channel.mapper.WxOrderMapper;
import com.lemon.erp.modules.channel.model.WxDeliveryStatus;
import com.lemon.erp.modules.channel.model.WxErpStatus;
import com.lemon.erp.modules.channel.model.WxOrderStatus;
import com.lemon.erp.modules.scm.dto.OrderItemRequest;
import com.lemon.erp.modules.scm.dto.OrderRequest;
import com.lemon.erp.modules.scm.dto.TradeDocItemRequest;
import com.lemon.erp.modules.scm.dto.TradeDocRequest;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.OrderSource;
import com.lemon.erp.modules.scm.model.TradeKind;
import com.lemon.erp.modules.scm.service.ScmOrderService;
import com.lemon.erp.modules.scm.service.ScmTradeDocService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 渠道订单：同步（增量拉取 + 消息推送触发单个订单）、在 ERP 中的处理、发货。
 * <p>
 * 处理规则（{@link #process}，同一订单加行锁串行）：
 * <ul>
 *   <li>未付款：只保存，不处理；</li>
 *   <li>已付款、还没有销售订单：全部商品已映射时，按实付单价（订单优惠按售价金额比例分摊，不含运费）生成销售订单（客户为店铺的客户、仓库为店铺的发货仓库，
 *       来源 WX_SHOP）并直接审核；有未映射的商品时为「待映射」，映射后重试（同步时也会自动重试）；店铺未开启自动生成时为「待生成」，手工生成；</li>
 *   <li>在 ERP 中发货（{@link #ship}）：生成销售出库（发货前退款的数量不出库，剩余数量关闭订单），提交后回传快递公司和运单号，失败可重试；</li>
 *   <li>在小店后台发货（线上状态已发货而 ERP 还没有出库）：自动生成销售出库；</li>
 *   <li>发货前取消：关闭销售订单。</li>
 * </ul>
 * 调用小店接口都在事务之外；写库在 TransactionTemplate 中，每个订单一个事务，一个失败不影响其他（错误记录在订单上）
 */
@Slf4j
@Service
public class WxOrderService {

    /** 订单的门店数据权限（店铺绑定的门店，写入时确定） */
    static final DataPermissionRule RULE = new DataPermissionRule("ch_wx_order", null, null, "store_id", null);
    /** 查看完整收货信息的权限 */
    public static final String SENSITIVE_PERM = "channel:wxOrder:sensitive";
    /** 首次同步拉取最近几天的订单 */
    private static final int INITIAL_DAYS = 7;
    /** 增量拉取向前重叠的分钟数（线上更新时间与本地时钟的误差、分页期间的更新） */
    private static final int OVERLAP_MINUTES = 10;
    /** 每次自动重试的订单数上限 */
    private static final int RETRY_LIMIT = 100;

    private final WxOrderMapper orderMapper;
    private final WxOrderItemMapper itemMapper;
    private final WxAftersaleMapper aftersaleMapper;
    private final WxShopService shopService;
    private final WxProductService productService;
    private final ScmOrderService scmOrderService;
    private final ScmTradeDocService tradeDocService;
    private final PermissionService permissionService;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate tx;
    private final TransactionTemplate txNew;

    public WxOrderService(WxOrderMapper orderMapper, WxOrderItemMapper itemMapper, WxAftersaleMapper aftersaleMapper,
                          WxShopService shopService, WxProductService productService, ScmOrderService scmOrderService,
                          ScmTradeDocService tradeDocService, PermissionService permissionService,
                          ApplicationEventPublisher eventPublisher, PlatformTransactionManager transactionManager) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.aftersaleMapper = aftersaleMapper;
        this.shopService = shopService;
        this.productService = productService;
        this.scmOrderService = scmOrderService;
        this.tradeDocService = tradeDocService;
        this.permissionService = permissionService;
        this.eventPublisher = eventPublisher;
        this.tx = new TransactionTemplate(transactionManager);
        this.txNew = new TransactionTemplate(transactionManager);
        this.txNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // ---------------------------------------------------------------- 查询

    /** 列表不含收货信息 */
    @DataPermission(table = "ch_wx_order", deptColumn = "", userColumn = "", storeColumn = "store_id")
    public PageResult<WxOrder> page(long current, long size, WxOrderQuery query) {
        Page<WxOrder> page = Page.of(current, size);
        orderMapper.selectPageList(page, query);
        return PageResult.of(page);
    }

    /** 详情：明细、售后单；收货信息脱敏，有 {@value #SENSITIVE_PERM} 权限时显示完整信息 */
    public WxOrder detail(Long id) {
        WxOrder o = DataScopes.require(RULE, () -> orderMapper.selectDetail(id), "channel.error.orderNotFound");
        o.setItems(itemMapper.selectByOrder(id, o.getShopId()));
        o.setAftersales(aftersaleMapper.selectList(Wrappers.<WxAftersale>lambdaQuery().eq(WxAftersale::getWxOrderId, id)
                .orderByAsc(WxAftersale::getId)));
        boolean visible = permissionService.hasPerm(SENSITIVE_PERM);
        o.setSensitiveVisible(visible);
        if (!visible) {
            o.setReceiverName(WxMasks.name(o.getReceiverName()));
            o.setReceiverPhone(WxMasks.phone(o.getReceiverPhone()));
            o.setReceiverAddress(WxMasks.address(o.getReceiverAddress()));
        }
        return o;
    }

    /** 在门店数据权限范围内的订单 */
    WxOrder getAccessible(Long id) {
        return DataScopes.require(RULE, () -> orderMapper.selectById(id), "channel.error.orderNotFound");
    }

    WxOrder findByOrderId(Long shopId, String orderId) {
        return orderMapper.selectOne(Wrappers.<WxOrder>lambdaQuery().eq(WxOrder::getShopId, shopId).eq(WxOrder::getOrderId, orderId));
    }

    // ---------------------------------------------------------------- 同步

    /**
     * 增量同步：从上次同步到的时间（向前重叠 {@value #OVERLAP_MINUTES} 分钟，首次为最近 {@value #INITIAL_DAYS} 天）按线上更新时间分段（每段一天）分页拉取；
     * 全部拉取成功才推进同步时间。最后自动重试待映射、处理失败的订单
     */
    WxSyncResult syncOrders(WxShop shop) {
        WxShopClient client = shopService.client(shop);
        WxShopClient.Credentials c = WxShopService.credentials(shop);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = shop.getOrderSyncedTo() == null ? now.minusDays(INITIAL_DAYS) : shop.getOrderSyncedTo().minusMinutes(OVERLAP_MINUTES);
        int total = 0;
        int failed = 0;
        String firstError = null;
        try {
            for (LocalDateTime a = from; a.isBefore(now); a = a.plusDays(1)) {
                LocalDateTime b = a.plusDays(1).isBefore(now) ? a.plusDays(1) : now;
                String next = null;
                do {
                    WxShopClient.Page<String> page = client.listOrderIds(c, a, b, next);
                    for (String orderId : page.items()) {
                        total++;
                        try {
                            syncOne(shop, orderId);
                        } catch (RuntimeException e) {
                            failed++;
                            firstError = firstError == null ? WxErrors.text(e) : firstError;
                            log.warn("微信小店订单 {} 同步失败", orderId, e);
                        }
                    }
                    next = page.hasMore() ? page.nextKey() : null;
                } while (next != null);
            }
        } catch (RuntimeException e) {
            log.warn("微信小店 {} 订单列表拉取失败", shop.getId(), e);
            return new WxSyncResult(total, total - failed, failed, false, WxErrors.text(e));
        }
        if (failed == 0) {
            shopService.markOrderSynced(shop.getId(), now);
        }
        retryPending(shop);
        return new WxSyncResult(total, total - failed, failed, true, firstError);
    }

    /** 拉取单个订单并处理（消息推送、手工刷新也调用），返回渠道订单 ID */
    Long syncOne(WxShop shop, String orderId) {
        WxShopClient.Order order = shopService.client(shop).getOrder(WxShopService.credentials(shop), orderId);
        Long id;
        try {
            id = tx.execute(s -> save(shop, order));
        } catch (DuplicateKeyException e) {
            // 并发同步同一订单：另一方已插入，按更新处理
            id = tx.execute(s -> save(shop, order));
        }
        process(id, false);
        return id;
    }

    /** 待映射、处理失败（映射后、错误修复后自动重试），以及已在线上发货 / 取消但还没有出库 / 关闭的订单 */
    private void retryPending(WxShop shop) {
        List<WxOrder> list = Queries.limit(orderMapper, Wrappers.<WxOrder>lambdaQuery().select(WxOrder::getId)
                .eq(WxOrder::getShopId, shop.getId())
                .and(w -> w.in(WxOrder::getErpStatus, WxErpStatus.UNMAPPED, WxErpStatus.FAILED)
                        .or(x -> x.eq(WxOrder::getErpStatus, WxErpStatus.ORDERED)
                                .in(WxOrder::getStatus, WxOrderStatus.SHIPPED, WxOrderStatus.COMPLETED, WxOrderStatus.CANCELLED)))
                .orderByAsc(WxOrder::getId), RETRY_LIMIT);
        list.forEach(o -> process(o.getId(), false));
    }

    /** 保存线上订单（新增或更新），明细只在新增时写入 */
    private Long save(WxShop shop, WxShopClient.Order src) {
        WxOrder old = findByOrderId(shop.getId(), src.orderId());
        WxOrder o = old != null ? old : new WxOrder();
        WxOrderStatus status = WxOrderStatus.ofPlatform(src.status());
        o.setStatus(status != null ? status : old != null ? old.getStatus() : WxOrderStatus.UNPAID);
        o.setPlatformStatus(src.status());
        o.setBuyer(WxMasks.buyer(src.buyer()));
        if (src.receiver() != null) {
            o.setReceiverName(cut(src.receiver().name(), 64));
            o.setReceiverPhone(cut(src.receiver().phone(), 32));
            o.setReceiverAddress(cut(src.receiver().address(), 255));
        }
        o.setGoodsAmount(Decimals.nz(src.goodsAmount()));
        o.setFreight(Decimals.nz(src.freight()));
        o.setDiscountAmount(Decimals.nz(src.discount()));
        o.setPaidAmount(Decimals.nz(src.paidAmount()));
        o.setOrderTime(src.createTime());
        o.setPayTime(src.payTime());
        o.setPlatformUpdateTime(src.updateTime());
        o.setSyncTime(LocalDateTime.now());
        WxShopClient.Shipment ship = src.shipment();
        if (ship != null) {
            if (o.getDeliveryStatus() == null || o.getDeliveryStatus() == WxDeliveryStatus.NONE) {
                // 在小店后台发货
                o.setDeliveryStatus(WxDeliveryStatus.PLATFORM);
                o.setDeliveryCompanyId(cut(ship.companyId(), 32));
                o.setDeliveryCompanyName(cut(ship.companyName(), 64));
                o.setWaybillNo(cut(ship.waybillNo(), 64));
                o.setShipTime(ship.shipTime());
            } else if (o.getDeliveryStatus() == WxDeliveryStatus.PUSHING || o.getDeliveryStatus() == WxDeliveryStatus.FAILED) {
                // 回传结果未知但线上已有发货信息
                o.setDeliveryStatus(WxDeliveryStatus.PUSHED);
            }
        }
        if (old == null) {
            o.setShopId(shop.getId());
            o.setStoreId(shop.getStoreId());
            o.setOrderId(src.orderId());
            o.setErpStatus(WxErpStatus.NONE);
            if (o.getDeliveryStatus() == null) {
                o.setDeliveryStatus(WxDeliveryStatus.NONE);
            }
            orderMapper.insert(o);
            List<WxOrderItem> items = toItems(o.getId(), src);
            if (!items.isEmpty()) {
                itemMapper.insertBatch(items);
            }
        } else {
            orderMapper.updateById(o);
        }
        return o.getId();
    }

    /** 明细：线上返回了每行实付时直接使用，否则把（实付 − 运费）按售价金额比例分摊，最后一行取余数 */
    static List<WxOrderItem> toItems(Long wxOrderId, WxShopClient.Order src) {
        List<WxShopClient.OrderLine> lines = src.lines();
        boolean linePaid = !lines.isEmpty() && lines.stream().allMatch(l -> l.paidAmount() != null);
        BigDecimal goodsPaid = Decimals.nz(src.paidAmount()).subtract(Decimals.nz(src.freight())).max(BigDecimal.ZERO);
        BigDecimal base = lines.stream().map(l -> Decimals.nz(l.salePrice()).multiply(BigDecimal.valueOf(l.qty()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<WxOrderItem> items = new ArrayList<>(lines.size());
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < lines.size(); i++) {
            WxShopClient.OrderLine l = lines.get(i);
            BigDecimal gross = Decimals.nz(l.salePrice()).multiply(BigDecimal.valueOf(l.qty()));
            BigDecimal paid;
            if (linePaid) {
                paid = Decimals.money(l.paidAmount());
            } else if (i == lines.size() - 1) {
                paid = goodsPaid.subtract(allocated).max(BigDecimal.ZERO);
            } else {
                paid = base.signum() == 0 ? BigDecimal.ZERO : goodsPaid.multiply(gross).divide(base, 2, RoundingMode.HALF_UP);
                allocated = allocated.add(paid);
            }
            WxOrderItem item = new WxOrderItem();
            item.setWxOrderId(wxOrderId);
            item.setProductId(l.productId());
            item.setSkuId(l.skuId());
            item.setTitle(cut(l.title(), 255));
            item.setSkuAttrs(cut(l.attrs(), 255));
            item.setQty(l.qty());
            item.setSalePrice(Decimals.money(Decimals.nz(l.salePrice())));
            item.setPaidAmount(paid);
            item.setRefundedQty(0);
            items.add(item);
        }
        return items;
    }

    // ---------------------------------------------------------------- ERP 处理

    /**
     * 按订单的线上状态在 ERP 中处理，失败时把错误记录在订单上（还没有销售订单的为「处理失败」）
     *
     * @param manual 手工操作：未开启自动生成的店铺也生成销售订单，失败时抛出
     */
    void process(Long id, boolean manual) {
        try {
            WxSystemIdentity.run(() -> tx.executeWithoutResult(s -> processInTx(id, manual)));
        } catch (RuntimeException e) {
            log.warn("渠道订单 {} 处理失败", id, e);
            String error = WxErrors.text(e);
            txNew.executeWithoutResult(s -> {
                WxOrder o = orderMapper.selectById(id);
                orderMapper.update(Wrappers.<WxOrder>lambdaUpdate()
                        .set(o != null && o.getErpOrderId() == null, WxOrder::getErpStatus, WxErpStatus.FAILED)
                        .set(WxOrder::getErrorMsg, error)
                        .eq(WxOrder::getId, id));
            });
            if (manual) {
                throw e;
            }
        }
    }

    private void processInTx(Long id, boolean manual) {
        orderMapper.lockById(id);
        WxOrder o = orderMapper.selectById(id);
        WxShop shop = shopService.get(o.getShopId());
        List<WxOrderItem> items = items(id);
        if (o.getStatus() == WxOrderStatus.UNPAID) {
            return;
        }
        if (o.getStatus() == WxOrderStatus.CANCELLED) {
            if (o.getSalesOutId() == null && o.getErpStatus() != WxErpStatus.CLOSED) {
                closeErpOrder(o);
                updateErp(id, WxErpStatus.CLOSED);
            }
            return;
        }
        if (o.getErpOrderId() == null) {
            if (items.stream().allMatch(i -> remaining(i) == 0)) {
                updateErp(id, WxErpStatus.CLOSED);
                return;
            }
            Map<String, WxProduct> mapping = productService.mappedSkus(shop.getId());
            boolean allMapped = items.stream().filter(i -> remaining(i) > 0)
                    .allMatch(i -> mapping.containsKey(WxProductService.key(i.getProductId(), i.getSkuId())));
            if (!allMapped) {
                updateErp(id, WxErpStatus.UNMAPPED);
                return;
            }
            if (!manual && !Boolean.TRUE.equals(shop.getAutoCreateOrder()) && o.getErpStatus() != WxErpStatus.FAILED) {
                updateErp(id, WxErpStatus.PENDING);
                return;
            }
            createErpOrder(o, shop, items, mapping);
            o = orderMapper.selectById(id);
            items = items(id);
        }
        if (o.getStatus().shipped() && o.getSalesOutId() == null && o.getErpStatus() == WxErpStatus.ORDERED) {
            createSalesOut(o, shop, items);
        }
    }

    /** 按实付单价生成销售订单并直接审核 */
    private void createErpOrder(WxOrder o, WxShop shop, List<WxOrderItem> items, Map<String, WxProduct> mapping) {
        List<WxOrderItem> included = items.stream().filter(i -> remaining(i) > 0).toList();
        List<OrderItemRequest> lines = new ArrayList<>(included.size());
        for (WxOrderItem i : included) {
            WxProduct p = mapping.get(WxProductService.key(i.getProductId(), i.getSkuId()));
            BigDecimal ratio = ratio(p.getUnitRatio());
            BigDecimal piecePrice = i.getQty() == 0 ? BigDecimal.ZERO
                    : Decimals.nz(i.getPaidAmount()).divide(BigDecimal.valueOf(i.getQty()), 6, RoundingMode.HALF_UP);
            BigDecimal price = piecePrice.divide(ratio, Decimals.PRICE_SCALE, RoundingMode.HALF_UP);
            lines.add(new OrderItemRequest(p.getMaterialId(), Decimals.qty(BigDecimal.valueOf(remaining(i)).multiply(ratio)), price, null, null,
                    cut(i.getTitle(), 255)));
        }
        LocalDate today = LocalDate.now();
        String remark = cut(Messages.get(Messages.DEFAULT_LOCALE, "channel.text.orderRemark", o.getOrderId()), 255);
        Long erpId = scmOrderService.createExternalApproved(OrderKind.SALES,
                new OrderRequest(shop.getCustomerId(), today, today, shop.getWarehouseId(), remark, lines), OrderSource.WX_SHOP);
        ScmOrder erp = scmOrderService.detail(OrderKind.SALES, erpId);
        List<ScmOrderItem> erpItems = erp.getItems();
        for (int k = 0; k < included.size(); k++) {
            WxOrderItem i = included.get(k);
            WxProduct p = mapping.get(WxProductService.key(i.getProductId(), i.getSkuId()));
            itemMapper.update(Wrappers.<WxOrderItem>lambdaUpdate()
                    .set(WxOrderItem::getMaterialId, p.getMaterialId())
                    .set(WxOrderItem::getUnitRatio, ratio(p.getUnitRatio()))
                    .set(WxOrderItem::getErpOrderItemId, erpItems.get(k).getId())
                    .eq(WxOrderItem::getId, i.getId()));
        }
        orderMapper.update(Wrappers.<WxOrder>lambdaUpdate()
                .set(WxOrder::getErpOrderId, erpId)
                .set(WxOrder::getErpOrderNo, erp.getOrderNo())
                .set(WxOrder::getErpStatus, WxErpStatus.ORDERED)
                .set(WxOrder::getErrorMsg, null)
                .eq(WxOrder::getId, o.getId()));
    }

    /** 销售出库：发货前退款的数量不出库，有退款时出库后关闭订单；全部退款时直接关闭 */
    private void createSalesOut(WxOrder o, WxShop shop, List<WxOrderItem> items) {
        List<TradeDocItemRequest> lines = new ArrayList<>();
        boolean refunded = false;
        for (WxOrderItem i : items) {
            refunded |= i.getRefundedQty() != null && i.getRefundedQty() > 0;
            if (i.getErpOrderItemId() != null && remaining(i) > 0) {
                lines.add(new TradeDocItemRequest(i.getErpOrderItemId(), null, null, null, null,
                        Decimals.qty(BigDecimal.valueOf(remaining(i)).multiply(ratio(i.getUnitRatio()))), null, null));
            }
        }
        if (lines.isEmpty()) {
            closeErpOrder(o);
            updateErp(o.getId(), WxErpStatus.CLOSED);
            return;
        }
        String remark = cut(Messages.get(Messages.DEFAULT_LOCALE, "channel.text.orderRemark", o.getOrderId()), 255);
        Long docId = tradeDocService.create(TradeKind.SALES_OUT, new TradeDocRequest(o.getErpOrderId(), shop.getWarehouseId(), LocalDate.now(),
                remark, false, lines));
        String docNo = tradeDocService.detail(TradeKind.SALES_OUT, docId).getDocNo();
        if (refunded) {
            closeErpOrder(o);
        }
        orderMapper.update(Wrappers.<WxOrder>lambdaUpdate()
                .set(WxOrder::getSalesOutId, docId)
                .set(WxOrder::getSalesOutNo, docNo)
                .set(WxOrder::getErpStatus, WxErpStatus.OUTBOUND)
                .set(WxOrder::getErrorMsg, null)
                .eq(WxOrder::getId, o.getId()));
    }

    /** 关闭销售订单（还可以出库的才关闭） */
    void closeErpOrder(WxOrder o) {
        if (o.getErpOrderId() == null) {
            return;
        }
        ScmOrder erp = scmOrderService.find(o.getErpOrderId());
        if (erp != null && erp.getStatus().isExecutable()) {
            scmOrderService.close(OrderKind.SALES, o.getErpOrderId());
        }
    }

    private void updateErp(Long id, WxErpStatus status) {
        orderMapper.update(Wrappers.<WxOrder>lambdaUpdate().set(WxOrder::getErpStatus, status).set(WxOrder::getErrorMsg, null)
                .eq(WxOrder::getId, id));
    }

    List<WxOrderItem> items(Long wxOrderId) {
        return itemMapper.selectList(Wrappers.<WxOrderItem>lambdaQuery().eq(WxOrderItem::getWxOrderId, wxOrderId).orderByAsc(WxOrderItem::getId));
    }

    /** 还需要出库的件数（扣除发货前已退款的） */
    static int remaining(WxOrderItem i) {
        return Math.max(0, i.getQty() - (i.getRefundedQty() == null ? 0 : i.getRefundedQty()));
    }

    static BigDecimal ratio(BigDecimal r) {
        return r == null || r.signum() <= 0 ? BigDecimal.ONE : r;
    }

    // ---------------------------------------------------------------- 手工操作

    /** 生成销售订单 / 重新处理：待生成、待映射、处理失败，或已在线上发货还没有出库的订单 */
    public void retry(Long id) {
        WxOrder o = getAccessible(id);
        boolean retryable = o.getStatus().paid() && (o.getErpStatus() == WxErpStatus.PENDING || o.getErpStatus() == WxErpStatus.UNMAPPED
                || o.getErpStatus() == WxErpStatus.FAILED || o.getErpStatus() == WxErpStatus.NONE)
                || o.getErpStatus() == WxErpStatus.ORDERED && (o.getStatus().shipped() || o.getStatus() == WxOrderStatus.CANCELLED);
        if (!retryable) {
            throw new BizException("channel.error.orderNotRetryable", o.getOrderId());
        }
        process(id, true);
        WxOrder after = orderMapper.selectById(id);
        if (after.getErpStatus() == WxErpStatus.UNMAPPED) {
            throw new BizException("channel.error.orderHasUnmapped", o.getOrderId());
        }
    }

    /** 从小店重新拉取订单并处理 */
    public void refresh(Long id) {
        WxOrder o = getAccessible(id);
        syncOne(shopService.get(o.getShopId()), o.getOrderId());
    }

    /**
     * 在 ERP 中发货：生成销售出库，提交后调用小店的发货接口回传快递公司和运单号（失败记录在订单上，可重试）
     */
    @Transactional
    public void ship(Long id, WxShopRequests.Ship req) {
        getAccessible(id);
        orderMapper.lockById(id);
        WxOrder o = orderMapper.selectById(id);
        if (o.getStatus() != WxOrderStatus.TO_SHIP) {
            throw new BizException("channel.error.orderNotToShip", o.getOrderId());
        }
        if (o.getErpOrderId() == null) {
            throw new BizException("channel.error.orderNotCreated", o.getOrderId());
        }
        if (o.getSalesOutId() != null) {
            throw new BizException("channel.error.orderAlreadyShipped", o.getOrderId());
        }
        WxShop shop = shopService.get(o.getShopId());
        createSalesOut(o, shop, items(id));
        orderMapper.update(Wrappers.<WxOrder>lambdaUpdate()
                .set(WxOrder::getDeliveryStatus, WxDeliveryStatus.PUSHING)
                .set(WxOrder::getDeliveryCompanyId, req.companyId().trim())
                .set(WxOrder::getDeliveryCompanyName, cut(req.companyName(), 64))
                .set(WxOrder::getWaybillNo, req.waybillNo().trim())
                .set(WxOrder::getShipTime, LocalDateTime.now())
                .eq(WxOrder::getId, id));
        eventPublisher.publishEvent(new WxOrderShippedEvent(id));
    }

    /** 出库提交后回传发货信息（不在事务中调用接口） */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onShipped(WxOrderShippedEvent e) {
        pushDelivery(e.wxOrderId());
    }

    /** 重新回传发货信息，失败时提示原因 */
    public void retryPush(Long id) {
        WxOrder o = getAccessible(id);
        if (o.getDeliveryStatus() != WxDeliveryStatus.FAILED && o.getDeliveryStatus() != WxDeliveryStatus.PUSHING) {
            throw new BizException("channel.error.deliveryNotRetryable", o.getOrderId());
        }
        pushDelivery(id);
        WxOrder after = orderMapper.selectById(id);
        if (after.getDeliveryStatus() == WxDeliveryStatus.FAILED) {
            throw new BizException("channel.error.deliveryPushFailed", after.getErrorMsg());
        }
    }

    /** 调用发货接口，结果在新事务中记录 */
    void pushDelivery(Long id) {
        WxOrder o = orderMapper.selectById(id);
        if (o == null || o.getDeliveryStatus() != WxDeliveryStatus.PUSHING && o.getDeliveryStatus() != WxDeliveryStatus.FAILED) {
            return;
        }
        WxShop shop = shopService.get(o.getShopId());
        List<WxShopClient.DeliveryItem> deliveryItems = items(id).stream().filter(i -> remaining(i) > 0)
                .map(i -> new WxShopClient.DeliveryItem(i.getProductId(), i.getSkuId(), remaining(i))).toList();
        WxDeliveryStatus result;
        String error = null;
        try {
            shopService.client(shop).sendDelivery(WxShopService.credentials(shop),
                    new WxShopClient.DeliveryRequest(o.getOrderId(), o.getDeliveryCompanyId(), o.getWaybillNo(), deliveryItems));
            result = WxDeliveryStatus.PUSHED;
        } catch (RuntimeException e) {
            log.warn("渠道订单 {} 发货回传失败", o.getOrderId(), e);
            result = WxDeliveryStatus.FAILED;
            error = WxErrors.text(e);
        }
        WxDeliveryStatus status = result;
        String msg = error;
        txNew.executeWithoutResult(s -> orderMapper.update(Wrappers.<WxOrder>lambdaUpdate()
                .set(WxOrder::getDeliveryStatus, status)
                .set(WxOrder::getErrorMsg, msg)
                .set(status == WxDeliveryStatus.PUSHED, WxOrder::getStatus, WxOrderStatus.SHIPPED)
                .eq(WxOrder::getId, id)));
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    /** 在 ERP 中发货并提交后回传发货信息 */
    public record WxOrderShippedEvent(Long wxOrderId) {
    }
}
