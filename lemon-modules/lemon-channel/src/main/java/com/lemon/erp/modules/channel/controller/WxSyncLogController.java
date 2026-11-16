package com.lemon.erp.modules.channel.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.channel.entity.WxSyncLog;
import com.lemon.erp.modules.channel.model.WxSyncType;
import com.lemon.erp.modules.channel.service.WxSyncLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/channel/wx-sync-log")
@RequiredArgsConstructor
public class WxSyncLogController {

    private final WxSyncLogService logService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('channel:wxSyncLog:list')")
    public R<PageResult<WxSyncLog>> page(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) Long shopId,
                                         @RequestParam(required = false) WxSyncType syncType,
                                         @RequestParam(required = false) Boolean success,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(logService.page(current, size, shopId, syncType, success, beginDate, endDate));
    }
}
