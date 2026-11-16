package com.lemon.erp.modules.chain.model;

/**
 * 总部费用分摊依据（字典 chain_alloc_basis，需求 §14 规则 4）
 */
public enum AllocBasis {
    /** 按销售额（当月零售 + 批发净销售额，不含税） */
    SALES,
    /** 按营业面积 */
    AREA,
    /** 平均分摊 */
    EQUAL
}
