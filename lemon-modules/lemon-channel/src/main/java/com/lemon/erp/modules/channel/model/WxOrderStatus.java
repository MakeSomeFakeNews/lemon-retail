package com.lemon.erp.modules.channel.model;

/**
 * 渠道订单状态（字典 ch_wx_order_status），由微信小店的订单状态码映射（见 {@link #ofPlatform(int)}）
 */
public enum WxOrderStatus {
    /** 待付款（10） */
    UNPAID,
    /** 待发货（20、21 部分发货） */
    TO_SHIP,
    /** 已发货 / 待收货（30） */
    SHIPPED,
    /** 已完成（100） */
    COMPLETED,
    /** 已取消（200 全部商品售后后取消、250 未付款取消） */
    CANCELLED;

    /**
     * 微信小店订单状态码 → 本系统状态（按官方文档，待联调确认）
     *
     * @return 不认识的状态码返回 null（保留原状态）
     */
    public static WxOrderStatus ofPlatform(int code) {
        return switch (code) {
            case 10 -> UNPAID;
            case 20, 21 -> TO_SHIP;
            case 30 -> SHIPPED;
            case 100 -> COMPLETED;
            case 200, 250 -> CANCELLED;
            default -> null;
        };
    }

    /** 已付款（待发货、已发货、已完成） */
    public boolean paid() {
        return this == TO_SHIP || this == SHIPPED || this == COMPLETED;
    }

    /** 已在线上发货（已发货、已完成） */
    public boolean shipped() {
        return this == SHIPPED || this == COMPLETED;
    }
}
