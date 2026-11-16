package com.lemon.erp.modules.channel.model;

/**
 * 店铺的对接方式（字典 ch_wx_shop_mode）：MOCK 模拟店铺（内存中的假店铺，用于演示、测试，未开通小店时使用）/ LIVE 正式对接微信小店接口
 */
public enum WxShopMode {
    MOCK,
    LIVE
}
