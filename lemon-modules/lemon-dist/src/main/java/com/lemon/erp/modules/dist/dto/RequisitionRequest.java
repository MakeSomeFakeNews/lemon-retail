package com.lemon.erp.modules.dist.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 新增 / 修改要货单
 *
 * @param warehouseId       收货仓库（要货门店的仓库，门店由仓库决定）
 * @param supplyWarehouseId 供货仓库，为空取要货门店的上级配送门店的默认仓库
 * @param expectedDate      期望到货日期，可为空
 */
public record RequisitionRequest(
        @NotNull(message = "{dist.valid.warehouseRequired}") Long warehouseId,
        Long supplyWarehouseId,
        @NotNull(message = "{dist.valid.bizDateRequired}") LocalDate bizDate,
        LocalDate expectedDate,
        @Size(max = 255, message = "{dist.valid.remarkTooLong}") String remark,
        @NotEmpty(message = "{dist.valid.itemsRequired}") @Size(max = 500, message = "{dist.valid.itemsTooMany}") List<@Valid @NotNull Item> items) {

    /**
     * @param suggestQty 建议量（录入时参考，可为空）
     */
    public record Item(
            @NotNull(message = "{dist.valid.materialRequired}") Long materialId,
            @NotNull(message = "{dist.valid.qtyRequired}")
            @DecimalMin(value = "0", inclusive = false, message = "{dist.valid.qtyPositive}")
            @Digits(integer = 14, fraction = 4, message = "{dist.valid.qtyScale}") BigDecimal qty,
            BigDecimal suggestQty,
            @Size(max = 255, message = "{dist.valid.remarkTooLong}") String remark) {
    }
}
