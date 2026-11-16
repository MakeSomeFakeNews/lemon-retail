package com.lemon.erp.modules.pos.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.dto.PosRequests;
import com.lemon.erp.modules.pos.entity.PosSpecial;
import com.lemon.erp.modules.pos.service.PosSpecialService;
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
 * 特价活动
 */
@RestController
@RequestMapping("/pos/special")
@RequiredArgsConstructor
public class PosSpecialController {

    private final PosSpecialService specialService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('pos:special:list')")
    public R<PageResult<PosSpecial>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                          PosQueries.Special query) {
        return R.ok(specialService.page(current, size, query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('pos:special:list')")
    public R<PosSpecial> get(@PathVariable Long id) {
        return R.ok(specialService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('pos:special:add')")
    @OperLog(title = "特价活动", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Valid @RequestBody PosRequests.Special req) {
        return R.ok(specialService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('pos:special:edit')")
    @OperLog(title = "特价活动", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PosRequests.Special req) {
        specialService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('pos:special:remove')")
    @OperLog(title = "特价活动", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        specialService.delete(ids);
        return R.ok();
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("@ss.hasPerm('pos:special:audit')")
    @OperLog(title = "特价活动-开始", type = OperType.UPDATE) // i18n-ignore
    public R<Void> activate(@PathVariable Long id) {
        specialService.activate(id);
        return R.ok();
    }

    @PostMapping("/{id}/end")
    @PreAuthorize("@ss.hasPerm('pos:special:audit')")
    @OperLog(title = "特价活动-结束", type = OperType.UPDATE) // i18n-ignore
    public R<Void> end(@PathVariable Long id) {
        specialService.end(id);
        return R.ok();
    }
}
