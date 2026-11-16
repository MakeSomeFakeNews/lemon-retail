package com.lemon.erp.modules.channel.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 微信小店定时同步（按租户执行）：每个启用店铺按自己的设置拉取订单、售后单，到达间隔时推送库存；一个店铺失败不影响其他，结果见同步日志
 */
@Component
@RequiredArgsConstructor
public class WxShopSyncJob implements JobHandler {

    private final WxShopSyncService syncService;

    @Override
    public String key() {
        return "channel.wxShopSync";
    }

    @Override
    public String name() {
        return "channel.label.wxShopSyncJob";
    }

    @Override
    public String defaultCron() {
        return "0 */5 * * * *";
    }

    @Override
    public String run(JobParams params) {
        return "同步 " + syncService.syncScheduled() + " 个店铺"; // i18n-ignore 存入执行日志
    }
}
