package com.lemon.erp.framework.ref;

/**
 * 被引用的主数据类型
 */
public enum RefType {
    MATERIAL,
    WAREHOUSE,
    CUSTOMER,
    SUPPLIER,
    USER,
    DEPT,
    /** 物料分类（ID 为 wms_material_category.id，引用处按分类编码关联） */
    MATERIAL_CATEGORY,
    /** 门店（store_info.id） */
    STORE,
    /** 区域（store_region.id） */
    REGION,
    /** 门店价格方案（scm_price_scheme.id） */
    PRICE_SCHEME
}
