package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.scm.dto.ReorderSuggestion;
import com.lemon.erp.modules.scm.service.ScmReorderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 补货建议（再订货点）
 */
@RestController
@RequestMapping("/scm/reorder")
@RequiredArgsConstructor
public class ScmReorderController {

    private final ScmReorderService reorderService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('scm:reorder:list')")
    public R<List<ReorderSuggestion>> suggestions(@RequestParam(required = false) String keyword,
                                                  @RequestParam(defaultValue = "true") boolean onlyNeeded) {
        return R.ok(reorderService.suggestions(keyword, onlyNeeded));
    }

    /**
     * @param warehouseId 收货仓库，选填
     * @param supplierId  供应商，为空取默认供应商
     */
    public record ToOrdersRequest(Long warehouseId,
                                  @NotEmpty(message = "{scm.valid.selectMaterialsReplenish}") @Size(max = 500, message = "{scm.valid.lineItemsCannotExceed}") List<@Valid @NotNull Line> items) {

        public record Line(@NotNull(message = "{scm.valid.selectMaterial}") Long materialId,
                           @NotNull(message = "{scm.valid.enterQuantity}") @DecimalMin(value = "0", inclusive = false, message = "{scm.valid.quantityMustGreater}")
                           @Digits(integer = 14, fraction = 4, message = "{scm.valid.quantityAllowsMostDecimalPlaces}") BigDecimal qty,
                           Long supplierId,
                           @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}") String remark) {
        }
    }

    /** 选中的建议按供应商生成采购订单（草稿），返回采购订单 ID */
    @PostMapping("/orders")
    @PreAuthorize("@ss.hasPerm('scm:purchase:add')")
    @OperLog(title = "补货建议生成采购订单", type = OperType.INSERT) // i18n-ignore
    public R<List<Long>> toOrders(@Valid @RequestBody ToOrdersRequest req) {
        return R.ok(reorderService.toOrders(req.items().stream()
                .map(l -> new ScmReorderService.Item(l.materialId(), l.qty(), l.supplierId(), l.remark())).toList(), req.warehouseId()));
    }
}
