package com.lemon.erp.modules.pos.model;

/**
 * POS 支付方式（字典 pos_pay_method）。现金、微信、支付宝、银行卡在门店收款账户中对应一个结算账户（现金默认为收银台的现金账户）；
 * 储值、积分抵现需要识别会员，不进结算账户（储值消费在门店日结凭证中由会员储值转收入，积分抵现视为折让）
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
    /** 储值：扣会员储值余额（全局一份） */
    STORED_VALUE,
    /** 积分抵现：按积分规则折算金额，扣会员积分 */
    POINTS;

    /** 经聚合支付通道收款、退款 */
    public boolean electronic() {
        return this == WECHAT || this == ALIPAY;
    }

    /** 会员权益（储值、积分抵现）：需要识别会员，没有结算账户 */
    public boolean member() {
        return this == STORED_VALUE || this == POINTS;
    }

    /** 储值充值可以使用的支付方式（收到真实款项的） */
    public boolean realMoney() {
        return !member();
    }
}
