package com.lemon.erp.framework.store;

/**
 * 用户可管辖的门店范围（字典 sys_store_scope）
 */
public enum StoreScope {
    /** 全部门店（总部人员） */
    ALL,
    /** 所属门店 + 指定的管辖门店 */
    ASSIGNED
}
