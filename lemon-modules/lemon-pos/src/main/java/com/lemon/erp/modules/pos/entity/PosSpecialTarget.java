package com.lemon.erp.modules.pos.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 特价活动的适用门店 / 区域
 */
@Data
@TableName("pos_special_target")
public class PosSpecialTarget {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long specialId;

    private Long targetId;
}
