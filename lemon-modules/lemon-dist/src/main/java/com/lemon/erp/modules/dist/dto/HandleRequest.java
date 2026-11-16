package com.lemon.erp.modules.dist.dto;

import com.lemon.erp.modules.dist.model.DiscrepancyHandling;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 处理收货差异
 *
 * @param ioCategory 报损出库的出库类别（字典 wms_out_category），为空取「配送差异报损」
 */
public record HandleRequest(
        @NotNull(message = "{dist.valid.handlingRequired}") DiscrepancyHandling handling,
        String ioCategory,
        @Size(max = 255, message = "{dist.valid.remarkTooLong}") String remark) {
}
