package com.lemon.erp.modules.dist.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.dist.dto.DateLockRequest;
import com.lemon.erp.modules.dist.dto.DistQuery;
import com.lemon.erp.modules.dist.dto.DistViews;
import com.lemon.erp.modules.dist.dto.HandleRequest;
import com.lemon.erp.modules.dist.entity.DistDateLock;
import com.lemon.erp.modules.dist.entity.DistDiscrepancy;
import com.lemon.erp.modules.dist.entity.DistStoreLedger;
import com.lemon.erp.modules.dist.model.DeliveryKind;
import com.lemon.erp.modules.dist.model.DiscrepancyStatus;
import com.lemon.erp.modules.dist.service.DistDateLockService;
import com.lemon.erp.modules.dist.service.DistDiscrepancyService;
import com.lemon.erp.modules.dist.service.DistLedgerService;
import com.lemon.erp.modules.dist.service.DistReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 收货差异、在途查询、门店往来、门店对比、日期锁定
 */
@RestController
@RequestMapping("/dist")
@RequiredArgsConstructor
public class DistQueryController {

    private final DistDiscrepancyService discrepancyService;
    private final DistLedgerService ledgerService;
    private final DistReportService reportService;
    private final DistDateLockService dateLockService;

    // ---------------------------------------------------------------- 收货差异

    @GetMapping("/discrepancy")
    @PreAuthorize("@ss.hasPerm('dist:discrepancy:list')")
    public R<PageResult<DistDiscrepancy>> discrepancies(@RequestParam(defaultValue = "1") long current,
                                                        @RequestParam(defaultValue = "10") long size,
                                                        @RequestParam(required = false) DeliveryKind kind,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) List<DiscrepancyStatus> statuses,
                                                        @RequestParam(required = false) Long storeId,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                                        @RequestParam(required = false) Long materialId) {
        return R.ok(discrepancyService.page(kind, current, size, DistQuery.of(keyword,
                statuses == null ? null : statuses.stream().map(Enum::name).toList(), storeId, null, null, begin, end, materialId)));
    }

    @PostMapping("/discrepancy/{id}/handle")
    @PreAuthorize("@ss.hasPerm('dist:discrepancy:handle')")
    @OperLog(title = "收货差异-处理", type = OperType.UPDATE) // i18n-ignore
    public R<Void> handle(@PathVariable Long id, @Valid @RequestBody HandleRequest req) {
        discrepancyService.handle(id, req);
        return R.ok();
    }

    // ---------------------------------------------------------------- 在途

    @GetMapping("/transit")
    @PreAuthorize("@ss.hasPerm('dist:transit:list')")
    public R<PageResult<DistViews.TransitRow>> transit(@RequestParam(defaultValue = "1") long current,
                                                       @RequestParam(defaultValue = "10") long size,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Long storeId,
                                                       @RequestParam(required = false) Long toStoreId,
                                                       @RequestParam(required = false) Long warehouseId,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                                       @RequestParam(required = false) Long materialId) {
        return R.ok(ledgerService.transit(current, size, DistQuery.of(keyword, null, storeId, toStoreId, warehouseId, begin, end, materialId)));
    }

    /** 在途合计（单数、数量、调拨金额），条件同在途查询 */
    @GetMapping("/transit/total")
    @PreAuthorize("@ss.hasPerm('dist:transit:list')")
    public R<DistViews.TransitTotal> transitTotal(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Long storeId,
                                                  @RequestParam(required = false) Long toStoreId,
                                                  @RequestParam(required = false) Long warehouseId,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                                  @RequestParam(required = false) Long materialId) {
        return R.ok(ledgerService.transitTotal(DistQuery.of(keyword, null, storeId, toStoreId, warehouseId, begin, end, materialId)));
    }

    // ---------------------------------------------------------------- 门店往来

    @GetMapping("/ledger")
    @PreAuthorize("@ss.hasPerm('dist:ledger:list')")
    public R<PageResult<DistStoreLedger>> ledger(@RequestParam(defaultValue = "1") long current,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Long storeId,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return R.ok(ledgerService.page(current, size, DistQuery.of(keyword, null, storeId, null, null, begin, end, null)));
    }

    @GetMapping("/ledger/summary")
    @PreAuthorize("@ss.hasPerm('dist:ledger:list')")
    public R<List<DistViews.LedgerSummaryRow>> ledgerSummary(@RequestParam(required = false) Long storeId,
                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return R.ok(ledgerService.summary(DistQuery.of(null, null, storeId, null, null, begin, end, null)));
    }

    // ---------------------------------------------------------------- 门店对比

    @GetMapping("/report/store-compare")
    @PreAuthorize("@ss.hasPerm('dist:report:compare')")
    public R<List<DistViews.StoreCompareRow>> storeCompare(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                                           @RequestParam(required = false) Long regionId,
                                                           @RequestParam(required = false) Long storeId) {
        return R.ok(reportService.storeCompare(begin, end, regionId, storeId));
    }

    // ---------------------------------------------------------------- 日期锁定

    @GetMapping("/date-lock")
    @PreAuthorize("@ss.hasPerm('dist:dateLock:list')")
    public R<List<DistDateLock>> dateLocks() {
        return R.ok(dateLockService.list());
    }

    /** 设置或解除（lockDate 为空）锁定，storeId 为空为全公司 */
    @PutMapping("/date-lock")
    @PreAuthorize("@ss.hasPerm('dist:dateLock:edit')")
    @OperLog(title = "日期锁定", type = OperType.UPDATE) // i18n-ignore
    public R<Void> setDateLock(@Valid @RequestBody DateLockRequest req) {
        dateLockService.set(req);
        return R.ok();
    }
}
