package com.lemon.erp.modules.scm.model;

/**
 * 调价单状态（字典 scm_price_adjust_status）：草稿 → 审批中（发布了调价单审批流程时）→ 待生效 → 已生效；待生效的可以作废
 */
public enum PriceAdjustStatus {
    /** 草稿：可修改、删除 */
    DRAFT,
    /** 审批中 */
    APPROVING,
    /** 已审核、待生效：定时生效的调价单等待生效时间 */
    APPROVED,
    /** 已生效：新价格已写入，并记录调价历史 */
    EFFECTIVE,
    /** 已作废：待生效时作废，不再生效 */
    VOIDED
}
