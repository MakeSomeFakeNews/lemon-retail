package com.lemon.erp.modules.scm.model;

/**
 * 调价对象（字典 scm_price_adjust_target）
 */
public enum PriceAdjustTarget {
    /** 总部价：物料的零售价 */
    HQ,
    /** 门店价格方案：一个或多个方案的方案价 */
    SCHEME
}
