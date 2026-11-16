package com.lemon.erp.modules.pos.controller;

import com.lemon.erp.common.dto.StatusRequest;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.entity.PosMember;
import com.lemon.erp.modules.pos.entity.PosMemberLevel;
import com.lemon.erp.modules.pos.entity.PosPointsLog;
import com.lemon.erp.modules.pos.entity.PosSale;
import com.lemon.erp.modules.pos.entity.PosStoredValueLog;
import com.lemon.erp.modules.pos.service.PosMemberLevelService;
import com.lemon.erp.modules.pos.service.PosMemberService;
import com.lemon.erp.modules.pos.service.PosPointsService;
import com.lemon.erp.modules.pos.service.PosSaleService;
import com.lemon.erp.modules.pos.service.PosStoredValueService;
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

/**
 * 会员与会员等级
 */
@RestController
@RequestMapping("/pos")
@RequiredArgsConstructor
public class PosMemberController {

    private final PosMemberService memberService;
    private final PosMemberLevelService levelService;
    private final PosSaleService saleService;
    private final PosPointsService pointsService;
    private final PosStoredValueService storedValueService;

    @GetMapping("/member")
    @PreAuthorize("@ss.hasPerm('pos:member:list')")
    public R<PageResult<PosMember>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                         PosQueries.Member query) {
        return R.ok(memberService.page(current, size, query));
    }

    @GetMapping("/member/{id}")
    @PreAuthorize("@ss.hasPerm('pos:member:list')")
    public R<PosMember> get(@PathVariable Long id) {
        return R.ok(memberService.detail(id));
    }

    /** 会员的消费记录（POS 单据，按门店范围过滤） */
    @GetMapping("/member/{id}/sales")
    @PreAuthorize("@ss.hasPerm('pos:member:list')")
    public R<PageResult<PosSale>> sales(@PathVariable Long id, @RequestParam(defaultValue = "1") long current,
                                        @RequestParam(defaultValue = "10") long size) {
        memberService.get(id);
        return R.ok(saleService.page(current, size, new PosQueries.Sale(null, null, null, null, null, null, id, null, null, null, null)));
    }

    /** 会员的积分流水（全部门店） */
    @GetMapping("/member/{id}/points-log")
    @PreAuthorize("@ss.hasPerm('pos:member:list')")
    public R<PageResult<PosPointsLog>> pointsLog(@PathVariable Long id, @RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "10") long size) {
        memberService.get(id);
        return R.ok(pointsService.page(current, size, new PosQueries.PointsLog(null, id, null, null, null, null)));
    }

    /** 会员的储值流水（全部门店） */
    @GetMapping("/member/{id}/stored-value-log")
    @PreAuthorize("@ss.hasPerm('pos:member:list')")
    public R<PageResult<PosStoredValueLog>> storedValueLog(@PathVariable Long id, @RequestParam(defaultValue = "1") long current,
                                                           @RequestParam(defaultValue = "10") long size) {
        memberService.get(id);
        return R.ok(storedValueService.pageOfMember(current, size, id));
    }

    @PostMapping("/member")
    @PreAuthorize("@ss.hasPerm('pos:member:add')")
    @OperLog(title = "会员", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Valid @RequestBody PosMember member) {
        return R.ok(memberService.create(member));
    }

    @PutMapping("/member/{id}")
    @PreAuthorize("@ss.hasPerm('pos:member:edit')")
    @OperLog(title = "会员", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PosMember member) {
        memberService.update(id, member);
        return R.ok();
    }

    @PutMapping("/member/{id}/status")
    @PreAuthorize("@ss.hasPerm('pos:member:edit')")
    @OperLog(title = "会员-修改状态", type = OperType.UPDATE) // i18n-ignore
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        memberService.changeStatus(id, req.status());
        return R.ok();
    }

    @DeleteMapping("/member/{ids}")
    @PreAuthorize("@ss.hasPerm('pos:member:remove')")
    @OperLog(title = "会员", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        memberService.delete(ids);
        return R.ok();
    }

    // ---------------------------------------------------------------- 会员等级

    @GetMapping("/member-level")
    @PreAuthorize("@ss.hasAnyPerm('pos:memberLevel:list', 'pos:member:list', 'pos:cashier:list')")
    public R<List<PosMemberLevel>> levels() {
        return R.ok(levelService.list());
    }

    @PostMapping("/member-level")
    @PreAuthorize("@ss.hasPerm('pos:memberLevel:add')")
    @OperLog(title = "会员等级", type = OperType.INSERT) // i18n-ignore
    public R<Long> createLevel(@Valid @RequestBody PosMemberLevel level) {
        return R.ok(levelService.create(level));
    }

    @PutMapping("/member-level/{id}")
    @PreAuthorize("@ss.hasPerm('pos:memberLevel:edit')")
    @OperLog(title = "会员等级", type = OperType.UPDATE) // i18n-ignore
    public R<Void> updateLevel(@PathVariable Long id, @Valid @RequestBody PosMemberLevel level) {
        levelService.update(id, level);
        return R.ok();
    }

    @DeleteMapping("/member-level/{ids}")
    @PreAuthorize("@ss.hasPerm('pos:memberLevel:remove')")
    @OperLog(title = "会员等级", type = OperType.DELETE) // i18n-ignore
    public R<Void> deleteLevel(@PathVariable List<Long> ids) {
        levelService.delete(ids);
        return R.ok();
    }
}
