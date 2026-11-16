package com.lemon.erp.modules.pos.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 满减活动的参与商品（指定商品）或分类（指定分类，含下级分类）
 */
@Data
@TableName("pos_promotion_goods")
public class PosPromotionGoods {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long promotionId;

    private Long materialId;

    private String categoryCode;

    @TableField(exist = false)
    private String materialCode;

    @TableField(exist = false)
    private String materialName;
}
