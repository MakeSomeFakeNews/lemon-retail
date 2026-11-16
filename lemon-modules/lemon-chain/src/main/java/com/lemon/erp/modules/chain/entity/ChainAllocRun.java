package com.lemon.erp.modules.chain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.chain.model.AllocBasis;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 一次费用分摊：每个月一条（重新运行时替换），分摊结果见 {@link ChainAllocRecord}
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chain_alloc_run")
public class ChainAllocRun extends BaseEntity {

    /** 月份 yyyy-MM */
    private String period;
    private AllocBasis basis;
    /** 待分摊的总部费用 */
    private BigDecimal poolAmount;
    private Integer storeCount;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String createByName;
    @TableField(exist = false)
    private List<ChainAllocRecord> records;
}
