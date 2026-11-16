package com.lemon.erp.modules.dist.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 要货单明细：要货数量、建议量、批准数量（审核时填写）、已配送数量（配送单发货时回写）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dist_requisition_item")
public class DistRequisitionItem extends BaseEntity {

    private Long requisitionId;
    private Long materialId;
    private BigDecimal qty;
    private BigDecimal suggestQty;
    private BigDecimal approvedQty;
    private BigDecimal deliveredQty;
    private String remark;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String spec;
    @TableField(exist = false)
    private String unit;
    /** 要货门店收货仓库的现存数量（详情中显示） */
    @TableField(exist = false)
    private BigDecimal stockQty;
    /** 收货仓库的在途数量（已调出未收货的调拨） */
    @TableField(exist = false)
    private BigDecimal transitQty;
    /** 供货仓库的可用数量（现存 − 冻结） */
    @TableField(exist = false)
    private BigDecimal supplyAvailable;
}
