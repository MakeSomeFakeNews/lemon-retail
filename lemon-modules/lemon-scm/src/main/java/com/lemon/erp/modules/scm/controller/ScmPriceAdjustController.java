package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.scm.dto.PriceAdjustRequest;
import com.lemon.erp.modules.scm.entity.ScmPriceAdjust;
import com.lemon.erp.modules.scm.entity.ScmPriceHistory;
import com.lemon.erp.modules.scm.model.PriceAdjustStatus;
import com.lemon.erp.modules.scm.model.PriceAdjustTarget;
import com.lemon.erp.modules.scm.service.ScmPriceAdjustService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 调价单与调价历史
 */
@RestController
@RequestMapping("/scm/price-adjust")
@RequiredArgsConstructor
public class ScmPriceAdjustController {

    private final ScmPriceAdjustService adjustService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('scm:priceAdjust:list')")
    public R<PageResult<ScmPriceAdjust>> page(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) PriceAdjustStatus status,
                                              @RequestParam(required = false) PriceAdjustTarget target,
                                              @RequestParam(required = false) Long schemeId) {
        return R.ok(adjustService.page(current, size, keyword, status, target, schemeId));
    }

    /**
     * 调价历史（调整前 → 调整后），按生效时间倒序
     *
     * @param schemeId 只看某个价格方案；target 为 HQ 时只看总部价
     */
    @GetMapping("/history")
    @PreAuthorize("@ss.hasAnyPerm('scm:priceAdjust:list', 'scm:priceScheme:list')")
    public R<PageResult<ScmPriceHistory>> history(@RequestParam(defaultValue = "1") long current,
                                                  @RequestParam(defaultValue = "20") long size,
                                                  @RequestParam(required = false) Long materialId,
                                                  @RequestParam(required = false) Long schemeId,
                                                  @RequestParam(required = false) PriceAdjustTarget target,
                                                  @RequestParam(required = false) Long adjustId) {
        return R.ok(adjustService.history(current, size, materialId, schemeId, target, adjustId));
    }

    /** 当前价格：schemeId 为空取总部零售价，否则取方案价 → { 物料 ID: { retailPrice, memberPrice } } */
    @GetMapping("/current-prices")
    @PreAuthorize("@ss.hasAnyPerm('scm:priceAdjust:add', 'scm:priceAdjust:edit')")
    public R<Map<Long, Map<String, BigDecimal>>> currentPrices(@RequestParam(required = false) Long schemeId,
                                                               @RequestParam List<Long> materialIds) {
        return R.ok(adjustService.currentPrices(schemeId, materialIds));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:priceAdjust:list')")
    public R<ScmPriceAdjust> detail(@PathVariable Long id) {
        return R.ok(adjustService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('scm:priceAdjust:add')")
    @OperLog(title = "调价单", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Valid @RequestBody PriceAdjustRequest req) {
        return R.ok(adjustService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:priceAdjust:edit')")
    @OperLog(title = "调价单", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PriceAdjustRequest req) {
        adjustService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('scm:priceAdjust:remove')")
    @OperLog(title = "调价单", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        adjustService.delete(ids);
        return R.ok();
    }

    /** 提交：发布了调价单审批流程时发起审批，否则直接审核（生效时间为空或已到时立即生效）→ 提交后的状态 */
    @PostMapping("/{id}/submit")
    @PreAuthorize("@ss.hasPerm('scm:priceAdjust:audit')")
    @OperLog(title = "调价单-提交", type = OperType.UPDATE) // i18n-ignore
    public R<PriceAdjustStatus> submit(@PathVariable Long id) {
        return R.ok(adjustService.submit(id));
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("@ss.hasPerm('scm:priceAdjust:audit')")
    @OperLog(title = "调价单-撤回", type = OperType.UPDATE) // i18n-ignore
    public R<Void> withdraw(@PathVariable Long id) {
        adjustService.withdraw(id);
        return R.ok();
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("@ss.hasPerm('scm:priceAdjust:audit')")
    @OperLog(title = "调价单-作废", type = OperType.UPDATE) // i18n-ignore
    public R<Void> voidAdjust(@PathVariable Long id) {
        adjustService.voidAdjust(id);
        return R.ok();
    }
}
