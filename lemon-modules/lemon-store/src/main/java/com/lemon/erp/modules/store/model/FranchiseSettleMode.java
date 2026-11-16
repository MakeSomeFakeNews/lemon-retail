package com.lemon.erp.modules.store.model;

/**
 * 加盟店结算方式（字典 store_settle_mode）
 */
public enum FranchiseSettleMode {
    /** 预付货款：要货从预付款中扣减 */
    PREPAID,
    /** 月结：按账期结算 */
    CREDIT
}
