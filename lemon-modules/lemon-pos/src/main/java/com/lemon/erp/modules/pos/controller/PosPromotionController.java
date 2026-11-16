package com.lemon.erp.modules.pos.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.dto.PosRequests;
import com.lemon.erp.modules.pos.entity.PosPromotion;
import com.lemon.erp.modules.pos.service.PosPromotionService;
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
 * 满减 / 满折 / 满赠活动
 */
@RestController
@RequestMapping("/pos/promotion")
@RequiredArgsConstructor
public class PosPromotionController {

    private final PosPromotionService promotionService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('pos:promotion:list')")
    public R<PageResult<PosPromotion>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                            PosQueries.Promotion query) {
        return R.ok(promotionService.page(current, size, query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('pos:promotion:list')")
    public R<PosPromotion> get(@PathVariable Long id) {
        return R.ok(promotionService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('pos:promotion:add')")
    @OperLog(title = "满减满赠", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Valid @RequestBody PosRequests.Promotion req) {
        return R.ok(promotionService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('pos:promotion:edit')")
    @OperLog(title = "满减满赠", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PosRequests.Promotion req) {
        promotionService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('pos:promotion:remove')")
    @OperLog(title = "满减满赠", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        promotionService.delete(ids);
        return R.ok();
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("@ss.hasPerm('pos:promotion:audit')")
    @OperLog(title = "满减满赠-开始", type = OperType.UPDATE) // i18n-ignore
    public R<Void> activate(@PathVariable Long id) {
        promotionService.activate(id);
        return R.ok();
    }

    @PostMapping("/{id}/end")
    @PreAuthorize("@ss.hasPerm('pos:promotion:audit')")
    @OperLog(title = "满减满赠-结束", type = OperType.UPDATE) // i18n-ignore
    public R<Void> end(@PathVariable Long id) {
        promotionService.end(id);
        return R.ok();
    }
}
