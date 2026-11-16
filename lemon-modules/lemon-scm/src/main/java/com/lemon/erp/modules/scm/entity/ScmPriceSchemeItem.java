package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 价格方案明细：方案 × 物料，各价格列为空表示不覆盖（按总部价）。价格含税、基本单位、本位币
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_price_scheme_item")
public class ScmPriceSchemeItem extends BaseEntity {

    private Long schemeId;

    @NotNull(message = "{scm.valid.selectMaterial}")
    private Long materialId;

    /** 零售价，为空按总部零售价 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @DecimalMin(value = "0", message = "{scm.valid.unitPriceCannotLess}")
    @Digits(integer = 14, fraction = 4, message = "{scm.valid.unitPriceAllowsMostDecimalPlaces}")
    private BigDecimal retailPrice;

    /** 会员价，为空不覆盖 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @DecimalMin(value = "0", message = "{scm.valid.unitPriceCannotLess}")
    @Digits(integer = 14, fraction = 4, message = "{scm.valid.unitPriceAllowsMostDecimalPlaces}")
    private BigDecimal memberPrice;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String spec;
    @TableField(exist = false)
    private String unit;
    /** 总部零售价（详情中对照显示） */
    @TableField(exist = false)
    private BigDecimal hqRetailPrice;
}
