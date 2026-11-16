package com.lemon.erp.modules.scm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.scm.model.PriceAdjustStatus;
import com.lemon.erp.modules.scm.model.PriceAdjustTarget;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 调价单：调整总部零售价或一个 / 多个门店价格方案的方案价；审核后立即生效或到生效时间由定时任务生效，生效时记录调价历史
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scm_price_adjust")
public class ScmPriceAdjust extends BaseEntity {

    private String adjustNo;

    private PriceAdjustTarget target;

    /** 定时生效时间，为空审核后立即生效 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime effectiveTime;

    private PriceAdjustStatus status;

    private String reason;

    private String flowInstanceId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approveTime;

    /** 实际生效时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime effectTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String createByName;
    /** 调价的价格方案（target 为 SCHEME 时） */
    @TableField(exist = false)
    private List<Long> schemeIds;
    /** 价格方案名称，与 schemeIds 对应 */
    @TableField(exist = false)
    private List<String> schemeNames;
    @TableField(exist = false)
    private Integer itemCount;
    @TableField(exist = false)
    private List<ScmPriceAdjustItem> items;
}
