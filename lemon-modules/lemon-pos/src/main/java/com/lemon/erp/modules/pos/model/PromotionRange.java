package com.lemon.erp.modules.pos.model;

/**
 * 满减活动的参与商品（字典 pos_promotion_range）
 */
public enum PromotionRange {
    ALL,
    /** 指定商品 */
    MATERIALS,
    /** 指定分类（含下级分类） */
    CATEGORIES
}
