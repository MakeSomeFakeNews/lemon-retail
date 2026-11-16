package com.lemon.erp.modules.chain.service;

import com.lemon.erp.framework.rule.BizRule;
import com.lemon.erp.framework.rule.BizRuleProvider;
import com.lemon.erp.modules.chain.model.FranchiseCreditControl;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 连锁经营的业务参数（模块、名称、说明为消息键）
 */
@Component
public class ChainBizRules implements BizRuleProvider {

    private static final String MODULE = "chain.label.module";

    /** 加盟店货款额度控制：不控制 / 提示 / 禁止 */
    public static final String CREDIT_CONTROL = "chain.franchise.creditControl";
    /** 补货建议：日均销量按最近多少天计算 */
    public static final String SALES_DAYS = "chain.replenish.salesDays";
    /** 补货建议：默认到货天数（门店可单独设置） */
    public static final String LEAD_DAYS = "chain.replenish.leadDays";
    /** 补货建议：默认安全天数（门店可单独设置） */
    public static final String SAFETY_DAYS = "chain.replenish.safetyDays";

    @Override
    public List<BizRule> rules() {
        return List.of(
                BizRule.select(CREDIT_CONTROL, MODULE, "chain.label.creditControl", FranchiseCreditControl.BLOCK, FranchiseCreditControl.class,
                        "chain.label.creditControlRemark"),
                BizRule.number(SALES_DAYS, MODULE, "chain.label.salesDays", 30, 1, 365, "chain.label.salesDaysRemark"),
                BizRule.number(LEAD_DAYS, MODULE, "chain.label.leadDays", 2, 0, 365, "chain.label.leadDaysRemark"),
                BizRule.number(SAFETY_DAYS, MODULE, "chain.label.safetyDays", 3, 0, 365, "chain.label.safetyDaysRemark"));
    }
}
