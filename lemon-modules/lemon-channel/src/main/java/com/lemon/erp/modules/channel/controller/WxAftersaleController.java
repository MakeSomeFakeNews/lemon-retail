package com.lemon.erp.modules.channel.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.channel.dto.WxAftersaleQuery;
import com.lemon.erp.modules.channel.dto.WxShopRequests;
import com.lemon.erp.modules.channel.entity.WxAftersale;
import com.lemon.erp.modules.channel.service.WxAftersaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/channel/wx-aftersale")
@RequiredArgsConstructor
public class WxAftersaleController {

    private final WxAftersaleService aftersaleService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('channel:wxAftersale:list')")
    public R<PageResult<WxAftersale>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           WxAftersaleQuery query) {
        return R.ok(aftersaleService.page(current, size, query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('channel:wxAftersale:list')")
    public R<WxAftersale> get(@PathVariable Long id) {
        return R.ok(aftersaleService.detail(id));
    }

    /** 同意：仅退款即退款，退货退款为同意退货 */
    @PostMapping("/{id}/accept")
    @PreAuthorize("@ss.hasPerm('channel:wxAftersale:handle')")
    @OperLog(title = "微信小店-同意售后", type = OperType.UPDATE) // i18n-ignore
    public R<Void> accept(@PathVariable Long id) {
        aftersaleService.accept(id);
        return R.ok();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@ss.hasPerm('channel:wxAftersale:handle')")
    @OperLog(title = "微信小店-拒绝售后", type = OperType.UPDATE) // i18n-ignore
    public R<Void> reject(@PathVariable Long id, @Valid @RequestBody WxShopRequests.Reject req) {
        aftersaleService.reject(id, req.reason());
        return R.ok();
    }

    /** 退货退款：确认收到退货并退款，退款完成后生成销售退货 */
    @PostMapping("/{id}/confirm-receive")
    @PreAuthorize("@ss.hasPerm('channel:wxAftersale:handle')")
    @OperLog(title = "微信小店-确认收货退款", type = OperType.UPDATE) // i18n-ignore
    public R<Void> confirmReceive(@PathVariable Long id) {
        aftersaleService.confirmReceived(id);
        return R.ok();
    }

    @PostMapping("/{id}/refresh")
    @PreAuthorize("@ss.hasPerm('channel:wxAftersale:handle')")
    public R<Void> refresh(@PathVariable Long id) {
        aftersaleService.refresh(id);
        return R.ok();
    }

    /** 重新处理失败的售后（生成销售退货 / 扣减待发数量） */
    @PostMapping("/{id}/retry")
    @PreAuthorize("@ss.hasPerm('channel:wxAftersale:handle')")
    @OperLog(title = "微信小店-处理售后", type = OperType.UPDATE) // i18n-ignore
    public R<Void> retry(@PathVariable Long id) {
        aftersaleService.retry(id);
        return R.ok();
    }
}
