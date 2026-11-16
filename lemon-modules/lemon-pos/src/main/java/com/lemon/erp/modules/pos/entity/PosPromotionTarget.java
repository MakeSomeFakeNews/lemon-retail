package com.lemon.erp.modules.pos.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 满减活动的适用门店 / 区域
 */
@Data
@TableName("pos_promotion_target")
public class PosPromotionTarget {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long promotionId;

    private Long targetId;
}
