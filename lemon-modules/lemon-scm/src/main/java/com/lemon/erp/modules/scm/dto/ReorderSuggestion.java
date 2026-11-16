package com.lemon.erp.modules.scm.dto;

import com.lemon.erp.modules.wms.model.StockAlertMode;

import java.math.BigDecimal;

/**
 * 补货建议：预计库存 = 可用 + 采购在途 − 销售待发，不高于再订货点时建议补货
 *
 * @param suggestQty   建议数量 = max(再订货量, 再订货点 + 安全库存 − 预计库存)，按默认供应商的最小起订量取整；不需要补货时为 0
 * @param safetyStock  安全库存：分仓库预警的物料为各仓库下限之和（补货按全公司计算）
 * @param alertMode    库存预警方式
 * @param lowWarehouses 分仓库预警时库存低于下限的仓库数：全公司不需要补货时，可在仓库之间调拨补足
 */
public record ReorderSuggestion(Long materialId, String code, String name, String spec, String unit,
                                BigDecimal safetyStock, StockAlertMode alertMode, int lowWarehouses, BigDecimal reorderPoint, BigDecimal reorderQty,
                                BigDecimal available, BigDecimal poOpen, BigDecimal soOpen, BigDecimal projected,
                                BigDecimal suggestQty, Long supplierId, String supplierName, BigDecimal price) {
}
