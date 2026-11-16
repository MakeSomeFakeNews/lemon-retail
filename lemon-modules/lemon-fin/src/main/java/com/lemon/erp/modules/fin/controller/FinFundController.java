package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.fin.dto.FundTransferRequest;
import com.lemon.erp.modules.fin.entity.FinFundTransfer;
import com.lemon.erp.modules.fin.service.FinFundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 资金管理：账户调拨
 */
@RestController
@RequestMapping("/fin")
@RequiredArgsConstructor
public class FinFundController {

    private final FinFundService fundService;

    // ---------------------------------------------------------------- 账户调拨

    @GetMapping("/fund-transfer")
    @PreAuthorize("@ss.hasPerm('fin:transfer:list')")
    public R<PageResult<FinFundTransfer>> pageTransfers(@RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                                        @RequestParam(defaultValue = "1") long current,
                                                        @RequestParam(defaultValue = "10") long size) {
        return R.ok(fundService.pageTransfers(current, size, keyword, begin, end));
    }

    @GetMapping("/fund-transfer/{id}")
    @PreAuthorize("@ss.hasPerm('fin:transfer:list')")
    public R<FinFundTransfer> getTransfer(@PathVariable Long id) {
        return R.ok(fundService.getTransfer(id));
    }

    @PostMapping("/fund-transfer")
    @PreAuthorize("@ss.hasPerm('fin:transfer:edit')")
    @OperLog(title = "账户调拨", type = OperType.INSERT) // i18n-ignore
    public R<Long> createTransfer(@Valid @RequestBody FundTransferRequest req) {
        return R.ok(fundService.createTransfer(req));
    }

    @DeleteMapping("/fund-transfer/{ids}")
    @PreAuthorize("@ss.hasPerm('fin:transfer:edit')")
    @OperLog(title = "账户调拨", type = OperType.DELETE) // i18n-ignore
    public R<Void> deleteTransfers(@PathVariable List<Long> ids) {
        fundService.deleteTransfers(ids);
        return R.ok();
    }
}
