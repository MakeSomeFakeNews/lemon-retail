package com.lemon.erp.modules.pos.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 会员等级：会员折扣、自动升级门槛
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pos_member_level")
public class PosMemberLevel extends BaseEntity {

    @NotBlank(message = "{pos.valid.levelNameRequired}")
    @Size(max = 32, message = "{pos.valid.levelNameTooLong}")
    private String name;

    @NotNull(message = "{pos.valid.discountRateRequired}")
    @DecimalMin(value = "1", message = "{pos.valid.discountRateRange}")
    @DecimalMax(value = "100", message = "{pos.valid.discountRateRange}")
    private BigDecimal discountRate;

    @DecimalMin(value = "0", message = "{pos.valid.upgradeAmountNonNegative}")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal upgradeAmount;

    private Integer sort;

    private EnableStatus status;

    @Size(max = 255, message = "{pos.valid.remarkTooLong}")
    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private Long memberCount;
}
