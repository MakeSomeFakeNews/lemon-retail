package com.lemon.erp.modules.channel.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.channel.dto.WxOrderQuery;
import com.lemon.erp.modules.channel.dto.WxShopRequests;
import com.lemon.erp.modules.channel.entity.WxOrder;
import com.lemon.erp.modules.channel.service.WxOrderService;
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
@RequestMapping("/channel/wx-order")
@RequiredArgsConstructor
public class WxOrderController {

    private final WxOrderService orderService;

    /** 列表（不含收货信息），按门店的数据权限过滤 */
    @GetMapping
    @PreAuthorize("@ss.hasPerm('channel:wxOrder:list')")
    public R<PageResult<WxOrder>> page(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "10") long size,
                                       WxOrderQuery query) {
        return R.ok(orderService.page(current, size, query));
    }

    /** 详情：收货信息脱敏，有 channel:wxOrder:sensitive 权限时显示完整信息 */
    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('channel:wxOrder:list')")
    public R<WxOrder> get(@PathVariable Long id) {
        return R.ok(orderService.detail(id));
    }

    /** 生成销售订单 / 重新处理（待生成、待映射、处理失败） */
    @PostMapping("/{id}/retry")
    @PreAuthorize("@ss.hasPerm('channel:wxOrder:ship')")
    @OperLog(title = "微信小店-处理渠道订单", type = OperType.UPDATE) // i18n-ignore
    public R<Void> retry(@PathVariable Long id) {
        orderService.retry(id);
        return R.ok();
    }

    /** 从小店重新拉取 */
    @PostMapping("/{id}/refresh")
    @PreAuthorize("@ss.hasPerm('channel:wxOrder:ship')")
    public R<Void> refresh(@PathVariable Long id) {
        orderService.refresh(id);
        return R.ok();
    }

    /** 发货：生成销售出库，提交后回传快递公司和运单号 */
    @PostMapping("/{id}/ship")
    @PreAuthorize("@ss.hasPerm('channel:wxOrder:ship')")
    @OperLog(title = "微信小店-发货", type = OperType.UPDATE) // i18n-ignore
    public R<Void> ship(@PathVariable Long id, @Valid @RequestBody WxShopRequests.Ship req) {
        orderService.ship(id, req);
        return R.ok();
    }

    /** 重新回传发货信息 */
    @PostMapping("/{id}/push-delivery")
    @PreAuthorize("@ss.hasPerm('channel:wxOrder:ship')")
    @OperLog(title = "微信小店-回传发货", type = OperType.UPDATE) // i18n-ignore
    public R<Void> pushDelivery(@PathVariable Long id) {
        orderService.retryPush(id);
        return R.ok();
    }
}
