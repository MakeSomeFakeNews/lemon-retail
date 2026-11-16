package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.dto.StatusRequest;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.scm.entity.ScmPriceScheme;
import com.lemon.erp.modules.scm.service.ScmPriceSchemeService;
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
 * 门店价格方案
 */
@RestController
@RequestMapping("/scm/price-scheme")
@RequiredArgsConstructor
public class ScmPriceSchemeController {

    private final ScmPriceSchemeService schemeService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('scm:priceScheme:list')")
    public R<PageResult<ScmPriceScheme>> page(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) EnableStatus status) {
        return R.ok(schemeService.page(current, size, keyword, status));
    }

    /** 下拉：启用的方案 [{ id, code, name, status }]，ids 为编辑回显的方案（含停用的）；门店、调价单表单使用，登录即可 */
    @GetMapping("/options")
    public R<List<ScmPriceScheme>> options(@RequestParam(required = false) List<Long> ids) {
        return R.ok(schemeService.options(ids));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:priceScheme:list')")
    public R<ScmPriceScheme> detail(@PathVariable Long id) {
        return R.ok(schemeService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('scm:priceScheme:add')")
    @OperLog(title = "价格方案", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Valid @RequestBody ScmPriceScheme req) {
        return R.ok(schemeService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('scm:priceScheme:edit')")
    @OperLog(title = "价格方案", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ScmPriceScheme req) {
        schemeService.update(id, req);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPerm('scm:priceScheme:edit')")
    @OperLog(title = "价格方案-修改状态", type = OperType.UPDATE) // i18n-ignore
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        schemeService.changeStatus(id, req.status());
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('scm:priceScheme:remove')")
    @OperLog(title = "价格方案", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        schemeService.delete(ids);
        return R.ok();
    }
}
