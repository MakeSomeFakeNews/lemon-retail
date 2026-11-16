package com.lemon.erp.modules.scm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 新增 / 修改采购、销售订单（草稿）
 *
 * @param partnerId   供应商 / 客户
 * @param warehouseId 默认收货 / 发货仓库，选填
 * @param wholeDiscount 整单优惠（含税，抹零也用它）：在促销之后按行金额比例分摊到各行，优惠后合计 = 原合计 − 优惠
 * @param invoiceKind 发票类型（业务字典 scm_invoice_kind），选填：明细未填写税率时取它的税率，没有时取默认税率
 */
public record OrderRequest(
        @NotNull(message = "{scm.valid.selectBusinessPartner}") Long partnerId,
        @NotNull(message = "{scm.valid.selectOrderDate}") LocalDate orderDate,
        LocalDate deliveryDate,
        Long warehouseId,
        @Size(max = 255, message = "{scm.valid.remarkCannotExceedCharacters}") String remark,
        @NotEmpty(message = "{scm.valid.addLineItems}") @Size(max = 500, message = "{scm.valid.lineItemsCannotExceed}") List<@Valid @NotNull OrderItemRequest> items,
        @Size(max = 8) String currency,
        @jakarta.validation.constraints.DecimalMin(value = "0", inclusive = false, message = "{scm.valid.exchangeRateMustGreater}") java.math.BigDecimal exchangeRate,
        @jakarta.validation.constraints.DecimalMin(value = "0", message = "{scm.valid.wholeDiscountCannotNegative}")
        @jakarta.validation.constraints.Digits(integer = 16, fraction = 2, message = "{scm.valid.wholeDiscountDecimals}") java.math.BigDecimal wholeDiscount,
        @Size(max = 16) String invoiceKind) {

    /** 本位币订单 */
    public OrderRequest(Long partnerId, LocalDate orderDate, LocalDate deliveryDate, Long warehouseId, String remark,
                        List<OrderItemRequest> items) {
        this(partnerId, orderDate, deliveryDate, warehouseId, remark, items, null, null, null, null);
    }
}
