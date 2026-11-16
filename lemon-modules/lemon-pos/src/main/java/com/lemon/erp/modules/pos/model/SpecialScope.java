package com.lemon.erp.modules.pos.model;

/**
 * 特价活动的适用门店（字典 pos_special_scope）
 */
public enum SpecialScope {
    /** 全部门店 */
    ALL,
    /** 指定门店 */
    STORES,
    /** 指定区域（含下级区域中的门店，按取价时的区域归属判断） */
    REGIONS
}
