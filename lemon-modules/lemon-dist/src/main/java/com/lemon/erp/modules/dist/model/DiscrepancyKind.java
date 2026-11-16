package com.lemon.erp.modules.dist.model;

/**
 * 收货差异种类（字典 dist_discrepancy_kind）
 */
public enum DiscrepancyKind {
    /** 少收：实收少于发货 */
    SHORT,
    /** 破损：货物破损未入库（库存处理同少收） */
    DAMAGED,
    /** 多收：实收多于发货 */
    OVER
}
