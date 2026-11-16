package com.lemon.erp.modules.chain.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 门店的补货参数：到货天数（提前期）、安全天数，为空取业务参数的默认值
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chain_replenish_param")
public class ChainReplenishParam extends BaseEntity {

    private Long storeId;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer leadDays;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer safetyDays;
}
