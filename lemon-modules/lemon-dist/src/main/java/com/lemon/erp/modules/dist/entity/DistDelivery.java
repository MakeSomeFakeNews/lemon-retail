package com.lemon.erp.modules.dist.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.framework.store.StoreType;
import com.lemon.erp.modules.dist.model.DeliveryKind;
import com.lemon.erp.modules.dist.model.DistDeliveryStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 配送单 / 门店退仓单：发货时生成在途调拨（发货仓库调出），收货时调入（差异见 {@link DistDiscrepancy}）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dist_delivery")
public class DistDelivery extends BaseEntity {

    private String deliveryNo;
    private DeliveryKind kind;
    private Long fromStoreId;
    private Long fromWarehouseId;
    private Long toStoreId;
    private Long toWarehouseId;
    private LocalDate bizDate;
    private LocalDate receiveDate;
    private DistDeliveryStatus status;
    private Long transferId;
    private BigDecimal totalQty;
    private BigDecimal receivedQty;
    private BigDecimal totalAmount;
    private String remark;
    private String rejectReason;
    private Long approveBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approveTime;
    private Long shipBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime shipTime;
    private Long receiveBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime receiveTime;
    private Long deptId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String fromStoreName;
    @TableField(exist = false)
    private String fromWarehouseName;
    @TableField(exist = false)
    private String toStoreName;
    /** 收货门店类型（加盟店按配送价结算） */
    @TableField(exist = false)
    private StoreType toStoreType;
    @TableField(exist = false)
    private String toWarehouseName;
    @TableField(exist = false)
    private String transferNo;
    @TableField(exist = false)
    private String createByName;
    @TableField(exist = false)
    private String shipByName;
    @TableField(exist = false)
    private String receiveByName;
    @TableField(exist = false)
    private String approveByName;
    /** 待处理的收货差异条数 */
    @TableField(exist = false)
    private Integer pendingDiffs;
    @TableField(exist = false)
    private List<DistDeliveryItem> items;
    @TableField(exist = false)
    private List<DistDiscrepancy> discrepancies;
}
