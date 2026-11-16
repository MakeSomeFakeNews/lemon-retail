package com.lemon.erp.modules.dist.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.dist.model.RequisitionStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 门店要货单：要货门店（收货仓库）向供货门店（总部 / 配送中心的仓库）要货，审核后由配送单发货
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dist_requisition")
public class DistRequisition extends BaseEntity {

    private String reqNo;
    private Long storeId;
    private Long warehouseId;
    private Long supplyStoreId;
    private Long supplyWarehouseId;
    private LocalDate bizDate;
    private LocalDate expectedDate;
    private RequisitionStatus status;
    private BigDecimal totalQty;
    private BigDecimal approvedQty;
    private BigDecimal deliveredQty;
    private String remark;
    private String rejectReason;
    private String closeReason;
    private String flowInstanceId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submitTime;
    private Long auditBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;
    private Long deptId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private String supplyStoreName;
    @TableField(exist = false)
    private String supplyWarehouseName;
    @TableField(exist = false)
    private String createByName;
    @TableField(exist = false)
    private String auditByName;
    @TableField(exist = false)
    private Integer itemCount;
    @TableField(exist = false)
    private List<DistRequisitionItem> items;
}
