package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.modules.scm.model.PriceAdjustTarget;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 调价历史：调价单生效时每个物料（价格方案时每个方案 × 物料）一行，记录调整前后的价格；只追加、不修改
 */
@Data
@TableName("scm_price_history")
public class ScmPriceHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long adjustId;

    private Long materialId;

    private PriceAdjustTarget target;

    /** 价格方案，总部价时为空 */
    private Long schemeId;

    private BigDecimal oldRetailPrice;

    private BigDecimal newRetailPrice;

    private BigDecimal oldMemberPrice;

    private BigDecimal newMemberPrice;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime effectTime;

    @TableField(exist = false)
    private String adjustNo;
    @TableField(exist = false)
    private String schemeCode;
    @TableField(exist = false)
    private String schemeName;
    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String spec;
}
