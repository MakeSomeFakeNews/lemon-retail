package com.lemon.erp.modules.chain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.chain.model.FranchiseDepositKind;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 加盟保证金收退记录：只登记（保证金是负债，不是货款，不进入加盟应收；资金的账务由出纳另行处理）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chain_franchise_deposit")
public class ChainFranchiseDeposit extends BaseEntity {

    private String depositNo;
    private Long storeId;
    private FranchiseDepositKind kind;
    private LocalDate bizDate;
    private BigDecimal amount;
    /** 收款 / 付款的结算账户（可空，只做记录） */
    private Long accountId;
    private String remark;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private String accountName;
    @TableField(exist = false)
    private String createByName;
}
