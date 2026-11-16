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
 * 新增 / 修改配送单（发货仓库为配送中心的仓库）或退仓单（发货仓库为门店的仓库，收货仓库为空时取门店上级配送门店的默认仓库）
 */
public record DeliveryRequest(
        @NotNull(message = "{dist.valid.fromWarehouseRequired}") Long fromWarehouseId,
        Long toWarehouseId,
        @NotNull(message = "{dist.valid.bizDateRequired}") LocalDate bizDate,
        @Size(max = 255, message = "{dist.valid.remarkTooLong}") String remark,
        @NotEmpty(message = "{dist.valid.itemsRequired}") @Size(max = 500, message = "{dist.valid.itemsTooMany}") List<@Valid @NotNull Item> items) {

    /**
     * @param requisitionItemId 来源要货单明细（配送），可为空
     * @param price             配送价：为空按规则（加盟店取配送价方案，其他按调出成本）；需要「修改配送价」权限
     */
    public record Item(
            Long requisitionItemId,
            @NotNull(message = "{dist.valid.materialRequired}") Long materialId,
            Long locationId,
            @Size(max = 64, message = "{dist.valid.batchTooLong}") String batchNo,
            String serialNos,
            @NotNull(message = "{dist.valid.qtyRequired}")
            @DecimalMin(value = "0", inclusive = false, message = "{dist.valid.qtyPositive}")
            @Digits(integer = 14, fraction = 4, message = "{dist.valid.qtyScale}") BigDecimal qty,
            @DecimalMin(value = "0", message = "{dist.valid.priceNonNegative}")
            @Digits(integer = 12, fraction = 6, message = "{dist.valid.priceScale}") BigDecimal price,
            @Size(max = 255, message = "{dist.valid.remarkTooLong}") String remark) {
    }
}
