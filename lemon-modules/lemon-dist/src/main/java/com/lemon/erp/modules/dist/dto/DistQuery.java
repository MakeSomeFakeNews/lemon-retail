package com.lemon.erp.modules.dist.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 列表查询条件（要货单、配送单、收货差异、门店往来、在途）
 *
 * @param keyword  单号 / 备注
 * @param statuses 状态（枚举名称），为空不限
 * @param storeId  门店：要货单为要货门店或供货门店，配送单为发货或收货门店
 * @param toStoreId 收货门店（门店收货页面）
 * @param begin    业务日期起
 * @param end      业务日期止
 * @param materialId 物料（含该物料的单据）
 */
public record DistQuery(String keyword, List<String> statuses, Long storeId, Long toStoreId, Long warehouseId,
                        LocalDate begin, LocalDate end, Long materialId) {

    public static DistQuery of(String keyword, List<String> statuses, Long storeId, Long toStoreId, Long warehouseId,
                               LocalDate begin, LocalDate end, Long materialId) {
        return new DistQuery(keyword == null || keyword.isBlank() ? null : keyword.trim(),
                statuses == null || statuses.isEmpty() ? null : statuses, storeId, toStoreId, warehouseId, begin, end, materialId);
    }
}
