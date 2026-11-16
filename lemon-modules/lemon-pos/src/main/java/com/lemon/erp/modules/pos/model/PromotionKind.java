package com.lemon.erp.modules.pos.model;

/**
 * 满减活动类型（字典 pos_promotion_kind）
 */
public enum PromotionKind {
    /** 满 X 减 Y（可每满叠加） */
    REDUCE,
    /** 满 X 打 Z 折 */
    DISCOUNT,
    /** 满 X 赠商品（可每满叠加） */
    GIFT
}
