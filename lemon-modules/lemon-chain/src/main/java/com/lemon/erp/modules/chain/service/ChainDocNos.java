package com.lemon.erp.modules.chain.service;

import com.lemon.erp.framework.code.DocNoCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import static com.lemon.erp.framework.code.DocNoCatalog.Type.doc;

/**
 * 连锁经营的单据编号（编码规则页面可配置）
 */
@Configuration
public class ChainDocNos {

    /** 加盟对账单 */
    public static final String STATEMENT = "JMDZ";
    /** 加盟保证金收退 */
    public static final String DEPOSIT = "BZJ";

    @Bean
    @Order(1)
    public DocNoCatalog chainDocNoCatalog() {
        return DocNoCatalog.of("chain.label.module",
                doc(STATEMENT, "chain.label.statement"),
                doc(DEPOSIT, "chain.label.deposit"));
    }
}
