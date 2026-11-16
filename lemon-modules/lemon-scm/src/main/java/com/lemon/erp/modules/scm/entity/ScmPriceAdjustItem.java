package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 调价单明细：物料的新价格，为空的价格列不调整（总部价只调整零售价）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_price_adjust_item")
public class ScmPriceAdjustItem extends BaseEntity {

    private Long adjustId;

    private Long materialId;

    /** 新零售价 */
    private BigDecimal retailPrice;

    /** 新会员价（只用于价格方案） */
    private BigDecimal memberPrice;

    private String remark;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String spec;
    @TableField(exist = false)
    private String unit;
    /** 当前总部零售价（详情中对照显示） */
    @TableField(exist = false)
    private BigDecimal hqRetailPrice;
}
