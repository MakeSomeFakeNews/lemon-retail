package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 门店价格方案：一组商品的价格覆盖（零售价、会员价）。门店绑定一个方案（门店表的 price_scheme_id），
 * 该门店仓库开单时方案中有的商品按方案价，没有的按总部价；方案价通过方案维护或调价单修改
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_price_scheme")
public class ScmPriceScheme extends BaseEntity {

    /** 编码：新增时留空按编码规则生成 */
    @Size(max = 32, message = "{scm.valid.priceSchemeCodeTooLong}")
    private String code;

    @NotBlank(message = "{scm.valid.priceSchemeNameRequired}")
    @Size(max = 64, message = "{scm.valid.priceSchemeNameTooLong}")
    private String name;

    private EnableStatus status;

    @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}")
    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    /** 方案中的商品数（列表） */
    @TableField(exist = false)
    private Integer itemCount;

    /** 方案明细（详情、保存） */
    @Valid
    @TableField(exist = false)
    private List<ScmPriceSchemeItem> items;
}
