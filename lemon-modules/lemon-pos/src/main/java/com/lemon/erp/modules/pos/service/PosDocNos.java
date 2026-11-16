package com.lemon.erp.modules.pos.service;

import com.lemon.erp.framework.code.DocNoCatalog;
import com.lemon.erp.framework.code.DocNoCatalog.DateFormat;
import com.lemon.erp.framework.code.DocNoCatalog.ResetCycle;
import com.lemon.erp.framework.code.DocNoCatalog.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import static com.lemon.erp.framework.code.DocNoCatalog.Type.doc;
import static com.lemon.erp.framework.code.DocNoCatalog.Type.master;

/**
 * 零售前台的编码规则（编码规则页面可配置）。POS 单据量大（单店日 2000 单 × 多门店），销售、退货、支付单号默认 6 / 8 位当天流水
 */
@Configuration
public class PosDocNos {

    /** POS 销售单 */
    public static final String SALE = "LS";
    /** POS 退货单 */
    public static final String RETURN = "LT";
    /** 班次 */
    public static final String SHIFT = "BC";
    /** 收银台编码 */
    public static final String TERMINAL = "SYT";
    /** 会员号 */
    public static final String MEMBER = "HY";
    /** 支付单号（商户订单号、退款单号） */
    public static final String PAYMENT = "ZF";
    /** 储值充值单 */
    public static final String TOPUP = "CZ";

    @Bean
    @Order(1)
    public DocNoCatalog posDocNoCatalog() {
        return DocNoCatalog.of("pos.label.module",
                new Type(SALE, "pos.label.saleDoc", DateFormat.YYYYMMDD, 6, ResetCycle.DAY),
                new Type(RETURN, "pos.label.returnDoc", DateFormat.YYYYMMDD, 5, ResetCycle.DAY),
                new Type(PAYMENT, "pos.label.paymentNo", DateFormat.YYYYMMDD, 8, ResetCycle.DAY),
                doc(SHIFT, "pos.label.shiftNo"),
                doc(TOPUP, "pos.label.topupDoc"),
                master(TERMINAL, "pos.label.terminalCode"),
                master(MEMBER, "pos.label.memberCode"));
    }
}
