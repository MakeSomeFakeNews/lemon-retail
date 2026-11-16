package com.lemon.erp.modules.pos.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.modules.pos.model.PointsType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 积分流水
 */
@Data
@TableName("pos_points_log")
public class PosPointsLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memberId;

    private Long storeId;

    private PointsType changeType;

    private BigDecimal points;

    private BigDecimal balanceAfter;

    private Long saleId;

    private String sourceNo;

    private String reason;

    private Long createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String memberCode;

    @TableField(exist = false)
    private String memberName;

    @TableField(exist = false)
    private String storeName;

    @TableField(exist = false)
    private String createByName;
}
