package com.lemon.erp.modules.dist.service;

import com.lemon.erp.framework.code.DocNoCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import static com.lemon.erp.framework.code.DocNoCatalog.Type.doc;

/**
 * 配送模块的单据编号（编码规则页面可配置）
 */
@Configuration
public class DistDocNos {

    /** 要货单 */
    public static final String REQUISITION = "YH";
    /** 配送单 */
    public static final String DELIVERY = "PS";
    /** 门店退仓单 */
    public static final String RETURN = "TC";

    @Bean
    @Order(1)
    public DocNoCatalog distDocNoCatalog() {
        return DocNoCatalog.of("dist.label.module",
                doc(REQUISITION, "dist.label.requisition"),
                doc(DELIVERY, "dist.label.delivery"),
                doc(RETURN, "dist.label.storeReturn"));
    }
}
