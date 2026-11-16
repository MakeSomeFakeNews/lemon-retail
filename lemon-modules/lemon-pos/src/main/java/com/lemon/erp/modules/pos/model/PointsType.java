package com.lemon.erp.modules.pos.model;

/**
 * 积分变动类型（字典 pos_points_type）
 */
public enum PointsType {
    /** 消费获得 */
    EARN,
    /** 积分抵现 */
    REDEEM,
    /** 退货 / 作废扣回获得的积分 */
    RETURN,
    /** 退货 / 作废退回抵现使用的积分 */
    REFUND,
    /** 手工调整（pos:member:points） */
    ADJUST,
    /** 批量清零（pos:member:points） */
    CLEAR
}
