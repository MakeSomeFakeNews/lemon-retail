package com.lemon.erp.modules.pos.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.pos.model.PayMethod;
import com.lemon.erp.modules.pos.model.PayProviderType;
import com.lemon.erp.modules.pos.model.PaymentStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 收款 / 退款记录（退款金额为负）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pos_payment")
public class PosPayment extends BaseEntity {

    private Long saleId;

    private Long topupId;

    private Long storeId;

    private Long terminalId;

    private Long shiftId;

    private LocalDate businessDate;

    private PayMethod payMethod;

    private BigDecimal amount;

    private BigDecimal tendered;

    private BigDecimal changeAmount;

    private Long accountId;

    private PayProviderType provider;

    private PaymentStatus status;

    private String outTradeNo;

    private String tradeNo;

    private String refNo;

    private String qrCode;

    private Long origPaymentId;

    private BigDecimal refundedAmount;

    private BigDecimal points;

    private String failReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;

    private Long createBy;

    @TableField(exist = false)
    private String accountName;

    @TableField(exist = false)
    private String saleNo;
}
