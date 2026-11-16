package com.lemon.erp.modules.dist.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * 审核要货单：各行的批准数量（不传的行按要货数量批准，可为 0 表示不批准该行）
 */
public record AuditRequest(List<@Valid @NotNull Line> items) {

    public record Line(
            @NotNull(message = "{dist.valid.itemRequired}") Long itemId,
            @NotNull(message = "{dist.valid.qtyRequired}")
            @DecimalMin(value = "0", message = "{dist.valid.qtyNonNegative}")
            @Digits(integer = 14, fraction = 4, message = "{dist.valid.qtyScale}") BigDecimal approvedQty) {
    }
}
