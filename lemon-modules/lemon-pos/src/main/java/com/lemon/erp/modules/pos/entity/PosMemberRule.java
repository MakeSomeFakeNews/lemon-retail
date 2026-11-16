package com.lemon.erp.modules.pos.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 积分与储值规则（每个租户一条，没有时按默认值，见 {@link #defaults()}）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pos_member_rule")
public class PosMemberRule extends BaseEntity {

    @NotNull(message = "{pos.valid.ruleRequired}")
    private Boolean pointsEnabled;

    /** 每消费 X 元 */
    @NotNull(message = "{pos.valid.ruleRequired}")
    @DecimalMin(value = "0.01", message = "{pos.valid.amountPositive}")
    @Digits(integer = 14, fraction = 2, message = "{pos.valid.amountScale}")
    private BigDecimal earnAmount;

    /** 获得 N 积分 */
    @NotNull(message = "{pos.valid.ruleRequired}")
    @DecimalMin(value = "0", message = "{pos.valid.amountNonNegative}")
    @Digits(integer = 14, fraction = 2, message = "{pos.valid.amountScale}")
    private BigDecimal earnPoints;

    @NotNull(message = "{pos.valid.ruleRequired}")
    private Boolean redeemEnabled;

    /** 多少积分抵 1 元 */
    @NotNull(message = "{pos.valid.ruleRequired}")
    @Min(value = 1, message = "{pos.valid.redeemPointsRange}")
    @Max(value = 1000000, message = "{pos.valid.redeemPointsRange}")
    private Integer redeemPoints;

    /** 积分抵现最多占应收的百分比 */
    @NotNull(message = "{pos.valid.ruleRequired}")
    @DecimalMin(value = "0", message = "{pos.valid.discountRateRange}")
    @DecimalMax(value = "100", message = "{pos.valid.discountRateRange}")
    private BigDecimal redeemMaxRate;

    @NotNull(message = "{pos.valid.ruleRequired}")
    private Boolean storedValueEnabled;

    @Size(max = 255, message = "{pos.valid.remarkTooLong}")
    private String remark;

    /** 没有保存过规则时的默认值：每消费 1 元得 1 积分，100 积分抵 1 元、最多抵应收的 50%，启用储值 */
    public static PosMemberRule defaults() {
        PosMemberRule r = new PosMemberRule();
        r.setPointsEnabled(true);
        r.setEarnAmount(BigDecimal.ONE);
        r.setEarnPoints(BigDecimal.ONE);
        r.setRedeemEnabled(true);
        r.setRedeemPoints(100);
        r.setRedeemMaxRate(new BigDecimal("50"));
        r.setStoredValueEnabled(true);
        return r;
    }
}
