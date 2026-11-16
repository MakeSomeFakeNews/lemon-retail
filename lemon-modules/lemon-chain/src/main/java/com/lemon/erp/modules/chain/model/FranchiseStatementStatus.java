package com.lemon.erp.modules.chain.model;

/**
 * 加盟对账单状态（字典 chain_statement_status）：生成后待确认（可重新生成、删除），确认后锁定
 */
public enum FranchiseStatementStatus {
    /** 待确认 */
    DRAFT,
    /** 已确认 */
    CONFIRMED
}
