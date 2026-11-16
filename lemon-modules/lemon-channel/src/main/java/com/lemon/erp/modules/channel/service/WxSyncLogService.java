package com.lemon.erp.modules.channel.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.modules.channel.dto.WxSyncResult;
import com.lemon.erp.modules.channel.entity.WxSyncLog;
import com.lemon.erp.modules.channel.mapper.WxSyncLogMapper;
import com.lemon.erp.modules.channel.model.WxSyncTrigger;
import com.lemon.erp.modules.channel.model.WxSyncType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 同步日志
 */
@Service
@RequiredArgsConstructor
public class WxSyncLogService {

    private static final int MESSAGE_MAX = 500;

    private final WxSyncLogMapper logMapper;

    /** 按用户的门店范围过滤（关联的店铺属于可管辖门店） */
    @DataPermission(table = "ch_wx_shop", deptColumn = "", userColumn = "", storeColumn = "store_id")
    public PageResult<WxSyncLog> page(long current, long size, Long shopId, WxSyncType type, Boolean success, LocalDate beginDate,
                                      LocalDate endDate) {
        Page<WxSyncLog> page = Page.of(current, size);
        logMapper.selectPageList(page, shopId, type, success, beginDate == null ? null : beginDate.atStartOfDay(),
                endDate == null ? null : endDate.plusDays(1).atStartOfDay());
        return PageResult.of(page);
    }

    /** 记录一次同步（在同步的事务之外单独写入） */
    void record(Long shopId, WxSyncType type, WxSyncTrigger trigger, WxSyncResult result, long startMillis) {
        WxSyncLog log = new WxSyncLog();
        log.setShopId(shopId);
        log.setSyncType(type);
        log.setSyncTrigger(trigger);
        log.setTotalCount(result.total());
        log.setSuccessCount(result.succeeded());
        log.setFailCount(result.failed());
        log.setSuccess(result.success() && result.failed() == 0);
        log.setMessage(truncate(result.message()));
        log.setDurationMs(System.currentTimeMillis() - startMillis);
        logMapper.insert(log);
    }

    static String truncate(String s) {
        return s == null || s.length() <= MESSAGE_MAX ? s : s.substring(0, MESSAGE_MAX);
    }
}
