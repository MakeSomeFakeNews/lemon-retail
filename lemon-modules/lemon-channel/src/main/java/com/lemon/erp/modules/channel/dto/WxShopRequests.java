package com.lemon.erp.modules.channel.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 线上渠道各接口的请求体
 */
public final class WxShopRequests {

    private WxShopRequests() {
    }

    /**
     * 手工映射商品
     *
     * @param unitRatio 1 件 SKU = 多少基本单位，为空取 1
     */
    public record Mapping(@NotNull(message = "{channel.valid.materialRequired}") Long materialId,
                          @DecimalMin(value = "0", inclusive = false, message = "{channel.valid.unitRatioPositive}")
                          @Digits(integer = 14, fraction = 4, message = "{channel.valid.unitRatioPositive}") BigDecimal unitRatio) {
    }

    /**
     * 在 ERP 中发货：生成销售出库后回传小店
     *
     * @param companyId   快递公司编码（小店的快递公司列表）
     * @param companyName 快递公司名称（显示用）
     */
    public record Ship(@NotBlank(message = "{channel.valid.deliveryCompanyRequired}") @Size(max = 32) String companyId,
                       @Size(max = 64) String companyName,
                       @NotBlank(message = "{channel.valid.waybillRequired}") @Size(max = 64, message = "{channel.valid.waybillTooLong}") String waybillNo) {
    }

    /** 拒绝售后 */
    public record Reject(@NotBlank(message = "{channel.valid.rejectReasonRequired}")
                         @Size(max = 200, message = "{channel.valid.rejectReasonTooLong}") String reason) {
    }

    /** 模拟店铺：生成订单数量 */
    public record MockOrders(@Min(value = 1, message = "{channel.valid.mockCountRange}")
                             @Max(value = 20, message = "{channel.valid.mockCountRange}") Integer count) {
    }
}
