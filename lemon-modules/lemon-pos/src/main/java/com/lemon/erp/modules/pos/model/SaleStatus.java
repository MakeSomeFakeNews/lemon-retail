package com.lemon.erp.modules.pos.model;

/**
 * POS 单据状态（字典 pos_sale_status）：结算即完成；当天日结前可作废（库存、收款按原单反向）
 */
public enum SaleStatus {
    COMPLETED,
    VOIDED
}
