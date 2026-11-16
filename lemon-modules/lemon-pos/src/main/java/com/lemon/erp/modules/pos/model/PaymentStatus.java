package com.lemon.erp.modules.pos.model;

/**
 * 收款 / 退款记录的状态（字典 pos_payment_status）
 */
public enum PaymentStatus {
    /** 处理中：等待顾客付款（扫码、付款码需输密码）或等待收银员确认（线下登记） */
    PENDING,
    /** 成功 */
    SUCCESS,
    /** 失败 */
    FAILED,
    /** 已关闭：未付款的交易被关闭，或已付款未结算的交易已原路退回 */
    CLOSED
}
