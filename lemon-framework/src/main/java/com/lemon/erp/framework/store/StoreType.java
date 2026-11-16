package com.lemon.erp.framework.store;

/**
 * 门店类型（字典 store_type）：总部每个租户唯一
 */
public enum StoreType {
    /** 总部 */
    HQ,
    /** 直营店 */
    DIRECT,
    /** 加盟店 */
    FRANCHISE,
    /** 配送中心 */
    DC
}
