package com.lemon.erp.modules.dist.model;

/**
 * 要货单状态（字典 dist_requisition_status）：草稿 →（提交，可选审批：审批中）→ 待审核 →（总部审核，可调整批准数量）→ 已审核
 * → 部分配送 / 已配送；已审核、部分配送的可以关闭（不再配送剩余数量）。审批驳回、总部驳回回到草稿（记录驳回原因）
 */
public enum RequisitionStatus {
    DRAFT,
    /** 审批中：发布了「门店要货审批」流程时，提交后先审批 */
    APPROVING,
    /** 待审核：已提交，等待总部 / 配送中心审核 */
    SUBMITTED,
    /** 已审核：可以配送 */
    APPROVED,
    /** 部分配送 */
    PARTIAL,
    /** 已配送：批准数量全部配送 */
    DELIVERED,
    /** 已关闭：剩余数量不再配送 */
    CLOSED;

    /** 可以配送（已审核、部分配送） */
    public boolean deliverable() {
        return this == APPROVED || this == PARTIAL;
    }
}
