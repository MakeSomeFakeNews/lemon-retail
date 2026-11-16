package com.lemon.erp.modules.scm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.modules.scm.model.PriceAdjustTarget;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 新增 / 修改调价单
 *
 * @param target        调价对象：总部价 / 价格方案
 * @param schemeIds     价格方案（target 为 SCHEME 时必填，可多个）
 * @param effectiveTime 定时生效时间，为空审核后立即生效
 */
public record PriceAdjustRequest(
        @NotNull(message = "{scm.valid.priceAdjustTargetRequired}") PriceAdjustTarget target,
        List<Long> schemeIds,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime effectiveTime,
        @Size(max = 255, message = "{scm.valid.priceAdjustReasonTooLong}") String reason,
        @NotEmpty(message = "{scm.valid.priceAdjustItemsRequired}") @Size(max = 2000, message = "{scm.valid.priceAdjustItemsTooMany}")
        List<@Valid Line> items) {

    /**
     * @param retailPrice 新零售价，为空不调整
     * @param memberPrice 新会员价（只用于价格方案），为空不调整
     */
    public record Line(
            @NotNull(message = "{scm.valid.selectMaterial}") Long materialId,
            @DecimalMin(value = "0", message = "{scm.valid.unitPriceCannotLess}")
            @Digits(integer = 14, fraction = 4, message = "{scm.valid.unitPriceAllowsMostDecimalPlaces}") BigDecimal retailPrice,
            @DecimalMin(value = "0", message = "{scm.valid.unitPriceCannotLess}")
            @Digits(integer = 14, fraction = 4, message = "{scm.valid.unitPriceAllowsMostDecimalPlaces}") BigDecimal memberPrice,
            @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}") String remark) {
    }
}
