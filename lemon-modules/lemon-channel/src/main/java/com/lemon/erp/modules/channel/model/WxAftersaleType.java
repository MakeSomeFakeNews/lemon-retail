package com.lemon.erp.modules.channel.model;

/**
 * 售后类型（字典 ch_wx_aftersale_type）：REFUND 仅退款 / RETURN 退货退款
 */
public enum WxAftersaleType {
    REFUND,
    RETURN;

    /** 微信小店的售后类型（按官方文档，待联调确认），不认识的视为仅退款 */
    public static WxAftersaleType ofPlatform(String type) {
        return RETURN.name().equals(type) ? RETURN : REFUND;
    }
}
