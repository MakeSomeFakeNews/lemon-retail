package com.lemon.erp.modules.dist.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 要货汇总的缺货生成采购订单（按供应商拆分）
 *
 * @param warehouseId 采购收货仓库（供货仓库）
 */
public record PurchaseRequest(
        @NotNull(message = "{dist.valid.warehouseRequired}") Long warehouseId,
        @NotEmpty(message = "{dist.valid.itemsRequired}") @Size(max = 500, message = "{dist.valid.itemsTooMany}") List<@Valid @NotNull Line> items) {

    /**
     * @param supplierId 供应商，为空取物料的默认供应商
     */
    public record Line(
            @NotNull(message = "{dist.valid.materialRequired}") Long materialId,
            @NotNull(message = "{dist.valid.qtyRequired}") @DecimalMin(value = "0", inclusive = false, message = "{dist.valid.qtyPositive}") BigDecimal qty,
            Long supplierId) {
    }
}
