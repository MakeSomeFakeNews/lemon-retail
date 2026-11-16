package com.lemon.erp.modules.dist.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.dist.model.DeliveryKind;
import com.lemon.erp.modules.dist.model.DiscrepancyHandling;
import com.lemon.erp.modules.dist.model.DiscrepancyKind;
import com.lemon.erp.modules.dist.model.DiscrepancyStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 收货差异：少收 / 破损的数量在途（差异调拨，收货仓库 → 发货仓库），多收的数量未入账，由总部判定责任方后处理
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dist_discrepancy")
public class DistDiscrepancy extends BaseEntity {

    private Long deliveryId;
    private Long deliveryItemId;
    private Long materialId;
    private String batchNo;
    private DiscrepancyKind kind;
    private BigDecimal qty;
    private BigDecimal price;
    private BigDecimal amount;
    private DiscrepancyStatus status;
    private Long transferId;
    private String reason;
    private DiscrepancyHandling handling;
    private String ioCategory;
    private String handleDocNos;
    private String handleRemark;
    private Long handleBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String deliveryNo;
    @TableField(exist = false)
    private DeliveryKind deliveryKind;
    @TableField(exist = false)
    private LocalDate receiveDate;
    @TableField(exist = false)
    private String fromStoreName;
    @TableField(exist = false)
    private String fromWarehouseName;
    @TableField(exist = false)
    private String toStoreName;
    @TableField(exist = false)
    private String toWarehouseName;
    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String spec;
    @TableField(exist = false)
    private String unit;
    @TableField(exist = false)
    private String transferNo;
    @TableField(exist = false)
    private String createByName;
    @TableField(exist = false)
    private String handleByName;
}
