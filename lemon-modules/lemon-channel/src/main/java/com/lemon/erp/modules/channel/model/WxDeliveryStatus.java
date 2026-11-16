package com.lemon.erp.modules.channel.model;

/**
 * 发货信息回传状态（字典 ch_wx_delivery_status）：在 ERP 中发货后调用小店的发货接口回传物流单号
 */
public enum WxDeliveryStatus {
    /** 未发货 */
    NONE,
    /** 回传中（ERP 已出库，提交后调用接口） */
    PUSHING,
    /** 已回传 */
    PUSHED,
    /** 回传失败，可重试 */
    FAILED,
    /** 在小店后台发货（ERP 按线上的发货自动出库） */
    PLATFORM
}
