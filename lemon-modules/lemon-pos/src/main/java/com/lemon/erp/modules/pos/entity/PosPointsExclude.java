package com.lemon.erp.modules.pos.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.pos.model.GoodsTargetType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 不积分的商品 / 分类（分类含下级分类）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pos_points_exclude")
public class PosPointsExclude extends BaseEntity {

    private GoodsTargetType targetType;

    private Long materialId;

    private String categoryCode;

    @TableField(exist = false)
    private String materialCode;

    @TableField(exist = false)
    private String materialName;
}
