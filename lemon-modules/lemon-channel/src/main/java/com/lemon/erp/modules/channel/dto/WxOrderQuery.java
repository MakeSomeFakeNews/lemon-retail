package com.lemon.erp.modules.channel.dto;

import com.lemon.erp.modules.channel.model.WxDeliveryStatus;
import com.lemon.erp.modules.channel.model.WxErpStatus;
import com.lemon.erp.modules.channel.model.WxOrderStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 渠道订单列表查询条件
 *
 * @param keyword   小店订单号、销售订单号、商品标题，模糊匹配
 * @param beginDate 下单日期范围
 */
public record WxOrderQuery(Long shopId, Long storeId, WxOrderStatus status, WxErpStatus erpStatus, WxDeliveryStatus deliveryStatus,
                           String keyword,
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

    /** 结束日期的次日（mapper XML 中按「小于次日」过滤） */
    public LocalDate getEndNext() {
        return endDate == null ? null : endDate.plusDays(1);
    }
}
