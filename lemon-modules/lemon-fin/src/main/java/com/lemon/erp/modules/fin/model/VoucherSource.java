package com.lemon.erp.modules.fin.model;

/**
 * 凭证来源，与字典 fin_voucher_source 对应。业务生成的凭证删除后，来源单据可以重新生成
 */
public enum VoucherSource {
    MANUAL,
    /** 应收单 */
    AR,
    /** 应付单 */
    AP,
    /** 收款单 */
    RECEIPT,
    /** 付款单 */
    PAYMENT,
    /** 期末结转损益（结账时自动生成并记账） */
    CLOSE,
    /** 预收预付及冲抵 */
    ADVANCE,
    /** 坏账核销 */
    BAD_DEBT,
    /** 红字冲销 */
    REVERSAL,
    /** 增值税结转、附加税计提 */
    TAX,
    /** 期末调汇 */
    FX,
    /** 账户调拨 */
    TRANSFER,
    /** 其他收支单（其他收入、费用支出） */
    INCOME_EXPENSE,
    /** 应收应付对冲 */
    NETTING
}
