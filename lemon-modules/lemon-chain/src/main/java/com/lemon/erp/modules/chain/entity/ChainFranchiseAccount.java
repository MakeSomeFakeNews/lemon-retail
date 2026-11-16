package com.lemon.erp.modules.chain.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.framework.store.StoreStatus;
import com.lemon.erp.modules.store.model.FranchiseSettleMode;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 加盟商账户：每个加盟店一个，关联一个供应链客户（首次使用时按加盟商名称自动创建），
 * 加盟应收、预收款（充值）、收款核销、往来对账单都复用财务模块按该客户处理
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chain_franchise_account")
public class ChainFranchiseAccount extends BaseEntity {

    private Long storeId;

    /** 加盟商对应的客户（scm_customer） */
    private Long customerId;

    /** 信用额度（月结的加盟店），为空不限；预付货款的加盟店按预付余额控制 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal creditLimit;

    private String remark;

    // ---------------- 列表显示（门店资料、财务余额）

    @TableField(exist = false)
    private String storeCode;
    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private String franchisee;
    @TableField(exist = false)
    private FranchiseSettleMode settleMode;
    @TableField(exist = false)
    private StoreStatus storeStatus;
    @TableField(exist = false)
    private LocalDate contractStart;
    @TableField(exist = false)
    private LocalDate contractEnd;
    @TableField(exist = false)
    private String customerCode;
    @TableField(exist = false)
    private String customerName;
    /** 未核销的应收（含红字） */
    @TableField(exist = false)
    private BigDecimal receivable;
    /** 未冲抵的预收款 */
    @TableField(exist = false)
    private BigDecimal prepaid;
    /** 应收余额 = 应收 − 预收（负数为预付结余） */
    @TableField(exist = false)
    private BigDecimal balance;
    /** 可用额度：预付货款为预付结余（−余额），月结为 额度 − 余额，不限额度为空 */
    @TableField(exist = false)
    private BigDecimal available;
    /** 保证金余额 = 收取 − 退还 */
    @TableField(exist = false)
    private BigDecimal depositBalance;
}
