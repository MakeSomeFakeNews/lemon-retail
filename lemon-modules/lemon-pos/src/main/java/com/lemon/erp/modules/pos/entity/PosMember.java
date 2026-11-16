package com.lemon.erp.modules.pos.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.pos.model.MemberGender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会员：独立于客户资料，全部门店通用；累计消费、消费次数由 POS 结算维护，积分、储值余额在 M4 启用
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pos_member")
public class PosMember extends BaseEntity {

    /** 留空时按编码规则（HY）自动生成 */
    @Pattern(regexp = "^[A-Za-z0-9_-]{0,32}$", message = "{pos.valid.codeFormat}")
    private String memberCode;

    @Size(max = 64, message = "{pos.valid.nameTooLong}")
    private String name;

    @NotBlank(message = "{pos.valid.phoneRequired}")
    /** 修改时没有查看手机号权限的人提交的是脱敏号码（含 *），保持原号码 */
    @Pattern(regexp = "^[0-9+*-]{5,20}$", message = "{pos.valid.phoneFormat}")
    private String phone;

    private MemberGender gender;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate birthday;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long levelId;

    /** 开卡门店 */
    private Long storeId;

    private EnableStatus status;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal totalAmount;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer visitCount;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastVisitTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal points;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal balance;

    @Size(max = 255, message = "{pos.valid.remarkTooLong}")
    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String levelName;

    /** 会员折扣（%），收银取价使用 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private BigDecimal discountRate;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String storeName;
}
