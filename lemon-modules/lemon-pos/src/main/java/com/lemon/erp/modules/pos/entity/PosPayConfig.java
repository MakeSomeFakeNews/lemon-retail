package com.lemon.erp.modules.pos.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.pos.model.PayProviderType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 聚合支付通道设置（每个租户一条）。密钥只写，接口返回时不带
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pos_pay_config")
public class PosPayConfig extends BaseEntity {

    private PayProviderType provider;

    private String appId;

    private String merchantNo;

    private String apiKey;

    private String apiSecret;

    private String notifyUrl;

    private String remark;
}
