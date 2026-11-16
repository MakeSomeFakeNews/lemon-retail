package com.lemon.erp.modules.pos.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.dto.PosRequests;
import com.lemon.erp.modules.pos.dto.PosViews;
import com.lemon.erp.modules.pos.entity.PosPointsLog;
import com.lemon.erp.modules.pos.entity.PosStoredValueLog;
import com.lemon.erp.modules.pos.entity.PosTopup;
import com.lemon.erp.modules.pos.service.PosMemberRuleService;
import com.lemon.erp.modules.pos.service.PosPointsService;
import com.lemon.erp.modules.pos.service.PosStoredValueService;
import com.lemon.erp.modules.pos.service.PosTopupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 会员营销：积分与储值规则、积分流水与调整 / 清零、储值流水与调整、储值充值单（后台充值）、储值跨店结算报表。
 * 收银台充值见 {@link PosCashierController}
 */
@RestController
@RequestMapping("/pos")
@RequiredArgsConstructor
public class PosMarketingController {

    private final PosMemberRuleService ruleService;
    private final PosPointsService pointsService;
    private final PosStoredValueService storedValueService;
    private final PosTopupService topupService;

    // ---------------------------------------------------------------- 规则

    @GetMapping("/member-rule")
    @PreAuthorize("@ss.hasAnyPerm('pos:memberRule:list', 'pos:cashier:list', 'pos:topup:list')")
    public R<PosViews.MemberRule> rule() {
        return R.ok(new PosViews.MemberRule(ruleService.get(), ruleService.excludes(), ruleService.tiers()));
    }

    @PutMapping("/member-rule")
    @PreAuthorize("@ss.hasPerm('pos:memberRule:edit')")
    @OperLog(title = "积分储值规则", type = OperType.UPDATE) // i18n-ignore
    public R<Void> saveRule(@Valid @RequestBody PosRequests.MemberRuleSave req) {
        ruleService.save(req);
        return R.ok();
    }

    // ---------------------------------------------------------------- 积分

    @GetMapping("/points-log")
    @PreAuthorize("@ss.hasPerm('pos:points:list')")
    public R<PageResult<PosPointsLog>> pointsLog(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                                PosQueries.PointsLog query) {
        return R.ok(pointsService.page(current, size, query));
    }

    /** 积分调整（敏感操作） */
    @PostMapping("/points/adjust")
    @PreAuthorize("@ss.hasPerm('pos:member:points')")
    @OperLog(title = "积分调整", type = OperType.UPDATE) // i18n-ignore
    public R<BigDecimal> adjustPoints(@Valid @RequestBody PosRequests.PointsAdjust req) {
        return R.ok(pointsService.adjust(req));
    }

    /** 积分清零（敏感操作，前端二次确认） */
    @PostMapping("/points/clear")
    @PreAuthorize("@ss.hasPerm('pos:member:points')")
    @OperLog(title = "积分清零", type = OperType.UPDATE) // i18n-ignore
    public R<PosViews.PointsCleared> clearPoints(@Valid @RequestBody PosRequests.PointsClear req) {
        return R.ok(pointsService.clear(req));
    }

    // ---------------------------------------------------------------- 储值

    @GetMapping("/stored-value-log")
    @PreAuthorize("@ss.hasAnyPerm('pos:storedValue:list', 'pos:report:storedValue')")
    public R<PageResult<PosStoredValueLog>> storedValueLog(@RequestParam(defaultValue = "1") long current,
                                                           @RequestParam(defaultValue = "10") long size, PosQueries.StoredValueLog query) {
        return R.ok(storedValueService.page(current, size, query));
    }

    /** 储值调整（敏感操作） */
    @PostMapping("/stored-value/adjust")
    @PreAuthorize("@ss.hasPerm('pos:member:storedValue')")
    @OperLog(title = "储值调整", type = OperType.UPDATE) // i18n-ignore
    public R<BigDecimal> adjustStoredValue(@Valid @RequestBody PosRequests.StoredValueAdjust req) {
        return R.ok(storedValueService.adjust(req));
    }

    @GetMapping("/topup")
    @PreAuthorize("@ss.hasPerm('pos:topup:list')")
    public R<PageResult<PosTopup>> topups(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                          PosQueries.Topup query) {
        return R.ok(topupService.page(current, size, query));
    }

    @GetMapping("/topup/{id}")
    @PreAuthorize("@ss.hasAnyPerm('pos:topup:list', 'pos:cashier:list')")
    public R<PosTopup> topup(@PathVariable Long id) {
        return R.ok(topupService.detail(id));
    }

    /** 后台充值 */
    @PostMapping("/topup")
    @PreAuthorize("@ss.hasPerm('pos:topup:add')")
    @OperLog(title = "储值充值", type = OperType.INSERT) // i18n-ignore
    public R<PosTopup> adminTopup(@Valid @RequestBody PosRequests.AdminTopup req) {
        return R.ok(topupService.detail(topupService.topupAtAdmin(req)));
    }

    /** 储值跨店结算（按门店） */
    @GetMapping("/report/stored-value")
    @PreAuthorize("@ss.hasPerm('pos:report:storedValue')")
    public R<PosViews.StoredValueSettle> storedValueSettle(PosQueries.StoredValueSettle query) {
        return R.ok(storedValueService.settle(query));
    }
}
