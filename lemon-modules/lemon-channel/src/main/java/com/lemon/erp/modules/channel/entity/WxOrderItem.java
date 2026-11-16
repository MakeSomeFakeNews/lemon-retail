package com.lemon.erp.modules.channel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 渠道订单明细：订单的优惠按售价金额比例分摊到各行（实付），生成销售订单时按映射的物料和换算率换算为基本单位
 */
@Data
@TableName("ch_wx_order_item")
public class WxOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long wxOrderId;
    private String productId;
    private String skuId;
    private String title;
    private String skuAttrs;
    /** 数量（件） */
    private Integer qty;
    private BigDecimal salePrice;
    /** 本行实付（订单优惠已分摊，不含运费） */
    private BigDecimal paidAmount;
    /** 发货前已退款的数量（不再出库） */
    private Integer refundedQty;
    private Long materialId;
    private BigDecimal unitRatio;
    private Long erpOrderItemId;

    // ---------------------------------------------------------------- 只用于显示

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String materialCode;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String materialName;

    /** 当前映射的物料（未生成销售订单时显示映射情况） */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private Long mappedMaterialId;
}
