package com.lemon.erp.modules.channel.model;

/**
 * 同步类型（字典 ch_wx_sync_type）
 */
public enum WxSyncType {
    /** 商品：拉取商品与 SKU，自动匹配物料 */
    PRODUCT,
    /** 库存：推送可售库存 */
    STOCK,
    /** 订单：增量拉取订单 */
    ORDER,
    /** 售后：增量拉取售后单 */
    AFTERSALE
}
