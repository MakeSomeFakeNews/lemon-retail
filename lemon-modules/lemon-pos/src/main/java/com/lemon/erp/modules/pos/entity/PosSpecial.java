package com.lemon.erp.modules.pos.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.pos.model.SaleChannel;
import com.lemon.erp.modules.pos.model.SpecialScope;
import com.lemon.erp.modules.pos.model.SpecialStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 特价活动
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pos_special")
public class PosSpecial extends BaseEntity {

    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private SaleChannel channel;

    private SpecialScope storeScope;

    private Integer memberOnly;

    private SpecialStatus status;

    private String remark;

    private Long createBy;

    @TableField(exist = false)
    private String createByName;

    @TableField(exist = false)
    private Integer itemCount;

    /** 适用的门店 / 区域 ID */
    @TableField(exist = false)
    private List<Long> targetIds;

    /** 适用的门店 / 区域名称（列表、详情显示） */
    @TableField(exist = false)
    private List<String> targetNames;

    @TableField(exist = false)
    private List<PosSpecialItem> items;
}
