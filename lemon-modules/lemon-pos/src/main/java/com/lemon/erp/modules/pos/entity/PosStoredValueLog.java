package com.lemon.erp.modules.pos.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.modules.pos.model.StoredValueType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 储值流水：每笔记录门店（充值为收款门店，消费 / 退回为消费门店），用于跨店结算
 */
@Data
@TableName("pos_stored_value_log")
public class PosStoredValueLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memberId;

    private Long storeId;

    private StoredValueType changeType;

    private BigDecimal amount;

    private BigDecimal balanceAfter;

    private LocalDate businessDate;

    private Long saleId;

    private Long topupId;

    private String sourceNo;

    private String remark;

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
