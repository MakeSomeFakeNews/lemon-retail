package com.lemon.erp.modules.dist.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.dist.dto.DeliveryRequest;
import com.lemon.erp.modules.dist.dto.DistQuery;
import com.lemon.erp.modules.dist.dto.ReasonRequest;
import com.lemon.erp.modules.dist.dto.ReceiveRequest;
import com.lemon.erp.modules.dist.entity.DistDelivery;
import com.lemon.erp.modules.dist.model.DeliveryKind;
import com.lemon.erp.modules.dist.model.DistDeliveryStatus;
import com.lemon.erp.modules.dist.service.DistDeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 配送单（/dist/delivery，权限 dist:delivery:*；门店收货页面 dist:receive:list / dist:receive:confirm）
 * 与门店退仓单（/dist/return，权限 dist:return:*）
 */
@RestController
@RequestMapping("/dist/{kind:delivery|return}")
@RequiredArgsConstructor
public class DistDeliveryController {

    private static final String VIEW = "@ss.hasAnyPerm('dist:' + #kind + ':list', #kind == 'delivery' ? 'dist:receive:list' : 'dist:return:list')";

    private final DistDeliveryService deliveryService;

    private static DeliveryKind kindOf(String path) {
        return "return".equals(path) ? DeliveryKind.RETURN : DeliveryKind.DELIVERY; // i18n-ignore 路径
    }

    /**
     * @param storeId   发货或收货门店
     * @param toStoreId 收货门店
     */
    @GetMapping
    @PreAuthorize(VIEW)
    public R<PageResult<DistDelivery>> page(@PathVariable String kind,
                                            @RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) List<DistDeliveryStatus> statuses,
                                            @RequestParam(required = false) Long storeId,
                                            @RequestParam(required = false) Long toStoreId,
                                            @RequestParam(required = false) Long warehouseId,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                            @RequestParam(required = false) Long materialId) {
        return R.ok(deliveryService.page(kindOf(kind), current, size, DistQuery.of(keyword,
                statuses == null ? null : statuses.stream().map(Enum::name).toList(), storeId, toStoreId, warehouseId, begin, end, materialId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW)
    public R<DistDelivery> detail(@PathVariable String kind, @PathVariable Long id) {
        return R.ok(deliveryService.detail(kindOf(kind), id));
    }

    /** 配送价预览：加盟店取配送价方案价（物料 ID → 价格），其他门店为空（发货时按调出成本） */
    @GetMapping("/prices")
    @PreAuthorize("@ss.hasAnyPerm('dist:' + #kind + ':add', 'dist:' + #kind + ':edit')")
    public R<Map<Long, BigDecimal>> prices(@PathVariable String kind, @RequestParam Long toWarehouseId, @RequestParam List<Long> materialIds) {
        return R.ok(deliveryService.prices(toWarehouseId, materialIds));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('dist:' + #kind + ':add')")
    @OperLog(title = "配送单", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@PathVariable String kind, @Valid @RequestBody DeliveryRequest req) {
        return R.ok(deliveryService.create(kindOf(kind), req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('dist:' + #kind + ':edit')")
    @OperLog(title = "配送单", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable String kind, @PathVariable Long id, @Valid @RequestBody DeliveryRequest req) {
        deliveryService.update(kindOf(kind), id, req);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('dist:' + #kind + ':remove')")
    @OperLog(title = "配送单", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable String kind, @PathVariable List<Long> ids) {
        deliveryService.delete(kindOf(kind), ids);
        return R.ok();
    }

    /** 退仓：门店提交申请 */
    @PostMapping("/{id}/submit")
    @PreAuthorize("#kind == 'return' and @ss.hasPerm('dist:return:edit')")
    @OperLog(title = "门店退仓-提交", type = OperType.UPDATE) // i18n-ignore
    public R<Void> submit(@PathVariable String kind, @PathVariable Long id) {
        deliveryService.submitReturn(id);
        return R.ok();
    }

    /** 退仓：总部审核 */
    @PostMapping("/{id}/approve")
    @PreAuthorize("#kind == 'return' and @ss.hasPerm('dist:return:audit')")
    @OperLog(title = "门店退仓-审核", type = OperType.UPDATE) // i18n-ignore
    public R<Void> approve(@PathVariable String kind, @PathVariable Long id) {
        deliveryService.approveReturn(id);
        return R.ok();
    }

    /** 退仓：总部驳回 */
    @PostMapping("/{id}/reject")
    @PreAuthorize("#kind == 'return' and @ss.hasPerm('dist:return:audit')")
    @OperLog(title = "门店退仓-驳回", type = OperType.UPDATE) // i18n-ignore
    public R<Void> reject(@PathVariable String kind, @PathVariable Long id, @Valid @RequestBody(required = false) ReasonRequest req) {
        deliveryService.rejectReturn(id, req == null ? null : req.reason());
        return R.ok();
    }

    /** 发货：生成在途调拨 */
    @PostMapping("/{id}/ship")
    @PreAuthorize("@ss.hasPerm('dist:' + #kind + ':ship')")
    @OperLog(title = "配送单-发货", type = OperType.UPDATE) // i18n-ignore
    public R<Void> ship(@PathVariable String kind, @PathVariable Long id) {
        deliveryService.ship(kindOf(kind), id);
        return R.ok();
    }

    /** 收货：按实收数量，差异生成收货差异 */
    @PostMapping("/{id}/receive")
    @PreAuthorize("@ss.hasPerm(#kind == 'delivery' ? 'dist:receive:confirm' : 'dist:return:receive')")
    @OperLog(title = "配送单-收货", type = OperType.UPDATE) // i18n-ignore
    public R<Void> receive(@PathVariable String kind, @PathVariable Long id, @Valid @RequestBody(required = false) ReceiveRequest req) {
        deliveryService.receive(kindOf(kind), id, req);
        return R.ok();
    }
}
