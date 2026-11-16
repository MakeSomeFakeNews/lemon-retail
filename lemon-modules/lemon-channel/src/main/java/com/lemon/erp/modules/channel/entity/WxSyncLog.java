package com.lemon.erp.modules.channel.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.modules.channel.model.WxSyncTrigger;
import com.lemon.erp.modules.channel.model.WxSyncType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 同步日志：每次同步（一个店铺、一种类型）一条
 */
@Data
@TableName("ch_wx_sync_log")
public class WxSyncLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long shopId;
    private WxSyncType syncType;
    private WxSyncTrigger syncTrigger;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private Boolean success;
    private String message;
    private Long durationMs;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String shopName;
}
