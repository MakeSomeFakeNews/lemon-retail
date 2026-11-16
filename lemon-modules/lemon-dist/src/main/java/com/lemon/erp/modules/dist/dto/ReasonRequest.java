package com.lemon.erp.modules.dist.dto;

import jakarta.validation.constraints.Size;

/**
 * 驳回 / 关闭的原因
 */
public record ReasonRequest(@Size(max = 255, message = "{dist.valid.reasonTooLong}") String reason) {
}
