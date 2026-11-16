package com.lemon.erp.modules.pos.model;

/**
 * 特价活动状态（字典 pos_special_status）：草稿 → 进行中（在活动时间内参与取价）→ 已结束
 */
public enum SpecialStatus {
    DRAFT,
    ACTIVE,
    ENDED
}
