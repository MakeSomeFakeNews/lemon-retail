package com.lemon.erp.modules.pos.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 特价活动商品：特价或折扣二选一，每单限购（基本单位）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pos_special_item")
public class PosSpecialItem extends BaseEntity {

    private Long specialId;

    private Long materialId;

    private BigDecimal specialPrice;

    private BigDecimal discountRate;

    private BigDecimal limitQty;

    @TableField(exist = false)
    private String materialCode;

    @TableField(exist = false)
    private String materialName;

    @TableField(exist = false)
    private String spec;

    @TableField(exist = false)
    private String unit;

    @TableField(exist = false)
    private BigDecimal retailPrice;
}
