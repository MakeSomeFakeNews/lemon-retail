package com.lemon.erp.modules.channel.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.channel.model.WxDeliveryStatus;
import com.lemon.erp.modules.channel.model.WxErpStatus;
import com.lemon.erp.modules.channel.model.WxOrderStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 微信小店渠道订单。收货信息是敏感数据：列表不返回，详情默认脱敏，有 channel:wxOrder:sensitive 权限时显示完整信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ch_wx_order")
public class WxOrder extends BaseEntity {

    private Long shopId;
    /** 店铺绑定的门店（写入时确定），按门店的数据权限过滤 */
    private Long storeId;
    private String orderId;
    private WxOrderStatus status;
    private Integer platformStatus;
    /** 买家标识（已脱敏） */
    private String buyer;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private BigDecimal goodsAmount;
    private BigDecimal freight;
    private BigDecimal discountAmount;
    private BigDecimal paidAmount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime platformUpdateTime;
    private WxErpStatus erpStatus;
    private Long erpOrderId;
    private String erpOrderNo;
    private Long salesOutId;
    private String salesOutNo;
    private WxDeliveryStatus deliveryStatus;
    private String deliveryCompanyId;
    private String deliveryCompanyName;
    private String waybillNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime shipTime;
    /** 处理成功后清空 */
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

    /** 商品摘要（列表）：第一个商品的标题 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String goodsSummary;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private Integer itemCount;

    /** 收货信息是否为完整信息（有查看权限） */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private boolean sensitiveVisible;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private List<WxOrderItem> items;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private List<WxAftersale> aftersales;
}
