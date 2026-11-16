package com.lemon.erp.modules.pos.model;

/**
 * 聚合支付通道（字典 pos_pay_provider）。接入真实服务商时新增取值并实现 {@code PayProvider}
 */
public enum PayProviderType {
    /** 线下登记：顾客用收款码 / 其他设备付款，收银员确认到账并登记参考号；退款由收银员线下办理 */
    OFFLINE,
    /** 模拟通道（只用于测试）：按金额尾数确定结果 */
    MOCK
}
