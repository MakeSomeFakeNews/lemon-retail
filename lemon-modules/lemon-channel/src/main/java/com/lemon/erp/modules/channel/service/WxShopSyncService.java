package com.lemon.erp.modules.channel.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.channel.client.MockWxShopClient;
import com.lemon.erp.modules.channel.client.WxCallbackCrypto;
import com.lemon.erp.modules.channel.client.WxShopClient;
import com.lemon.erp.modules.channel.dto.WxSyncResult;
import com.lemon.erp.modules.channel.entity.WxProduct;
import com.lemon.erp.modules.channel.entity.WxShop;
import com.lemon.erp.modules.channel.mapper.WxProductMapper;
import com.lemon.erp.modules.channel.model.WxShopMode;
import com.lemon.erp.modules.channel.model.WxSyncTrigger;
import com.lemon.erp.modules.channel.model.WxSyncType;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 同步的入口：手工「立即同步」、定时任务（按店铺的设置同步订单、售后、库存，一个店铺失败不影响其他）、消息推送（异步处理单个订单 / 售后单），
 * 每次同步写一条同步日志；以及模拟店铺的演示数据（生成模拟商品、模拟订单）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxShopSyncService {

    /** 模拟商品最多取几个物料 */
    private static final int MOCK_PRODUCT_LIMIT = 5;

    private final WxShopService shopService;
    private final WxProductService productService;
    private final WxOrderService orderService;
    private final WxAftersaleService aftersaleService;
    private final WxSyncLogService logService;
    private final WxProductMapper productMapper;
    private final MockWxShopClient mockClient;
    private final WmsMaterialService materialService;

    /** 手工同步一种数据（库存为全部推送） */
    public WxSyncResult syncNow(Long shopId, WxSyncType type) {
        WxShop shop = shopService.getAccessible(shopId);
        return run(shop, type, WxSyncTrigger.MANUAL, true);
    }

    /**
     * 定时同步当前租户的启用店铺：开启了订单同步的拉取订单和售后单，开启了库存同步且到达间隔的推送库存（只推送有变化的）
     *
     * @return 同步的店铺数
     */
    public int syncScheduled() {
        int count = 0;
        LocalDateTime now = LocalDateTime.now();
        for (WxShop shop : shopService.enabledShops()) {
            try {
                if (Boolean.TRUE.equals(shop.getOrderSyncEnabled())) {
                    run(shop, WxSyncType.ORDER, WxSyncTrigger.SCHEDULE, false);
                    run(shopService.get(shop.getId()), WxSyncType.AFTERSALE, WxSyncTrigger.SCHEDULE, false);
                }
                int interval = shop.getStockSyncInterval() == null ? 30 : shop.getStockSyncInterval();
                if (Boolean.TRUE.equals(shop.getStockSyncEnabled())
                        && (shop.getStockSyncedAt() == null || !shop.getStockSyncedAt().plusMinutes(interval).isAfter(now))) {
                    run(shop, WxSyncType.STOCK, WxSyncTrigger.SCHEDULE, false);
                }
                count++;
            } catch (RuntimeException e) {
                log.warn("微信小店 {} 定时同步失败", shop.getId(), e);
            }
        }
        return count;
    }

    private WxSyncResult run(WxShop shop, WxSyncType type, WxSyncTrigger trigger, boolean all) {
        long start = System.currentTimeMillis();
        WxSyncResult result;
        try {
            result = WxSystemIdentity.call(() -> switch (type) {
                case PRODUCT -> productService.syncProducts(shop);
                case STOCK -> productService.pushStock(shop, all);
                case ORDER -> orderService.syncOrders(shop);
                case AFTERSALE -> aftersaleService.syncAftersales(shop);
            });
        } catch (RuntimeException e) {
            log.warn("微信小店 {} {} 同步失败", shop.getId(), type, e);
            result = new WxSyncResult(0, 0, 0, false, WxErrors.text(e));
        }
        logService.record(shop.getId(), type, trigger, result, start);
        return result;
    }

    // ---------------------------------------------------------------- 消息推送

    /**
     * 异步处理推送的事件：订单事件拉取该订单、售后事件拉取该售后单、商品事件忽略（定时 / 手工同步商品）。
     * 调用方已设置店铺所属的租户（异步线程由 TenantTaskDecorator 传递）
     */
    @Async
    public void handleEvent(Long shopId, WxCallbackCrypto.Message message) {
        long start = System.currentTimeMillis();
        WxShop shop;
        try {
            shop = shopService.get(shopId);
        } catch (BizException e) {
            return;
        }
        WxSyncType type = message.aftersaleId() != null ? WxSyncType.AFTERSALE : message.orderId() != null ? WxSyncType.ORDER : null;
        if (type == null) {
            return;
        }
        WxSyncResult result;
        try {
            WxSystemIdentity.run(() -> {
                if (message.aftersaleId() != null) {
                    aftersaleService.syncOne(shop, message.aftersaleId());
                } else {
                    orderService.syncOne(shop, message.orderId());
                }
            });
            result = new WxSyncResult(1, 1, 0, true, null);
        } catch (RuntimeException e) {
            log.warn("微信小店 {} 推送事件 {} 处理失败", shopId, message.event(), e);
            result = new WxSyncResult(1, 0, 1, true, WxErrors.text(e));
        }
        logService.record(shopId, type, WxSyncTrigger.CALLBACK, result, start);
    }

    // ---------------------------------------------------------------- 模拟店铺

    /** 模拟店铺：用最近的几个启用物料（编码作为商家编码，零售价作为售价）生成商品，返回生成的商品数 */
    public int mockProducts(Long shopId) {
        WxShop shop = requireMock(shopId);
        List<WmsMaterial> materials = materialService.options(null).stream()
                .filter(m -> !Integer.valueOf(1).equals(m.getIsTemplate())).limit(MOCK_PRODUCT_LIMIT).toList();
        for (WmsMaterial m : materials) {
            BigDecimal price = m.getRetailPrice() != null && m.getRetailPrice().signum() > 0 ? m.getRetailPrice() : BigDecimal.valueOf(99);
            mockClient.addProduct(shop.getId(), m.getName(), List.of(new MockWxShopClient.SkuSpec(m.getCode(), m.getBarcode(), m.getSpec(), price)));
        }
        return materials.size();
    }

    /** 模拟店铺：买家用已映射的商品下单并付款（每单 1～2 个商品），随后同步这些订单，返回生成的订单数 */
    public int mockOrders(Long shopId, int count) {
        WxShop shop = requireMock(shopId);
        List<WxProduct> mapped = productMapper.selectList(Wrappers.<WxProduct>lambdaQuery().eq(WxProduct::getShopId, shopId)
                .isNotNull(WxProduct::getMaterialId).eq(WxProduct::getRemoved, false));
        if (mapped.isEmpty()) {
            throw new BizException("channel.error.mockNoMappedProduct");
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<String> orderIds = new ArrayList<>(count);
        for (int n = 0; n < count; n++) {
            List<MockWxShopClient.LineSpec> lines = new ArrayList<>();
            int kinds = Math.min(mapped.size(), 1 + random.nextInt(2));
            List<WxProduct> pool = new ArrayList<>(mapped);
            for (int k = 0; k < kinds; k++) {
                WxProduct p = pool.remove(random.nextInt(pool.size()));
                lines.add(new MockWxShopClient.LineSpec(p.getProductId(), p.getSkuId(), 1 + random.nextInt(3)));
            }
            BigDecimal freight = random.nextBoolean() ? BigDecimal.ZERO : BigDecimal.valueOf(8);
            BigDecimal discount = random.nextBoolean() ? BigDecimal.ZERO : BigDecimal.valueOf(5);
            orderIds.add(mockClient.addOrder(shopId, lines, freight, discount, true));
        }
        WxSystemIdentity.run(() -> orderIds.forEach(id -> orderService.syncOne(shop, id)));
        return orderIds.size();
    }

    private WxShop requireMock(Long shopId) {
        WxShop shop = shopService.getAccessible(shopId);
        if (shop.getMode() != WxShopMode.MOCK) {
            throw new BizException("channel.error.notMockShop", shop.getName());
        }
        return shop;
    }

    /** 快递公司列表（发货时选择） */
    public List<WxShopClient.DeliveryCompany> deliveryCompanies(Long shopId) {
        WxShop shop = shopService.getAccessible(shopId);
        return shopService.client(shop).deliveryCompanies(WxShopService.credentials(shop));
    }

    /** 消息推送的说明文字（测试连接结果） */
    public String testText(Long shopId) {
        int count = shopService.test(shopId);
        return count < 0 ? Messages.get("channel.text.testOk") : Messages.get("channel.text.testOkWithCount", count);
    }
}
