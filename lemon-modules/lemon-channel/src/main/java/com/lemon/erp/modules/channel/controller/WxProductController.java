package com.lemon.erp.modules.channel.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.channel.dto.WxProductQuery;
import com.lemon.erp.modules.channel.dto.WxShopRequests;
import com.lemon.erp.modules.channel.entity.WxProduct;
import com.lemon.erp.modules.channel.service.WxProductService;
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

@RestController
@RequestMapping("/channel/wx-product")
@RequiredArgsConstructor
public class WxProductController {

    private final WxProductService productService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('channel:wxProduct:list')")
    public R<PageResult<WxProduct>> page(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size,
                                         WxProductQuery query) {
        return R.ok(productService.page(current, size, query));
    }

    /** 手工映射到物料 */
    @PutMapping("/{id}/mapping")
    @PreAuthorize("@ss.hasPerm('channel:wxProduct:edit')")
    @OperLog(title = "微信小店-商品映射", type = OperType.UPDATE) // i18n-ignore
    public R<Void> map(@PathVariable Long id, @Valid @RequestBody WxShopRequests.Mapping req) {
        productService.map(id, req);
        return R.ok();
    }

    /** 取消映射 */
    @DeleteMapping("/{id}/mapping")
    @PreAuthorize("@ss.hasPerm('channel:wxProduct:edit')")
    @OperLog(title = "微信小店-取消商品映射", type = OperType.UPDATE) // i18n-ignore
    public R<Void> unmap(@PathVariable Long id) {
        productService.unmap(id);
        return R.ok();
    }

    /** 自动匹配店铺未映射的 SKU，返回新匹配的数量 */
    @PostMapping("/auto-match")
    @PreAuthorize("@ss.hasPerm('channel:wxProduct:edit')")
    @OperLog(title = "微信小店-自动匹配商品", type = OperType.UPDATE) // i18n-ignore
    public R<Integer> autoMatch(@RequestParam Long shopId) {
        return R.ok(productService.autoMatch(shopId));
    }
}
