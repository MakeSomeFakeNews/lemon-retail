package com.lemon.erp.modules.channel.model;

/**
 * 同步的触发方式（字典 ch_wx_sync_trigger）
 */
public enum WxSyncTrigger {
    /** 手工「立即同步」 */
    MANUAL,
    /** 定时任务 */
    SCHEDULE,
    /** 小店消息推送 */
    CALLBACK
}
