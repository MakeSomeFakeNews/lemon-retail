package com.lemon.erp.modules.pos.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.modules.pos.model.PayMethod;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 门店日结的收款明细：支付方式 × 结算账户
 */
@Data
@TableName("pos_daily_close_pay")
public class PosDailyClosePay {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long closeId;

    private PayMethod payMethod;

    private Long accountId;

    private BigDecimal receiptAmount;

    private BigDecimal refundAmount;

    private Integer payCount;

    @TableField(exist = false)
    private String accountName;
}
