package com.lemon.erp.modules.pos.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.entity.PosSale;
import com.lemon.erp.modules.pos.service.PosSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * POS 单据查询（销售单、退货单）；作废、退货见收银台接口
 */
@RestController
@RequestMapping("/pos/sale")
@RequiredArgsConstructor
public class PosSaleController {

    private final PosSaleService saleService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('pos:sale:list')")
    public R<PageResult<PosSale>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                       PosQueries.Sale query) {
        return R.ok(saleService.page(current, size, query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasAnyPerm('pos:sale:list', 'pos:cashier:list', 'pos:member:list')")
    public R<PosSale> get(@PathVariable Long id) {
        return R.ok(saleService.detail(id));
    }
}
