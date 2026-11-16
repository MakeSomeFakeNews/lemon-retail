package com.lemon.erp.modules.dist.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 配送单 / 退仓单明细
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dist_delivery_item")
public class DistDeliveryItem extends BaseEntity {

    private Long deliveryId;
    private Long requisitionId;
    private Long requisitionItemId;
    private Long materialId;
    private Long locationId;
    private String batchNo;
    private String serialNos;
    private BigDecimal qty;
    /** 配送价：草稿为指定的价格（为空按规则），发货后为实际调拨价 */
    private BigDecimal price;
    private BigDecimal amount;
    private BigDecimal costPrice;
    private BigDecimal receivedQty;
    private String remark;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String spec;
    @TableField(exist = false)
    private String unit;
    @TableField(exist = false)
    private Integer serialEnabled;
    @TableField(exist = false)
    private String locationName;
    @TableField(exist = false)
    private String requisitionNo;
}
