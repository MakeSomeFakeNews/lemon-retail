package com.lemon.erp.modules.pos.model;

/**
 * POS 行价格来源（字典 pos_price_source），按取价顺序：特价活动 → 会员价 / 会员折扣 → 门店价格方案 → 总部零售价；手工改价为 MANUAL
 */
public enum PosPriceSource {
    SPECIAL,
    MEMBER,
    SCHEME,
    RETAIL,
    MANUAL
}
