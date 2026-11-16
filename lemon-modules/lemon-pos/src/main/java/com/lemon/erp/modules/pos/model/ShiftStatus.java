package com.lemon.erp.modules.pos.model;

/**
 * 班次状态（字典 pos_shift_status）：开班后才能收银，交班后结束
 */
public enum ShiftStatus {
    OPEN,
    CLOSED
}
