package com.lemon.erp.modules.pos.service;

import com.lemon.erp.framework.rule.BizRule;
import com.lemon.erp.framework.rule.BizRuleProvider;
import com.lemon.erp.modules.pos.model.PosRounding;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 零售前台的业务参数（模块、名称、说明为消息键）。最低售价控制沿用供应链「价格控制」中各商品的最低销售价与控制方式（提示 / 禁止）
 */
@Component
public class PosBizRules implements BizRuleProvider {

    private static final String MODULE = "pos.label.module";

    /** 有「收银折扣」权限的人，手工优惠（改价、单行折扣、整单优惠）最多占取价金额的百分比；有「超限折扣」权限的不受限 */
    public static final String MAX_DISCOUNT_RATE = "pos.discount.maxRate";
    /** 抹零规则 */
    public static final String ROUNDING = "pos.rounding";

    @Override
    public List<BizRule> rules() {
        return List.of(
                BizRule.decimal(MAX_DISCOUNT_RATE, MODULE, "pos.label.maxDiscountRate", "20", 2, 0, 100, "pos.label.maxDiscountRateRemark"),
                BizRule.select(ROUNDING, MODULE, "pos.label.rounding", PosRounding.NONE, PosRounding.class, "pos.label.roundingRemark"));
    }

    @Override
    public Set<String> publicKeys() {
        return Set.of(ROUNDING);
    }
}
