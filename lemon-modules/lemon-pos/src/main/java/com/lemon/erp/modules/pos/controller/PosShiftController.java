package com.lemon.erp.modules.pos.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.dto.PosRequests;
import com.lemon.erp.modules.pos.dto.PosViews;
import com.lemon.erp.modules.pos.entity.PosShift;
import com.lemon.erp.modules.pos.service.PosShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 班次：开班、交班（收银台）与班次记录
 */
@RestController
@RequestMapping("/pos/shift")
@RequiredArgsConstructor
public class PosShiftController {

    private final PosShiftService shiftService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('pos:shift:list')")
    public R<PageResult<PosShift>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                        PosQueries.Shift query) {
        return R.ok(shiftService.page(current, size, query));
    }

    /** 收银台当班的班次，没有时 data 为空 */
    @GetMapping("/current")
    @PreAuthorize("@ss.hasPerm('pos:cashier:list')")
    public R<PosShift> current(@RequestParam Long terminalId) {
        return R.ok(shiftService.current(terminalId));
    }

    @PostMapping("/open")
    @PreAuthorize("@ss.hasPerm('pos:cashier:list')")
    @OperLog(title = "POS 开班", type = OperType.INSERT) // i18n-ignore
    public R<PosShift> open(@Valid @RequestBody PosRequests.ShiftOpen req) {
        return R.ok(shiftService.open(req));
    }

    /** 交班单：按支付方式汇总的系统金额、应有现金 */
    @GetMapping("/{id}/report")
    @PreAuthorize("@ss.hasAnyPerm('pos:cashier:list', 'pos:shift:list')")
    public R<PosViews.ShiftReport> report(@PathVariable Long id) {
        return R.ok(shiftService.report(id));
    }

    /** 交班：本人的班次，或有代交班权限 pos:shift:close（服务中校验） */
    @PostMapping("/{id}/close")
    @PreAuthorize("@ss.hasAnyPerm('pos:cashier:list', 'pos:shift:close')")
    @OperLog(title = "POS 交班", type = OperType.UPDATE) // i18n-ignore
    public R<PosViews.ShiftReport> close(@PathVariable Long id, @Valid @RequestBody PosRequests.ShiftClose req) {
        return R.ok(shiftService.close(id, req));
    }
}
