package com.lemon.erp.modules.channel.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.store.StoreLookup;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.modules.channel.client.WxCallbackCrypto;
import com.lemon.erp.modules.channel.client.WxShopClient;
import com.lemon.erp.modules.channel.client.WxShopClients;
import com.lemon.erp.modules.channel.entity.WxAftersale;
import com.lemon.erp.modules.channel.entity.WxOrder;
import com.lemon.erp.modules.channel.entity.WxProduct;
import com.lemon.erp.modules.channel.entity.WxShop;
import com.lemon.erp.modules.channel.entity.WxSyncLog;
import com.lemon.erp.modules.channel.mapper.WxAftersaleMapper;
import com.lemon.erp.modules.channel.mapper.WxOrderMapper;
import com.lemon.erp.modules.channel.mapper.WxProductMapper;
import com.lemon.erp.modules.channel.mapper.WxShopMapper;
import com.lemon.erp.modules.channel.mapper.WxSyncLogMapper;
import com.lemon.erp.modules.channel.model.WxShopMode;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.scm.service.ScmCustomerService;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 微信小店店铺设置：绑定门店与发货仓库（必须是该门店的启用仓库）、生成销售订单的客户（未选择时自动创建「微信小店顾客」）、
 * 同步设置。密钥只写不读；按用户的门店范围只能看到、维护可管辖门店的店铺
 */
@Service
@RequiredArgsConstructor
public class WxShopService {

    /** 消息推送地址（相对接口根路径 /api） */
    public static final String CALLBACK_PATH = "/channel/wx-shop/callback/";

    private final WxShopMapper shopMapper;
    private final WxProductMapper productMapper;
    private final WxOrderMapper orderMapper;
    private final WxAftersaleMapper aftersaleMapper;
    private final WxSyncLogMapper syncLogMapper;
    private final WxShopClients clients;
    private final StoreLookup storeLookup;
    private final WmsWarehouseService warehouseService;
    private final ScmCustomerService customerService;

    @DataPermission(table = "ch_wx_shop", deptColumn = "", userColumn = "", storeColumn = "store_id")
    public PageResult<WxShop> page(long current, long size, String keyword, EnableStatus status, Long storeId) {
        Page<WxShop> page = Page.of(current, size);
        shopMapper.selectPageList(page, keyword, status, storeId);
        page.getRecords().forEach(WxShopService::fillDisplay);
        return PageResult.of(page);
    }

    /** 店铺下拉（可管辖门店的店铺）：[{id, name, mode, status}] */
    public List<WxShop> options() {
        var scope = SecurityUtils.getDataScope();
        return shopMapper.selectList(Wrappers.<WxShop>lambdaQuery()
                        .select(WxShop::getId, WxShop::getName, WxShop::getMode, WxShop::getStatus, WxShop::getStoreId)
                        .orderByAsc(WxShop::getId))
                .stream().filter(s -> scope.canAccessStore(s.getStoreId())).toList();
    }

    /** 详情（不含密钥），没有该门店的权限时视为不存在 */
    public WxShop detail(Long id) {
        WxShop shop = getAccessible(id);
        fillDisplay(shop);
        return shop;
    }

    /** 店铺（含密钥），不存在时报错；不校验门店权限，供同步、推送等系统处理使用 */
    public WxShop get(Long id) {
        WxShop shop = id == null ? null : shopMapper.selectById(id);
        if (shop == null) {
            throw new BizException("channel.error.shopNotFound");
        }
        return shop;
    }

    /** 当前用户可管辖门店的店铺（含密钥） */
    public WxShop getAccessible(Long id) {
        WxShop shop = get(id);
        if (!SecurityUtils.getDataScopeOrAll().canAccessStore(shop.getStoreId())) {
            throw new BizException("channel.error.shopNotFound");
        }
        return shop;
    }

    /** 当前租户的启用店铺（含密钥），定时同步使用 */
    public List<WxShop> enabledShops() {
        return shopMapper.selectList(Wrappers.<WxShop>lambdaQuery().eq(WxShop::getStatus, EnableStatus.ENABLED).orderByAsc(WxShop::getId));
    }

    /**
     * 消息推送：按店铺 ID 跨租户查找（推送请求没有登录信息），停用或不存在时返回 null
     */
    public WxShop findForCallback(Long id) {
        WxShop shop = id == null ? null : TenantContext.callIgnore(() -> shopMapper.selectById(id));
        return shop == null || shop.getStatus() != EnableStatus.ENABLED ? null : shop;
    }

    /** 店铺所属租户 */
    public static Long tenantOf(WxShop shop) {
        return shop.getTenantId() == null ? TenantContext.PLATFORM_TENANT_ID : shop.getTenantId();
    }

    public WxShopClient client(WxShop shop) {
        return clients.of(shop.getMode());
    }

    public static WxShopClient.Credentials credentials(WxShop shop) {
        return new WxShopClient.Credentials(shop.getId(), shop.getMode(), shop.getAppId(), shop.getAppSecret(), shop.getReturnAddressId());
    }

    @Transactional
    public Long create(WxShop shop) {
        shop.setId(null);
        normalize(shop, null);
        shopMapper.insert(shop);
        return shop.getId();
    }

    @Transactional
    public void update(Long id, WxShop shop) {
        WxShop old = getAccessible(id);
        shop.setId(id);
        if (old.getStoreId() != null && !old.getStoreId().equals(shop.getStoreId()) && hasOrders(id)) {
            // 已有渠道订单的店铺不能改绑门店：订单、售后的门店在写入时确定
            throw new BizException("channel.error.storeCannotChange");
        }
        normalize(shop, old);
        shop.setStockSyncedAt(null);
        shop.setOrderSyncedTo(null);
        shop.setAftersaleSyncedTo(null);
        shopMapper.updateById(shop);
        if (shop.isClearPushSecrets()) {
            shopMapper.update(Wrappers.<WxShop>lambdaUpdate().set(WxShop::getPushToken, null).set(WxShop::getAesKey, null).eq(WxShop::getId, id));
        }
        // 凭证在提交后清理，避免并发请求在提交前又按旧配置缓存
        afterCommit(() -> clients.evictToken(id));
    }

    /** 有渠道订单的店铺不能删除（停用即可） */
    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            WxShop shop = getAccessible(id);
            if (hasOrders(id) || aftersaleMapper.exists(Wrappers.<WxAftersale>lambdaQuery().eq(WxAftersale::getShopId, id))) {
                throw new BizException("channel.error.shopHasOrders", shop.getName());
            }
            productMapper.delete(Wrappers.<WxProduct>lambdaQuery().eq(WxProduct::getShopId, id));
            syncLogMapper.delete(Wrappers.<WxSyncLog>lambdaQuery().eq(WxSyncLog::getShopId, id));
            shopMapper.deleteById(id);
        }
        afterCommit(() -> ids.forEach(clients::evictToken));
    }

    /** 测试连接：获取接口调用凭证并读取商品列表，返回商品总数（-1 表示接口未返回） */
    public int test(Long id) {
        WxShop shop = getAccessible(id);
        return client(shop).test(credentials(shop));
    }

    /** 记录同步进度（库存同步时间、订单 / 售后已同步到的时间），单条更新 */
    void markStockSynced(Long id, LocalDateTime time) {
        shopMapper.update(Wrappers.<WxShop>lambdaUpdate().set(WxShop::getStockSyncedAt, time).eq(WxShop::getId, id));
    }

    void markOrderSynced(Long id, LocalDateTime to) {
        shopMapper.update(Wrappers.<WxShop>lambdaUpdate().set(WxShop::getOrderSyncedTo, to).eq(WxShop::getId, id));
    }

    void markAftersaleSynced(Long id, LocalDateTime to) {
        shopMapper.update(Wrappers.<WxShop>lambdaUpdate().set(WxShop::getAftersaleSyncedTo, to).eq(WxShop::getId, id));
    }

    // ---------------------------------------------------------------- 内部

    private boolean hasOrders(Long shopId) {
        return orderMapper.exists(Wrappers.<WxOrder>lambdaQuery().eq(WxOrder::getShopId, shopId));
    }

    /** 校验并补全：门店、仓库、客户、密钥（修改时留空的密钥保持不变）、默认值 */
    private void normalize(WxShop shop, WxShop old) {
        shop.setName(shop.getName().trim());
        WxShop sameName = shopMapper.selectOne(Wrappers.<WxShop>lambdaQuery().eq(WxShop::getName, shop.getName()));
        if (sameName != null && !Objects.equals(sameName.getId(), shop.getId())) {
            throw new BizException("channel.error.shopNameExists", shop.getName());
        }
        if (storeLookup.find(shop.getStoreId()).isEmpty()) {
            throw new BizException("framework.error.storeNotFound");
        }
        if (!SecurityUtils.getDataScopeOrAll().canAccessStore(shop.getStoreId())) {
            throw new BizException("framework.error.storeNotAccessible");
        }
        warehouseService.checkStoreWarehouse(shop.getStoreId(), shop.getWarehouseId());
        if (shop.getCustomerId() == null) {
            shop.setCustomerId(old != null && old.getCustomerId() != null ? old.getCustomerId() : defaultCustomer());
        } else {
            ScmCustomer customer = customerService.get(shop.getCustomerId());
            if (customer.getStatus() != EnableStatus.ENABLED) {
                throw new BizException("channel.error.customerDisabled", customer.getName());
            }
        }
        shop.setAppId(trim(shop.getAppId()));
        shop.setReturnAddressId(trim(shop.getReturnAddressId()));
        shop.setAppSecret(keep(trim(shop.getAppSecret()), old == null ? null : old.getAppSecret()));
        shop.setPushToken(keep(trim(shop.getPushToken()), old == null || shop.isClearPushSecrets() ? null : old.getPushToken()));
        shop.setAesKey(keep(trim(shop.getAesKey()), old == null || shop.isClearPushSecrets() ? null : old.getAesKey()));
        if (shop.getAesKey() != null && !WxCallbackCrypto.validKey(shop.getAesKey())) {
            throw new BizException("channel.error.aesKeyInvalid");
        }
        if (shop.getMode() == WxShopMode.LIVE && (shop.getAppId() == null || shop.getAppSecret() == null)) {
            throw new BizException("channel.error.appIdSecretRequired");
        }
        if (shop.getStockSyncEnabled() == null) shop.setStockSyncEnabled(old != null ? old.getStockSyncEnabled() : false);
        if (shop.getOrderSyncEnabled() == null) shop.setOrderSyncEnabled(old != null ? old.getOrderSyncEnabled() : true);
        if (shop.getAutoCreateOrder() == null) shop.setAutoCreateOrder(old != null ? old.getAutoCreateOrder() : true);
        if (shop.getSafetyStock() == null) shop.setSafetyStock(old != null ? old.getSafetyStock() : BigDecimal.ZERO);
        if (shop.getStockSyncInterval() == null) shop.setStockSyncInterval(old != null ? old.getStockSyncInterval() : 30);
        if (shop.getStatus() == null) shop.setStatus(old != null ? old.getStatus() : EnableStatus.ENABLED);
        shop.setTenantId(null);
        shop.setCreateBy(null);
    }

    /** 未选择客户时使用「微信小店顾客」（没有时创建，名称按默认语言） */
    private Long defaultCustomer() {
        String name = Messages.get(Messages.DEFAULT_LOCALE, "channel.text.defaultCustomer");
        Long id = customerService.findIdByName(name);
        if (id != null) {
            return id;
        }
        ScmCustomer customer = new ScmCustomer();
        customer.setName(name);
        customer.setRemark(Messages.get(Messages.DEFAULT_LOCALE, "channel.text.defaultCustomerRemark"));
        customerService.create(customer);
        return customer.getId();
    }

    private static void fillDisplay(WxShop shop) {
        shop.setAppSecretSet(StringUtils.hasText(shop.getAppSecret()));
        shop.setPushTokenSet(StringUtils.hasText(shop.getPushToken()));
        shop.setAesKeySet(StringUtils.hasText(shop.getAesKey()));
        shop.setCallbackPath(CALLBACK_PATH + shop.getId());
    }

    private static String keep(String input, String old) {
        return input != null ? input : old;
    }

    private static String trim(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }

    private static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
