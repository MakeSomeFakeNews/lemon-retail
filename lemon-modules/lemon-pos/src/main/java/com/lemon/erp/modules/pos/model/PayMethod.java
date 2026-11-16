package com.lemon.erp.modules.pos.model;

/**
 * POS 支付方式（字典 pos_pay_method）。每种方式在门店收款账户中对应一个结算账户（现金默认为收银台的现金账户）
 */
public enum PayMethod {
    /** 现金：可找零 */
    CASH,
    /** 微信：经聚合支付通道（付款码 / 扫码） */
    WECHAT,
    /** 支付宝：经聚合支付通道（付款码 / 扫码） */
    ALIPAY,
    /** 银行卡：刷卡后收银员登记参考号 */
    BANK_CARD,
    /** 储值：会员储值支付，M4 启用，目前不可用 */
    STORED_VALUE;

    /** 经聚合支付通道收款、退款 */
    public boolean electronic() {
        return this == WECHAT || this == ALIPAY;
    }

    /** 目前可以使用（储值在会员储值功能上线前不可用） */
    public boolean available() {
        return this != STORED_VALUE;
    }
}
