package com.lemon.erp.modules.scm.model;

/**
 * 订单来源：手工录入、补货建议生成或直接开单（字典 scm_order_source）
 */
public enum OrderSource {
    /** 手工 */
    MANUAL,
    /** 补货建议 */
    REORDER,
    /** 直接开单：不选订单直接开销售出库 / 采购入库（或退货）时系统生成并直接审核的隐式订单，不能修改、变更 */
    DIRECT
}
