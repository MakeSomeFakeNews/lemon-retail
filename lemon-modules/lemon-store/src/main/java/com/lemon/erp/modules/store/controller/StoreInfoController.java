package com.lemon.erp.modules.store.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.common.validation.Create;
import com.lemon.erp.common.validation.Update;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.framework.store.StoreStatus;
import com.lemon.erp.framework.store.StoreType;
import com.lemon.erp.modules.store.dto.StoreOption;
import com.lemon.erp.modules.store.dto.StoreStatusRequest;
import com.lemon.erp.modules.store.entity.StoreInfo;
import com.lemon.erp.modules.store.service.StoreInfoService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/store/store")
@RequiredArgsConstructor
public class StoreInfoController {

    private final StoreInfoService storeService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('store:store:list')")
    public R<PageResult<StoreInfo>> page(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) StoreType type,
                                         @RequestParam(required = false) StoreStatus status,
                                         @RequestParam(required = false) Long regionId) {
        return R.ok(storeService.page(current, size, keyword, type, status, regionId));
    }

    /**
     * 登录即可访问：单据、报表、用户表单选择门店
     *
     * @param all     返回全部门店（默认只返回可管辖的门店）
     * @param trading 只返回可以开单的门店
     */
    @GetMapping("/options")
    public R<List<StoreOption>> options(@RequestParam(defaultValue = "false") boolean all,
                                        @RequestParam(defaultValue = "false") boolean trading) {
        return R.ok(storeService.options(all, trading));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('store:store:list')")
    public R<StoreInfo> get(@PathVariable Long id) {
        return R.ok(storeService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('store:store:add')")
    @OperLog(title = "门店管理", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Validated(Create.class) @RequestBody StoreInfo store) {
        return R.ok(storeService.create(store));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('store:store:edit')")
    @OperLog(title = "门店管理", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Validated(Update.class) @RequestBody StoreInfo store) {
        storeService.update(id, store);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPerm('store:store:edit')")
    @OperLog(title = "门店管理-修改状态", type = OperType.UPDATE) // i18n-ignore
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody StoreStatusRequest req) {
        storeService.changeStatus(id, req.status());
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('store:store:remove')")
    @OperLog(title = "门店管理", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        storeService.delete(ids);
        return R.ok();
    }
}
