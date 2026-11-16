package com.lemon.erp.framework.store;

/**
 * 门店状态（字典 store_status）：停业、闭店的门店不能开单
 */
public enum StoreStatus {
    /** 筹备 */
    PREPARING,
    /** 营业 */
    OPEN,
    /** 停业 */
    SUSPENDED,
    /** 闭店 */
    CLOSED;

    /** 能否开单（筹备中的门店可以录入期初、要货等准备工作） */
    public boolean canTrade() {
        return this == PREPARING || this == OPEN;
    }
}
