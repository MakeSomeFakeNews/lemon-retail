package com.lemon.erp.modules.chain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 费用分摊结果：某月分摊给某门店的总部费用
 */
@Data
@TableName("chain_alloc_record")
public class ChainAllocRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long runId;
    private String period;
    private Long storeId;
    /** 分摊依据的数值：销售额 / 面积 / 1 */
    private BigDecimal basisValue;
    /** 分摊比例（0–1） */
    private BigDecimal ratio;
    private BigDecimal amount;

    @TableField(exist = false)
    private String storeCode;
    @TableField(exist = false)
    private String storeName;
}
