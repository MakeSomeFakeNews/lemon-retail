package com.lemon.erp.framework.tenant;

/**
 * 当前租户的套餐配额，由系统模块实现（{@code SysTenantLookup}）。门店模块新增门店时检查门店数量上限，不依赖系统模块
 */
public interface TenantQuotaLookup {

    /** 当前租户套餐的门店数量上限（不含总部），为空不限；平台租户或未开启 SaaS 模式时为空 */
    Integer storeLimit();
}
