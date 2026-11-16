package com.lemon.erp.modules.channel.model;

/**
 * 渠道订单在 ERP 中的处理状态（字典 ch_wx_erp_status）：
 * 未付款不处理；付款后全部商品已映射时生成并审核销售订单，有未映射的商品时待映射（映射后重试）；
 * 发货后生成销售出库；发货前取消的关闭销售订单
 */
public enum WxErpStatus {
    /** 未处理（未付款） */
    NONE,
    /** 待生成：已付款，店铺未开启自动生成销售订单，手工「生成销售订单」 */
    PENDING,
    /** 待映射：有商品没有映射到物料 */
    UNMAPPED,
    /** 已生成销售订单（待出库） */
    ORDERED,
    /** 已出库 */
    OUTBOUND,
    /** 已关闭（发货前取消或全部退款） */
    CLOSED,
    /** 处理失败（见错误信息），可重试 */
    FAILED
}
