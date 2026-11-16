package com.lemon.erp.modules.chain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.modules.chain.model.FranchiseLedgerKind;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 加盟往来：配送给加盟店 / 加盟店退回的调拨确认（含冲销）各记一笔，同时生成的加盟应收（红字）单关联在这里。只追加
 */
@Data
@TableName("chain_franchise_ledger")
public class ChainFranchiseLedger {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long storeId;
    private FranchiseLedgerKind kind;
    private Long transferId;
    private String transferNo;
    /** 来源单号（配送单 / 退仓单号），手工调拨为空 */
    private String sourceNo;
    private LocalDate bizDate;
    /** 应收金额：配送为正数，退回为负数 */
    private BigDecimal amount;
    private Long billId;
    private String billNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String storeName;
}
