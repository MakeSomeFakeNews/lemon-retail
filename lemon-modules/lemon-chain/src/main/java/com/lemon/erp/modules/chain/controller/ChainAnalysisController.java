package com.lemon.erp.modules.chain.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.chain.dto.ChainViews;
import com.lemon.erp.modules.chain.dto.FranchiseRequests;
import com.lemon.erp.modules.chain.entity.ChainAllocRule;
import com.lemon.erp.modules.chain.entity.ChainAllocRun;
import com.lemon.erp.modules.chain.service.AllocationService;
import com.lemon.erp.modules.chain.service.ReplenishService;
import com.lemon.erp.modules.chain.service.StoreDashboardService;
import com.lemon.erp.modules.chain.service.StorePnlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

/**
 * 经营分析与补货：门店损益、总部费用分摊、门店补货建议、店长看板
 */
@RestController
@RequestMapping("/chain")
@RequiredArgsConstructor
public class ChainAnalysisController {

    private final StorePnlService pnlService;
    private final AllocationService allocationService;
    private final ReplenishService replenishService;
    private final StoreDashboardService dashboardService;

    // ---------------------------------------------------------------- 门店损益

    /** 按日期范围：每个门店一行，最后一行为合计 */
    @GetMapping("/pnl")
    @PreAuthorize("@ss.hasPerm('chain:pnl:list')")
    public R<List<ChainViews.PnlRow>> pnl(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                          @RequestParam(required = false) Long regionId,
                                          @RequestParam(required = false) Long storeId) {
        return R.ok(pnlService.range(begin, end, regionId, storeId));
    }

    /** 门店 × 月份（beginMonth / endMonth 为 yyyy-MM） */
    @GetMapping("/pnl/grid")
    @PreAuthorize("@ss.hasPerm('chain:pnl:list')")
    public R<List<ChainViews.PnlRow>> grid(@RequestParam String beginMonth, @RequestParam String endMonth,
                                           @RequestParam(required = false) Long regionId,
                                           @RequestParam(required = false) Long storeId) {
        return R.ok(pnlService.grid(beginMonth, endMonth, regionId, storeId));
    }

    /** 穿透：门店的各月损益与费用类别 */
    @GetMapping("/pnl/detail")
    @PreAuthorize("@ss.hasPerm('chain:pnl:list')")
    public R<ChainViews.PnlDetail> pnlDetail(@RequestParam Long storeId,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return R.ok(pnlService.detail(storeId, begin, end));
    }

    // ---------------------------------------------------------------- 费用分摊

    @GetMapping("/alloc/rule")
    @PreAuthorize("@ss.hasPerm('chain:alloc:list')")
    public R<ChainAllocRule> allocRule() {
        return R.ok(allocationService.rule());
    }

    @PutMapping("/alloc/rule")
    @PreAuthorize("@ss.hasPerm('chain:alloc:edit')")
    @OperLog(title = "费用分摊设置", type = OperType.UPDATE) // i18n-ignore
    public R<Void> saveAllocRule(@Valid @RequestBody ChainAllocRule rule) {
        allocationService.saveRule(rule);
        return R.ok();
    }

    @GetMapping("/alloc/run")
    @PreAuthorize("@ss.hasPerm('chain:alloc:list')")
    public R<PageResult<ChainAllocRun>> allocRuns(@RequestParam(defaultValue = "1") long current,
                                                  @RequestParam(defaultValue = "10") long size) {
        return R.ok(allocationService.page(current, size));
    }

    @GetMapping("/alloc/run/{id}")
    @PreAuthorize("@ss.hasPerm('chain:alloc:list')")
    public R<ChainAllocRun> allocRun(@PathVariable Long id) {
        return R.ok(allocationService.detail(id));
    }

    /** 运行某月的分摊（替换该月已有的结果），返回分摊记录 ID */
    @PostMapping("/alloc/run")
    @PreAuthorize("@ss.hasPerm('chain:alloc:run')")
    @OperLog(title = "运行费用分摊", type = OperType.INSERT) // i18n-ignore
    public R<Long> runAlloc(@Valid @RequestBody FranchiseRequests.AllocRun req) {
        return R.ok(allocationService.run(req.period()));
    }

    @DeleteMapping("/alloc/run/{id}")
    @PreAuthorize("@ss.hasPerm('chain:alloc:run')")
    @OperLog(title = "运行费用分摊", type = OperType.DELETE) // i18n-ignore
    public R<Void> deleteAlloc(@PathVariable Long id) {
        allocationService.delete(id);
        return R.ok();
    }

    // ---------------------------------------------------------------- 补货建议

    @GetMapping("/replenish")
    @PreAuthorize("@ss.hasPerm('chain:replenish:list')")
    public R<List<ChainViews.ReplenishRow>> replenish(@RequestParam Long storeId,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(defaultValue = "false") boolean onlySuggest) {
        return R.ok(replenishService.suggest(storeId, keyword, onlySuggest));
    }

    @GetMapping("/replenish/params")
    @PreAuthorize("@ss.hasPerm('chain:replenish:list')")
    public R<ChainViews.ReplenishParams> replenishParams(@RequestParam Long storeId) {
        return R.ok(replenishService.params(storeId));
    }

    @PutMapping("/replenish/params/{storeId}")
    @PreAuthorize("@ss.hasPerm('chain:replenish:param')")
    @OperLog(title = "门店补货参数", type = OperType.UPDATE) // i18n-ignore
    public R<Void> saveReplenishParams(@PathVariable Long storeId, @Valid @RequestBody FranchiseRequests.ReplenishParam req) {
        replenishService.saveParams(storeId, req);
        return R.ok();
    }

    /** 一键生成要货单（草稿），返回要货单 ID */
    @PostMapping("/replenish/requisition")
    @PreAuthorize("@ss.hasPerm('chain:replenish:requisition')")
    @OperLog(title = "补货建议生成要货单", type = OperType.INSERT) // i18n-ignore
    public R<Long> toRequisition(@Valid @RequestBody FranchiseRequests.ToRequisition req) {
        return R.ok(replenishService.toRequisition(req));
    }

    // ---------------------------------------------------------------- 店长看板

    @GetMapping("/dashboard/store")
    @PreAuthorize("@ss.hasAnyPerm('pos:sale:list', 'chain:pnl:list')")
    public R<ChainViews.StoreDashboard> storeDashboard(@RequestParam Long storeId,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return R.ok(dashboardService.dashboard(storeId, date));
    }
}
