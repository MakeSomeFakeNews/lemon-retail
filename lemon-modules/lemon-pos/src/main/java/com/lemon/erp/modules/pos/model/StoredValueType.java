package com.lemon.erp.modules.pos.model;

/**
 * 储值变动类型（字典 pos_stored_value_type）
 */
public enum StoredValueType {
    /** 充值（实收） */
    TOPUP,
    /** 充值赠送 */
    BONUS,
    /** 储值支付 */
    CONSUME,
    /** 退货 / 作废退回储值 */
    REFUND,
    /** 手工调整（pos:member:storedValue） */
    ADJUST
}
