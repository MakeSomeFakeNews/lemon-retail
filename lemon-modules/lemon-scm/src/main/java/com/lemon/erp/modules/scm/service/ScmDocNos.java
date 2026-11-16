package com.lemon.erp.modules.scm.service;

import com.lemon.erp.framework.code.DocNoCatalog;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.TradeKind;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.lemon.erp.framework.code.DocNoCatalog.Type.doc;
import static com.lemon.erp.framework.code.DocNoCatalog.Type.master;

/**
 * 供应链模块的单据编号（编码规则页面可配置），规则标识与 docNoService.next 的参数一致；名称为消息键
 */
@Configuration
public class ScmDocNos {

    /** 供应商、客户编码规则标识：新增时编码留空则自动生成 */
    public static final String SUPPLIER = "GYS";
    public static final String CUSTOMER = "KH";
    /** 门店价格方案编码 */
    public static final String PRICE_SCHEME = "JGFA";

    /** 单据编号规则标识 */
    public static final String ORDER_CHANGE = "BG";
    public static final String DELIVERY = "FH";
    /** 调价单 */
    public static final String PRICE_ADJUST = "TJ";

    @Bean
    @Order(1)
    public DocNoCatalog scmDocNoCatalog() {
        List<DocNoCatalog.Type> types = new ArrayList<>();
        Arrays.stream(OrderKind.values()).forEach(k -> types.add(doc(k.getPrefix(), k.getLabelKey())));
        types.add(doc(ORDER_CHANGE, "scm.label.orderChange"));
        Arrays.stream(TradeKind.values()).forEach(k -> types.add(doc(k.getPrefix(), k.getLabelKey())));
        types.add(doc(DELIVERY, "scm.label.deliveryNotice"));
        types.add(doc(PRICE_ADJUST, "scm.label.priceAdjust"));
        types.add(master(SUPPLIER, "scm.label.supplierCode"));
        types.add(master(CUSTOMER, "scm.label.customerCode"));
        types.add(master(PRICE_SCHEME, "scm.label.priceSchemeCode"));
        return new DocNoCatalog("scm.label.supplyChainManagement", types);
    }
}
