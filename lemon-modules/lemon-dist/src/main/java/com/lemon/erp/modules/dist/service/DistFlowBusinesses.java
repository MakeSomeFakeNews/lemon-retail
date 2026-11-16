package com.lemon.erp.modules.dist.service;

import com.lemon.erp.framework.workflow.FlowBusiness;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.lemon.erp.framework.workflow.FlowBusiness.Variable.number;
import static com.lemon.erp.framework.workflow.FlowBusiness.Variable.text;

/**
 * 配送模块接入审批的流程及发起时传入的流程变量，与 {@link DistRequisitionService#submit} 传入的变量一致；名称为消息键
 */
@Configuration
public class DistFlowBusinesses {

    @Bean
    public FlowBusiness requisitionFlow() {
        return FlowBusiness.of(DistRequisitionService.PROCESS_KEY, "dist.label.requisitionApproval",
                text("reqNo", "dist.label.reqNo"),
                text("storeName", "dist.label.requestStore"),
                number("totalQty", "dist.label.totalQty"),
                number("itemCount", "dist.label.itemCount"));
    }
}
