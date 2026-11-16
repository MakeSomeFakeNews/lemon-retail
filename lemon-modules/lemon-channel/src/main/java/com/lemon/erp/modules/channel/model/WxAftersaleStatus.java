package com.lemon.erp.modules.channel.model;

/**
 * 售后单状态（字典 ch_wx_aftersale_status），由微信小店的售后状态映射（见 {@link #ofPlatform(String)}，原始状态另存）
 */
public enum WxAftersaleStatus {
    /** 待商家处理 */
    PENDING,
    /** 商家已拒绝 */
    REJECTED,
    /** 待买家退货 */
    WAIT_RETURN,
    /** 待商家收货 */
    WAIT_RECEIPT,
    /** 退款中 */
    REFUNDING,
    /** 已退款 */
    REFUNDED,
    /** 退款失败 */
    REFUND_FAILED,
    /** 已取消 / 关闭 */
    CANCELLED;

    /**
     * 微信小店售后状态 → 本系统状态（按官方文档，待联调确认）
     *
     * @return 不认识的状态返回 null（保留原状态）
     */
    public static WxAftersaleStatus ofPlatform(String status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case "MERCHANT_PROCESSING", "USER_WAIT_CONFIRM_UPDATE", "USER_WAIT_HANDLE_MERCHANT_AFTER_SALE_UPDATE" -> PENDING;
            case "MERCHANT_REJECT_REFUND", "MERCHANT_REJECT_RETURN" -> REJECTED;
            case "USER_WAIT_RETURN" -> WAIT_RETURN;
            case "MERCHANT_WAIT_RECEIPT" -> WAIT_RECEIPT;
            case "PLATFORM_REFUNDING", "MERCHANT_OVERDUE_REFUND", "USER_WAIT_CONFIRM" -> REFUNDING;
            case "MERCHANT_REFUND_SUCCESS", "MERCHANT_RETURN_SUCCESS" -> REFUNDED;
            case "PLATFORM_REFUND_FAIL", "MERCHANT_REFUND_RETRY_FAIL", "MERCHANT_FAIL" -> REFUND_FAILED;
            case "USER_CANCELD", "RETURN_CLOSED" -> CANCELLED;
            default -> null;
        };
    }
}
