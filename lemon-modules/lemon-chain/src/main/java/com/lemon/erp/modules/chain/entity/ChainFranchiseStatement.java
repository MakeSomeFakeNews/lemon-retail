package com.lemon.erp.modules.chain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.chain.model.FranchiseStatementStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 加盟对账单（月结）：一个加盟店一个月一张，期初 + 配送 − 退货 − 收款（收款、预收）± 其他 = 期末，取自财务的往来对账单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chain_franchise_statement")
public class ChainFranchiseStatement extends BaseEntity {

    private String statementNo;
    private Long storeId;
    private Long customerId;
    /** 对账月份 yyyy-MM */
    private String period;
    private LocalDate beginDate;
    private LocalDate endDate;
    private BigDecimal opening;
    private BigDecimal deliveryAmount;
    /** 退货（正数，冲减） */
    private BigDecimal returnAmount;
    /** 收款（收款单、预收款、对冲，正数，冲减） */
    private BigDecimal paymentAmount;
    /** 其他应收（期初 / 手工、冲账单等，带符号） */
    private BigDecimal otherAmount;
    private BigDecimal closing;
    private FranchiseStatementStatus status;
    private Long confirmBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime confirmTime;
    private String remark;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String storeCode;
    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private String franchisee;
    @TableField(exist = false)
    private String confirmByName;
    /** 明细（详情、打印）：财务往来对账单的逐笔明细 */
    @TableField(exist = false)
    private List<?> rows;
}
