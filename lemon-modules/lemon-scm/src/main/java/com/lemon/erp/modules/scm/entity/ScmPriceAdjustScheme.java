package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 调价单调整的价格方案
 */
@Data
@TableName("scm_price_adjust_scheme")
public class ScmPriceAdjustScheme {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long adjustId;

    private Long schemeId;
}
