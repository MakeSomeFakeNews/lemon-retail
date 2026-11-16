package com.lemon.erp.modules.pos.controller;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.pos.dto.PosRequests;
import com.lemon.erp.modules.pos.entity.PosTerminal;
import com.lemon.erp.modules.pos.model.PayMethod;
import com.lemon.erp.modules.pos.service.PosTerminalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 收银台管理、门店收款账户
 */
@RestController
@RequestMapping("/pos/terminal")
@RequiredArgsConstructor
public class PosTerminalController {

    private final PosTerminalService terminalService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('pos:terminal:list')")
    public R<PageResult<PosTerminal>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String keyword, @RequestParam(required = false) Long storeId,
                                           @RequestParam(required = false) EnableStatus status) {
        return R.ok(terminalService.page(current, size, keyword, storeId, status));
    }

    /** 可管辖门店中启用的收银台（收银台页面、报表筛选） */
    @GetMapping("/options")
    @PreAuthorize("@ss.hasAnyPerm('pos:cashier:list', 'pos:terminal:list', 'pos:sale:list', 'pos:shift:list', 'pos:report:cashier', 'pos:report:detail')")
    public R<List<PosTerminal>> options() {
        return R.ok(terminalService.options());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('pos:terminal:list')")
    public R<PosTerminal> get(@PathVariable Long id) {
        return R.ok(terminalService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('pos:terminal:add')")
    @OperLog(title = "收银台管理", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Valid @RequestBody PosTerminal terminal) {
        return R.ok(terminalService.create(terminal));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('pos:terminal:edit')")
    @OperLog(title = "收银台管理", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PosTerminal terminal) {
        terminalService.update(id, terminal);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('pos:terminal:remove')")
    @OperLog(title = "收银台管理", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        terminalService.delete(ids);
        return R.ok();
    }

    /** 门店收款账户：支付方式 → 结算账户 */
    @GetMapping("/pay-accounts")
    @PreAuthorize("@ss.hasPerm('pos:terminal:list')")
    public R<Map<PayMethod, Long>> payAccounts(@RequestParam Long storeId) {
        return R.ok(terminalService.payAccounts(storeId));
    }

    @PutMapping("/pay-accounts")
    @PreAuthorize("@ss.hasPerm('pos:terminal:edit')")
    @OperLog(title = "门店收款账户", type = OperType.UPDATE) // i18n-ignore
    public R<Void> savePayAccounts(@Valid @RequestBody PosRequests.PayAccounts req) {
        terminalService.savePayAccounts(req);
        return R.ok();
    }
}
