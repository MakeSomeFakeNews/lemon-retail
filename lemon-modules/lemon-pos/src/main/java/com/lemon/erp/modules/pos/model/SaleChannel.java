package com.lemon.erp.modules.pos.model;

/**
 * 营销活动的适用渠道（字典 pos_sale_channel）：目前只有门店收银参与取价，「全部渠道」预留给线上渠道
 */
public enum SaleChannel {
    /** 门店收银 */
    POS,
    /** 全部渠道 */
    ALL
}
