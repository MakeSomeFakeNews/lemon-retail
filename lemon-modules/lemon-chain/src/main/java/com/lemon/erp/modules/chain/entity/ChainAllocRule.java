package com.lemon.erp.modules.chain.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.chain.model.AllocBasis;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 总部费用分摊设置（每个租户一条，没有时按销售额、分摊到直营店）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chain_alloc_rule")
public class ChainAllocRule extends BaseEntity {

    @NotNull(message = "{chain.valid.basisRequired}")
    private AllocBasis basis;

    /** 配送中心也参与分摊 */
    private Boolean includeDc;

    /** 加盟店也参与分摊 */
    private Boolean includeFranchise;

    /** 参与分摊的区域（含下级区域），为空为全部门店 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long regionId;

    @Size(max = 255, message = "{chain.valid.remarkTooLong}")
    private String remark;
}
