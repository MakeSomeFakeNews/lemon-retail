package com.lemon.erp.modules.pos.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 满赠的赠品：每份赠送数量（基本单位）
 */
@Data
@TableName("pos_promotion_gift")
public class PosPromotionGift {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long promotionId;

    private Long materialId;

    private BigDecimal qty;

    @TableField(exist = false)
    private String materialCode;

    @TableField(exist = false)
    private String materialName;

    @TableField(exist = false)
    private String unit;
}
