package com.lemon.erp.modules.chain.dto;

import com.lemon.erp.modules.chain.model.FranchiseDepositKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 连锁经营的请求参数
 */
public final class FranchiseRequests {

    private FranchiseRequests() {
    }

    /** 加盟商账户设置：信用额度（月结，为空不限） */
    public record Account(
            @DecimalMin(value = "0", message = "{chain.valid.creditLimitNonNegative}")
            @Digits(integer = 16, fraction = 2, message = "{chain.valid.amountScale}") BigDecimal creditLimit,
            @Size(max = 255, message = "{chain.valid.remarkTooLong}") String remark) {
    }

    /** 充值（预付货款）：登记预收款 */
    public record Topup(
            LocalDate date,
            @NotNull(message = "{chain.valid.accountRequired}") Long accountId,
            @Size(max = 16) String method,
            @NotNull(message = "{chain.valid.amountRequired}") @DecimalMin(value = "0.01", message = "{chain.valid.amountPositive}")
            @Digits(integer = 16, fraction = 2, message = "{chain.valid.amountScale}") BigDecimal amount,
            @Size(max = 255, message = "{chain.valid.remarkTooLong}") String remark) {
    }

    /** 生成对账单：加盟店 + 月份（yyyy-MM） */
    public record Statement(
            @NotNull(message = "{chain.valid.storeRequired}") Long storeId,
            @NotNull(message = "{chain.valid.periodRequired}") @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "{chain.valid.periodFormat}") String period,
            @Size(max = 255, message = "{chain.valid.remarkTooLong}") String remark) {
    }

    /** 保证金收退登记 */
    public record Deposit(
            @NotNull(message = "{chain.valid.storeRequired}") Long storeId,
            @NotNull(message = "{chain.valid.depositKindRequired}") FranchiseDepositKind kind,
            @NotNull(message = "{chain.valid.dateRequired}") LocalDate bizDate,
            @NotNull(message = "{chain.valid.amountRequired}") @DecimalMin(value = "0.01", message = "{chain.valid.amountPositive}")
            @Digits(integer = 16, fraction = 2, message = "{chain.valid.amountScale}") BigDecimal amount,
            Long accountId,
            @Size(max = 255, message = "{chain.valid.remarkTooLong}") String remark) {
    }

    /** 运行分摊：月份（yyyy-MM） */
    public record AllocRun(
            @NotNull(message = "{chain.valid.periodRequired}") @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "{chain.valid.periodFormat}") String period) {
    }

    /** 门店补货参数：为空取默认值 */
    public record ReplenishParam(
            @Min(value = 0, message = "{chain.valid.daysRange}") @Max(value = 365, message = "{chain.valid.daysRange}") Integer leadDays,
            @Min(value = 0, message = "{chain.valid.daysRange}") @Max(value = 365, message = "{chain.valid.daysRange}") Integer safetyDays) {
    }

    /** 补货建议一键生成要货单：门店仓库 + 物料与数量 */
    public record ToRequisition(
            @NotNull(message = "{chain.valid.warehouseRequired}") Long warehouseId,
            LocalDate expectedDate,
            @NotEmpty(message = "{chain.valid.itemsRequired}") @Size(max = 500, message = "{chain.valid.itemsTooMany}") List<@Valid @NotNull Line> items) {

        public record Line(
                @NotNull(message = "{chain.valid.materialRequired}") Long materialId,
                @NotNull(message = "{chain.valid.qtyRequired}") @DecimalMin(value = "0", inclusive = false, message = "{chain.valid.qtyPositive}")
                @Digits(integer = 14, fraction = 4, message = "{chain.valid.qtyScale}") BigDecimal qty,
                BigDecimal suggestQty) {
        }
    }
}
