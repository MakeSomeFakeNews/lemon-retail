package com.lemon.erp.modules.channel.model;

/**
 * 售后单在 ERP 中的处理状态（字典 ch_wx_aftersale_erp_status）：退款完成后，发货前的仅退款扣减待发数量（全部退款时关闭销售订单），
 * 发货后的退货退款生成销售退货；发货后的仅退款只记录（不影响库存）
 */
public enum WxAftersaleErpStatus {
    /** 未处理（售后未完成） */
    NONE,
    /** 已处理 */
    DONE,
    /** 无需处理（只记录） */
    SKIPPED,
    /** 处理失败，可重试 */
    FAILED
}
