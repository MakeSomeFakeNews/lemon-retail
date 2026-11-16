package com.lemon.erp.modules.scm.model;

/**
 * 开单默认价的来源。销售：客户执行价 → 销售价目表 → 客户价格等级价 → 门店价格方案价 → 零售价 → 最近售价；
 * 采购：供货价格 → 最近进价 → 参考成本价
 */
public enum PriceSource {
    CUSTOMER_PRICE,
    PRICE_LIST,
    PRICE_LEVEL,
    /** 门店价格方案的零售价（单据仓库所属门店绑定的方案） */
    STORE_SCHEME,
    RETAIL,
    SUPPLIER_PRICE,
    LAST_PRICE,
    REF_COST
}
