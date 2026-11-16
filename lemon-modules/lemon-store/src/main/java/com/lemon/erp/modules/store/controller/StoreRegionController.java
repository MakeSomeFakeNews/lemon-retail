package com.lemon.erp.modules.store.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.common.validation.Create;
import com.lemon.erp.common.validation.Update;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.store.entity.StoreRegion;
import com.lemon.erp.modules.store.service.StoreRegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/store/region")
@RequiredArgsConstructor
public class StoreRegionController {

    private final StoreRegionService regionService;

    /** 登录即可访问：门店表单、报表筛选选择区域 */
    @GetMapping("/tree")
    public R<List<StoreRegion>> tree() {
        return R.ok(regionService.tree());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('store:region:list')")
    public R<StoreRegion> get(@PathVariable Long id) {
        return R.ok(regionService.get(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('store:region:add')")
    @OperLog(title = "区域管理", type = OperType.INSERT) // i18n-ignore
    public R<Void> create(@Validated(Create.class) @RequestBody StoreRegion region) {
        regionService.create(region);
        return R.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('store:region:edit')")
    @OperLog(title = "区域管理", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Validated(Update.class) @RequestBody StoreRegion region) {
        regionService.update(id, region);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('store:region:remove')")
    @OperLog(title = "区域管理", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable Long id) {
        regionService.delete(id);
        return R.ok();
    }
}
