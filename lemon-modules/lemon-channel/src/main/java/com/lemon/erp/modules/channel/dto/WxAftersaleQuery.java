package com.lemon.erp.modules.channel.dto;

import com.lemon.erp.modules.channel.model.WxAftersaleErpStatus;
import com.lemon.erp.modules.channel.model.WxAftersaleStatus;
import com.lemon.erp.modules.channel.model.WxAftersaleType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 售后单列表查询条件
 *
 * @param keyword   售后单号、订单号、商品标题，模糊匹配
 * @param beginDate 申请日期范围
 */
public record WxAftersaleQuery(Long shopId, Long storeId, WxAftersaleType type, WxAftersaleStatus status, WxAftersaleErpStatus erpStatus,
                               String keyword,
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

    /** 结束日期的次日 */
    public LocalDate getEndNext() {
        return endDate == null ? null : endDate.plusDays(1);
    }
}
