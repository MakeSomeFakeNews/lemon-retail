package com.lemon.erp.modules.dist.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.dist.dto.AuditRequest;
import com.lemon.erp.modules.dist.dto.DistQuery;
import com.lemon.erp.modules.dist.dto.DistViews;
import com.lemon.erp.modules.dist.dto.PurchaseRequest;
import com.lemon.erp.modules.dist.dto.ReasonRequest;
import com.lemon.erp.modules.dist.dto.RequisitionRequest;
import com.lemon.erp.modules.dist.entity.DistRequisition;
import com.lemon.erp.modules.dist.model.RequisitionStatus;
import com.lemon.erp.modules.dist.service.DistDeliveryService;
import com.lemon.erp.modules.dist.service.DistRequisitionService;
import com.lemon.erp.modules.dist.service.DistSummaryService;
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

import java.time.LocalDate;
import java.util.List;

/**
 * 门店要货单与要货审核 / 汇总。列表、详情：要货单页面（dist:requisition:list）或要货审核页面（dist:audit:list）
 */
@RestController
@RequestMapping("/dist/requisition")
@RequiredArgsConstructor
public class DistRequisitionController {

    private static final String VIEW = "@ss.hasAnyPerm('dist:requisition:list', 'dist:audit:list')";

    private final DistRequisitionService requisitionService;
    private final DistSummaryService summaryService;
    private final DistDeliveryService deliveryService;

    /**
     * @param statuses 状态（逗号分隔），为空不限
     * @param storeId  要货门店或供货门店
     */
    @GetMapping
    @PreAuthorize(VIEW)
    public R<PageResult<DistRequisition>> page(@RequestParam(defaultValue = "1") long current,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) List<RequisitionStatus> statuses,
                                               @RequestParam(required = false) Long storeId,
                                               @RequestParam(required = false) Long supplyWarehouseId,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                               @RequestParam(required = false) Long materialId) {
        return R.ok(requisitionService.page(current, size, DistQuery.of(keyword, statuses == null ? null : statuses.stream().map(Enum::name).toList(),
                storeId, null, supplyWarehouseId, begin, end, materialId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW)
    public R<DistRequisition> detail(@PathVariable Long id) {
        return R.ok(requisitionService.detail(id));
    }

    /** 要货建议（录入要货单时）：收货仓库的下限、现存、在途和建议量，供货仓库的可用数量 */
    @GetMapping("/suggest")
    @PreAuthorize("@ss.hasAnyPerm('dist:requisition:add', 'dist:requisition:edit')")
    public R<List<DistViews.SuggestRow>> suggest(@RequestParam Long warehouseId, @RequestParam(required = false) Long supplyWarehouseId,
                                                 @RequestParam List<Long> materialIds) {
        return R.ok(requisitionService.suggest(warehouseId, supplyWarehouseId, materialIds));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('dist:requisition:add')")
    @OperLog(title = "要货单", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Valid @RequestBody RequisitionRequest req) {
        return R.ok(requisitionService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('dist:requisition:edit')")
    @OperLog(title = "要货单", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody RequisitionRequest req) {
        requisitionService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('dist:requisition:remove')")
    @OperLog(title = "要货单", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        requisitionService.delete(ids);
        return R.ok();
    }

    /** 提交：发布了审批流程时进入审批，否则待审核 → 提交后的状态 */
    @PostMapping("/{id}/submit")
    @PreAuthorize("@ss.hasPerm('dist:requisition:submit')")
    @OperLog(title = "要货单-提交", type = OperType.UPDATE) // i18n-ignore
    public R<RequisitionStatus> submit(@PathVariable Long id) {
        return R.ok(requisitionService.submit(id));
    }

    /** 审核：可调整各行批准数量 */
    @PostMapping("/{id}/audit")
    @PreAuthorize("@ss.hasPerm('dist:requisition:audit')")
    @OperLog(title = "要货单-审核", type = OperType.UPDATE) // i18n-ignore
    public R<Void> audit(@PathVariable Long id, @Valid @RequestBody(required = false) AuditRequest req) {
        requisitionService.audit(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@ss.hasPerm('dist:requisition:audit')")
    @OperLog(title = "要货单-驳回", type = OperType.UPDATE) // i18n-ignore
    public R<Void> reject(@PathVariable Long id, @Valid @RequestBody(required = false) ReasonRequest req) {
        requisitionService.reject(id, req == null ? null : req.reason());
        return R.ok();
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("@ss.hasPerm('dist:requisition:close')")
    @OperLog(title = "要货单-关闭", type = OperType.UPDATE) // i18n-ignore
    public R<Void> close(@PathVariable Long id, @Valid @RequestBody(required = false) ReasonRequest req) {
        requisitionService.close(id, req == null ? null : req.reason());
        return R.ok();
    }

    // ---------------------------------------------------------------- 要货汇总

    /** 要货汇总：已审核未配送的要货按 供货仓库 + 物料，含可用库存、缺货和默认供应商 */
    @GetMapping("/summary")
    @PreAuthorize("@ss.hasPerm('dist:audit:list')")
    public R<List<DistViews.SummaryRow>> summary(@RequestParam(required = false) Long supplyWarehouseId,
                                                 @RequestParam(required = false) String keyword) {
        return R.ok(summaryService.summary(supplyWarehouseId, keyword));
    }

    /** 选中的要货单生成配送单（按 供货仓库 + 收货仓库 分组）→ 配送单 ID */
    @PostMapping("/to-delivery")
    @PreAuthorize("@ss.hasPerm('dist:delivery:add')")
    @OperLog(title = "要货单-生成配送单", type = OperType.INSERT) // i18n-ignore
    public R<List<Long>> toDelivery(@RequestBody ToDeliveryRequest req) {
        return R.ok(deliveryService.fromRequisitions(req.requisitionIds(), req.bizDate()));
    }

    /** 缺货生成采购订单（按供应商拆分）→ 采购订单 ID */
    @PostMapping("/to-purchase")
    @PreAuthorize("@ss.hasPerm('dist:summary:purchase')")
    @OperLog(title = "要货汇总-生成采购订单", type = OperType.INSERT) // i18n-ignore
    public R<List<Long>> toPurchase(@Valid @RequestBody PurchaseRequest req) {
        return R.ok(summaryService.toPurchaseOrders(req));
    }

    /**
     * @param bizDate 配送单业务日期，为空取当天
     */
    public record ToDeliveryRequest(List<Long> requisitionIds, LocalDate bizDate) {
    }
}
