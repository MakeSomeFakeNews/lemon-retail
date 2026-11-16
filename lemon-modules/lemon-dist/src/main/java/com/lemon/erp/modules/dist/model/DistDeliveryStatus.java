package com.lemon.erp.modules.dist.model;

/**
 * 配送单 / 退仓单状态（字典 dist_delivery_status）。配送：草稿 → 已发货（在途）→ 已收货 / 收货有差异 → 已完成（差异全部处理）；
 * 退仓：草稿 → 待审核 → 已审核 → 已发货 → 已收货 / 收货有差异 → 已完成。驳回的退仓单回到草稿
 */
public enum DistDeliveryStatus {
    DRAFT,
    /** 待审核（退仓） */
    SUBMITTED,
    /** 已审核（退仓），门店可以发货 */
    APPROVED,
    /** 已发货：发货仓库已调出，货物在途 */
    SHIPPED,
    /** 已收货：实收与发货一致 */
    RECEIVED,
    /** 收货有差异：差异待总部处理 */
    DIFF,
    /** 已完成：收货差异已全部处理 */
    CLOSED
}
