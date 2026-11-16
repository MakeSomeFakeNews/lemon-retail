package com.lemon.erp.modules.dist.model;

/**
 * 收货差异状态（字典 dist_discrepancy_status）
 */
public enum DiscrepancyStatus {
    /** 待处理：少收 / 破损的数量在途（差异调拨），多收的数量未入账 */
    PENDING,
    /** 已处理 */
    HANDLED
}
