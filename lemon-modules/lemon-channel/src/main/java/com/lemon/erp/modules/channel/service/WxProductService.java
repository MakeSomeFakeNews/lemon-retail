package com.lemon.erp.modules.channel.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.modules.channel.client.WxShopClient;
import com.lemon.erp.modules.channel.dto.WxProductQuery;
import com.lemon.erp.modules.channel.dto.WxShopRequests;
import com.lemon.erp.modules.channel.dto.WxSyncResult;
import com.lemon.erp.modules.channel.entity.WxProduct;
import com.lemon.erp.modules.channel.entity.WxShop;
import com.lemon.erp.modules.channel.mapper.WxOrderMapper;
import com.lemon.erp.modules.channel.mapper.WxProductMapper;
import com.lemon.erp.modules.wms.dto.BarcodeMatch;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.StockReservationService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 商品映射与库存推送。
 * <ul>
 *   <li>商品同步：分页拉取小店全部商品及 SKU，写入映射表（新增或更新标题、规格、编码、售价、线上库存），全部拉取成功后把这次没有出现的 SKU 标记为线上已删除；
 *       然后对未映射（且不是手工映射）的 SKU 自动匹配：先按商家编码、再按 SKU 编码，依次匹配物料编码、物料条码、包装条码（包装条码时换算率为包装的换算率），
 *       多规格款式（模板）不参与匹配；</li>
 *   <li>库存推送：已映射 SKU 的可售数量 = ⌊(发货仓库的空闲库存 − 已付款未出库的渠道订单占用 − 安全库存) ÷ 换算率⌋，不小于 0，按 SKU 全量设置；
 *       定时推送时数量没有变化的 SKU 不再调用接口，手工同步全部推送。同一物料映射多个 SKU 时每个 SKU 都推送全部可售数量。</li>
 * </ul>
 * 调用接口在事务之外，每个 SKU 的写入单独提交，一个失败不影响其他
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxProductService {

    private final WxProductMapper productMapper;
    private final WxOrderMapper orderMapper;
    private final WxShopService shopService;
    private final WmsMaterialService materialService;
    private final StockReservationService reservationService;

    /** 按用户的门店范围过滤（店铺属于可管辖门店） */
    @DataPermission(table = "ch_wx_shop", deptColumn = "", userColumn = "", storeColumn = "store_id")
    public PageResult<WxProduct> page(long current, long size, WxProductQuery query) {
        Page<WxProduct> page = Page.of(current, size);
        productMapper.selectPageList(page, query);
        return PageResult.of(page);
    }

    /** 手工映射（之后自动匹配不再覆盖） */
    public void map(Long id, WxShopRequests.Mapping req) {
        WxProduct p = getAccessible(id);
        WmsMaterial m = materialService.get(req.materialId());
        if (m.getStatus() != EnableStatus.ENABLED) {
            throw new BizException("channel.error.materialDisabled", m.getCode(), m.getName());
        }
        if (Integer.valueOf(1).equals(m.getIsTemplate())) {
            throw new BizException("channel.error.materialIsTemplate", m.getCode(), m.getName());
        }
        productMapper.update(Wrappers.<WxProduct>lambdaUpdate()
                .set(WxProduct::getMaterialId, m.getId())
                .set(WxProduct::getUnitRatio, req.unitRatio() == null ? BigDecimal.ONE : req.unitRatio())
                .set(WxProduct::getMatchManual, true)
                .set(WxProduct::getUpdateTime, LocalDateTime.now())
                .eq(WxProduct::getId, p.getId()));
    }

    /** 取消映射（恢复为自动匹配） */
    public void unmap(Long id) {
        WxProduct p = getAccessible(id);
        productMapper.update(Wrappers.<WxProduct>lambdaUpdate()
                .set(WxProduct::getMaterialId, null)
                .set(WxProduct::getUnitRatio, BigDecimal.ONE)
                .set(WxProduct::getMatchManual, false)
                .set(WxProduct::getUpdateTime, LocalDateTime.now())
                .eq(WxProduct::getId, p.getId()));
    }

    /** 对店铺未映射的 SKU 自动匹配，返回新匹配的数量 */
    @Transactional
    public int autoMatch(Long shopId) {
        shopService.getAccessible(shopId);
        return autoMatchInternal(shopId);
    }

    /** 店铺已映射的 SKU：小店 SKU ID → 映射（不含线上已删除的） */
    Map<String, WxProduct> mappedSkus(Long shopId) {
        Map<String, WxProduct> map = new HashMap<>();
        productMapper.selectList(Wrappers.<WxProduct>lambdaQuery().eq(WxProduct::getShopId, shopId).isNotNull(WxProduct::getMaterialId))
                .forEach(p -> map.put(key(p.getProductId(), p.getSkuId()), p));
        return map;
    }

    static String key(String productId, String skuId) {
        return productId + "/" + skuId;
    }

    // ---------------------------------------------------------------- 商品同步

    /** 拉取商品并自动匹配（由 {@link WxShopSyncService} 调用并记录日志） */
    WxSyncResult syncProducts(WxShop shop) {
        WxShopClient client = shopService.client(shop);
        WxShopClient.Credentials c = WxShopService.credentials(shop);
        LocalDateTime now = LocalDateTime.now();
        Set<String> seen = new HashSet<>();
        int total = 0;
        int failed = 0;
        String firstError = null;
        String next = null;
        boolean complete = true;
        try {
            do {
                WxShopClient.Page<String> page = client.listProductIds(c, next);
                for (String productId : page.items()) {
                    total++;
                    try {
                        WxShopClient.Product product = client.getProduct(c, productId);
                        for (WxShopClient.Sku sku : product.skus()) {
                            upsert(shop.getId(), product, sku, now);
                            seen.add(key(product.productId(), sku.skuId()));
                        }
                    } catch (RuntimeException e) {
                        failed++;
                        complete = false;
                        firstError = firstError == null ? WxErrors.text(e) : firstError;
                        log.warn("微信小店商品 {} 同步失败", productId, e);
                    }
                }
                next = page.hasMore() ? page.nextKey() : null;
            } while (next != null);
        } catch (RuntimeException e) {
            log.warn("微信小店 {} 商品列表拉取失败", shop.getId(), e);
            return new WxSyncResult(total, total - failed, failed, false, WxErrors.text(e));
        }
        if (complete) {
            markRemoved(shop.getId(), seen);
        }
        autoMatchInternal(shop.getId());
        return new WxSyncResult(total, total - failed, failed, true, firstError);
    }

    private void upsert(Long shopId, WxShopClient.Product product, WxShopClient.Sku sku, LocalDateTime now) {
        WxProduct old = productMapper.selectOne(Wrappers.<WxProduct>lambdaQuery().eq(WxProduct::getShopId, shopId)
                .eq(WxProduct::getProductId, product.productId()).eq(WxProduct::getSkuId, sku.skuId()));
        WxProduct p = old != null ? old : new WxProduct();
        p.setShopId(shopId);
        p.setProductId(product.productId());
        p.setSkuId(sku.skuId());
        p.setTitle(cut(product.title(), 255));
        p.setSkuAttrs(cut(sku.attrs(), 255));
        p.setOutSkuId(cut(sku.outSkuId(), 64));
        p.setSkuCode(cut(sku.skuCode(), 64));
        p.setPrice(sku.price());
        p.setPlatformStock(sku.stock());
        p.setProductStatus(product.status());
        p.setRemoved(false);
        p.setSyncTime(now);
        if (old == null) {
            p.setUnitRatio(BigDecimal.ONE);
            p.setMatchManual(false);
            productMapper.insert(p);
        } else {
            productMapper.updateById(p);
        }
    }

    private void markRemoved(Long shopId, Set<String> seen) {
        List<WxProduct> all = productMapper.selectList(Wrappers.<WxProduct>lambdaQuery()
                .select(WxProduct::getId, WxProduct::getProductId, WxProduct::getSkuId)
                .eq(WxProduct::getShopId, shopId).eq(WxProduct::getRemoved, false));
        List<Long> gone = all.stream().filter(p -> !seen.contains(key(p.getProductId(), p.getSkuId()))).map(WxProduct::getId).toList();
        if (!gone.isEmpty()) {
            productMapper.update(Wrappers.<WxProduct>lambdaUpdate().set(WxProduct::getRemoved, true).in(WxProduct::getId, gone));
        }
    }

    private int autoMatchInternal(Long shopId) {
        List<WxProduct> candidates = productMapper.selectList(Wrappers.<WxProduct>lambdaQuery().eq(WxProduct::getShopId, shopId)
                .isNull(WxProduct::getMaterialId).eq(WxProduct::getMatchManual, false).eq(WxProduct::getRemoved, false));
        int matched = 0;
        for (WxProduct p : candidates) {
            BarcodeMatch match = match(p.getOutSkuId());
            if (match == null) {
                match = match(p.getSkuCode());
            }
            if (match != null) {
                productMapper.update(Wrappers.<WxProduct>lambdaUpdate()
                        .set(WxProduct::getMaterialId, match.material().getId())
                        .set(WxProduct::getUnitRatio, match.ratio() == null || match.ratio().signum() <= 0 ? BigDecimal.ONE : match.ratio())
                        .set(WxProduct::getUpdateTime, LocalDateTime.now())
                        .eq(WxProduct::getId, p.getId())
                        .isNull(WxProduct::getMaterialId));
                matched++;
            }
        }
        return matched;
    }

    /** 物料编码、条码、包装条码（启用的物料，排除多规格款式） */
    private BarcodeMatch match(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        BarcodeMatch m = materialService.byBarcode(code);
        return m == null || Integer.valueOf(1).equals(m.material().getIsTemplate()) ? null : m;
    }

    // ---------------------------------------------------------------- 库存推送

    /**
     * 推送库存
     *
     * @param all 全部推送（否则数量没有变化的不推送）
     */
    WxSyncResult pushStock(WxShop shop, boolean all) {
        List<WxProduct> mapped = productMapper.selectList(Wrappers.<WxProduct>lambdaQuery().eq(WxProduct::getShopId, shop.getId())
                .isNotNull(WxProduct::getMaterialId).eq(WxProduct::getRemoved, false));
        if (mapped.isEmpty()) {
            shopService.markStockSynced(shop.getId(), LocalDateTime.now());
            return new WxSyncResult(0, 0, 0, true, null);
        }
        Map<Long, BigDecimal> qty = sellable(shop, mapped.stream().map(WxProduct::getMaterialId).collect(java.util.stream.Collectors.toSet()));
        WxShopClient client = shopService.client(shop);
        WxShopClient.Credentials c = WxShopService.credentials(shop);
        int failed = 0;
        String firstError = null;
        for (WxProduct p : mapped) {
            BigDecimal ratio = p.getUnitRatio() == null || p.getUnitRatio().signum() <= 0 ? BigDecimal.ONE : p.getUnitRatio();
            long value = qty.getOrDefault(p.getMaterialId(), BigDecimal.ZERO).divide(ratio, 0, RoundingMode.FLOOR).max(BigDecimal.ZERO).longValue();
            if (!all && Objects.equals(p.getPushedStock(), value) && p.getStockError() == null) {
                continue;
            }
            try {
                client.setStock(c, p.getProductId(), p.getSkuId(), value);
                productMapper.update(Wrappers.<WxProduct>lambdaUpdate().set(WxProduct::getPushedStock, value)
                        .set(WxProduct::getPlatformStock, value).set(WxProduct::getStockPushTime, LocalDateTime.now())
                        .set(WxProduct::getStockError, null).eq(WxProduct::getId, p.getId()));
            } catch (RuntimeException e) {
                failed++;
                String error = WxErrors.text(e);
                firstError = firstError == null ? error : firstError;
                productMapper.update(Wrappers.<WxProduct>lambdaUpdate().set(WxProduct::getStockError, cut(error, 255))
                        .set(WxProduct::getStockPushTime, LocalDateTime.now()).eq(WxProduct::getId, p.getId()));
                log.warn("微信小店 {} SKU {} 库存推送失败", shop.getId(), p.getSkuId(), e);
            }
        }
        shopService.markStockSynced(shop.getId(), LocalDateTime.now());
        return new WxSyncResult(mapped.size(), mapped.size() - failed, failed, true, firstError);
    }

    /** 各物料可售数量（基本单位）：空闲库存 − 已付款未出库的渠道订单占用 − 安全库存 */
    Map<Long, BigDecimal> sellable(WxShop shop, Set<Long> materialIds) {
        Map<Long, BigDecimal> free = reservationService.free(shop.getWarehouseId(), materialIds);
        Map<Long, BigDecimal> pending = new HashMap<>();
        for (Map<String, Object> row : orderMapper.selectPendingQty(shop.getId())) {
            pending.put(((Number) row.get("materialId")).longValue(), new BigDecimal(row.get("qty").toString()));
        }
        BigDecimal safety = Decimals.nz(shop.getSafetyStock());
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Long id : materialIds) {
            result.put(id, Decimals.nz(free.get(id)).subtract(Decimals.nz(pending.get(id))).subtract(safety));
        }
        return result;
    }

    private WxProduct getAccessible(Long id) {
        WxProduct p = id == null ? null : productMapper.selectById(id);
        if (p == null) {
            throw new BizException("channel.error.productNotFound");
        }
        shopService.getAccessible(p.getShopId());
        return p;
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
