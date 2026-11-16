package com.lemon.erp.modules.pos.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.dto.PosRequests;
import com.lemon.erp.modules.pos.dto.PosViews;
import com.lemon.erp.modules.pos.entity.PosDailyClose;
import com.lemon.erp.modules.pos.service.PosDailyCloseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 门店日结
 */
@RestController
@RequestMapping("/pos/daily-close")
@RequiredArgsConstructor
public class PosDailyCloseController {

    private final PosDailyCloseService closeService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('pos:dailyClose:list')")
    public R<PageResult<PosDailyClose>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                             PosQueries.DailyClose query) {
        return R.ok(closeService.page(current, size, query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('pos:dailyClose:list')")
    public R<PosDailyClose> get(@PathVariable Long id) {
        return R.ok(closeService.detail(id));
    }

    /** 日结试算：门店某营业日期的汇总 */
    @GetMapping("/summary")
    @PreAuthorize("@ss.hasPerm('pos:dailyClose:list')")
    public R<PosViews.DailySummary> summary(@RequestParam Long storeId,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate) {
        return R.ok(closeService.summary(storeId, businessDate));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('pos:dailyClose:close')")
    @OperLog(title = "门店日结", type = OperType.INSERT) // i18n-ignore
    public R<Long> close(@Valid @RequestBody PosRequests.DailyClose req) {
        return R.ok(closeService.close(req));
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("@ss.hasPerm('pos:dailyClose:reopen')")
    @OperLog(title = "门店反日结", type = OperType.UPDATE) // i18n-ignore
    public R<Void> reopen(@PathVariable Long id) {
        closeService.reopen(id);
        return R.ok();
    }
}
