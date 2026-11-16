package com.lemon.erp.modules.fin.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * 应收应付来源：应收应付单的来源（字典 fin_bill_source）
 */
public enum BillSource {
    /** 销售出库 */
    SALES_OUT,
    /** 销售退货 */
    SALES_RETURN,
    /** 采购入库 */
    PURCHASE_IN,
    /** 采购退货 */
    PURCHASE_RETURN,
    /** 期初 / 手工 */
    MANUAL,
    /** 冲账单（垫付运费、返利、赔偿等抵扣往来款，红字） */
    OFFSET,
    /** 订单尾差调整：部分出入库的订单关闭时，已出入库金额与订单口径的差额（蓝字或红字） */
    ORDER_ADJUST,
    /** 采购运费：采购入库的运费（供应商代垫或付承运商），计入存货成本 */
    PURCHASE_FREIGHT,
    /** 销售运费：销售出库单的运费，来源为该出库单（月结的承运商应付、代垫运费的客户应收） */
    SALES_FREIGHT,
    /** 销售退货运费：客户代垫的退货运费，来源为该退货单（红字应收） */
    SALES_RETURN_FREIGHT,
    /** 采购退货运费：本公司替供应商垫付的退货运费，来源为该退货单（红字应付、月结的承运商应付） */
    PURCHASE_RETURN_FREIGHT,
    /** 调拨费用：调拨单的配送费，来源为该调拨单（月结的承运商应付；现付的费用支出单也以此为来源类型） */
    TRANSFER_FEE,
    /** 门店日结：POS 零售收款 / 退款按结算账户汇总生成的其他收入单、费用支出单，来源为日结记录 */
    POS_DAILY_CLOSE,
    /** 加盟配送：配送给加盟店（调拨确认）生成的加盟商应收，来源为调拨单（来源单号为配送单号） */
    FRANCHISE_DELIVERY,
    /** 加盟退货：加盟店退仓、收货少收 / 破损退回、配送冲销生成的红字应收，来源为调拨单 */
    FRANCHISE_RETURN;

    /** 供应链出入库生成的单据（与 scm TradeKind 同名） */
    public static final Set<BillSource> TRADE = EnumSet.of(SALES_OUT, SALES_RETURN, PURCHASE_IN, PURCHASE_RETURN);

    /** 是否由供应链出入库生成 */
    public boolean isTrade() {
        return TRADE.contains(this);
    }
}
