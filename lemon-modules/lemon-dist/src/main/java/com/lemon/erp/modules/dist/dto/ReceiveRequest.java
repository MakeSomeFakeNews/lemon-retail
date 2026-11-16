package com.lemon.erp.modules.dist.dto;

import com.lemon.erp.modules.dist.model.DiscrepancyKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 收货：各行实收数量（不传的行按发货数量收货）
 *
 * @param receiveDate 收货日期，为空取当天，不能早于发货日期
 */
public record ReceiveRequest(LocalDate receiveDate, List<@Valid @NotNull Line> items) {

    /**
     * @param kind   实收少于发货时的差异种类：少收（默认）或破损；多于发货时为多收
     * @param reason 差异说明
     */
    public record Line(
            @NotNull(message = "{dist.valid.itemRequired}") Long itemId,
            @NotNull(message = "{dist.valid.qtyRequired}")
            @DecimalMin(value = "0", message = "{dist.valid.qtyNonNegative}")
            @Digits(integer = 14, fraction = 4, message = "{dist.valid.qtyScale}") BigDecimal receivedQty,
            DiscrepancyKind kind,
            @Size(max = 255, message = "{dist.valid.reasonTooLong}") String reason) {
    }
}
