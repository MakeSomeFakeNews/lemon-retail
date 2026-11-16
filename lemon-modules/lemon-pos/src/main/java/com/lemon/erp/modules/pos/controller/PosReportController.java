package com.lemon.erp.modules.pos.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.dto.PosRequests;
import com.lemon.erp.modules.pos.dto.PosViews;
import com.lemon.erp.modules.pos.service.PosPayService;
import com.lemon.erp.modules.pos.service.PosReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 收银报表、支付通道设置
 */
@RestController
@RequestMapping("/pos")
@RequiredArgsConstructor
public class PosReportController {

    private final PosReportService reportService;
    private final PosPayService payService;

    /** 收银日报 / 收银员报表 */
    @GetMapping("/report/cashier")
    @PreAuthorize("@ss.hasPerm('pos:report:cashier')")
    public R<PosViews.CashierReport> cashier(PosQueries.CashierReport query) {
        return R.ok(reportService.cashier(query));
    }

    /** POS 销售明细（按行） */
    @GetMapping("/report/detail")
    @PreAuthorize("@ss.hasPerm('pos:report:detail')")
    public R<PageResult<PosViews.DetailRow>> detail(@RequestParam(defaultValue = "1") long current,
                                                    @RequestParam(defaultValue = "10") long size, PosQueries.Sale query) {
        return R.ok(reportService.detail(current, size, query));
    }

    @GetMapping("/pay-config")
    @PreAuthorize("@ss.hasPerm('pos:payConfig:list')")
    public R<PosViews.PayConfig> payConfig() {
        return R.ok(payService.configView());
    }

    @PutMapping("/pay-config")
    @PreAuthorize("@ss.hasPerm('pos:payConfig:edit')")
    @OperLog(title = "支付通道设置", type = OperType.UPDATE) // i18n-ignore
    public R<Void> savePayConfig(@Valid @RequestBody PosRequests.PayConfig req) {
        payService.saveConfig(req);
        return R.ok();
    }
}
