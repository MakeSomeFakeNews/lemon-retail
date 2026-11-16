package com.lemon.erp.modules.store.service;

import com.lemon.erp.framework.code.DocNoCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import static com.lemon.erp.framework.code.DocNoCatalog.Type.master;

/**
 * 门店模块的编码规则（编码规则页面可配置）
 */
@Configuration
public class StoreDocNos {

    /** 门店编码：新增时编码留空则自动生成 */
    public static final String STORE = "MD";

    @Bean
    @Order(0)
    public DocNoCatalog storeDocNoCatalog() {
        return DocNoCatalog.of("store.label.module", master(STORE, "store.label.storeCode"));
    }
}
