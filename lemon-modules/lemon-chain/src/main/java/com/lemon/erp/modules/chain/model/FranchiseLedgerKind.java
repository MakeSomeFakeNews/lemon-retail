package com.lemon.erp.modules.chain.model;

/**
 * 加盟往来种类（字典 chain_ledger_kind）：配送给加盟店生成应收，加盟店退回（退仓、收货差异退回、冲销）生成红字应收
 */
public enum FranchiseLedgerKind {
    /** 加盟配送 */
    DELIVERY,
    /** 加盟退货 */
    RETURN
}
