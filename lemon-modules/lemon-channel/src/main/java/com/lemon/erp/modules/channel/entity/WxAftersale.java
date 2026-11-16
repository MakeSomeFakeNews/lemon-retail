package com.lemon.erp.modules.channel.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.channel.model.WxAftersaleErpStatus;
import com.lemon.erp.modules.channel.model.WxAftersaleStatus;
import com.lemon.erp.modules.channel.model.WxAftersaleType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 微信小店售后单。退款金额只记录（退款资金由小店结算，不生成收付款）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ch_wx_aftersale")
public class WxAftersale extends BaseEntity {

    private Long shopId;
    private Long storeId;
    private String aftersaleId;
    private String orderId;
    private Long wxOrderId;
    private WxAftersaleType type;
    private WxAftersaleStatus status;
    private String platformStatus;
    private String productId;
    private String skuId;
    private String title;
    private Integer qty;
    private BigDecimal refundAmount;
    private String reason;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applyTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime platformUpdateTime;
    private WxAftersaleErpStatus erpStatus;
    private Long salesReturnId;
    private String salesReturnNo;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String errorMsg;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime syncTime;

    // ---------------------------------------------------------------- 只用于显示

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String shopName;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String storeName;

    /** 渠道订单在 ERP 中是否已出库（决定退货 / 退款的处理方式） */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String orderErpStatus;
}
