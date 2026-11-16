package com.lemon.erp.modules.dist.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 设置日期锁定
 *
 * @param storeId  门店，为空或 0 为全公司
 * @param lockDate 锁定日期，为空表示解除锁定
 */
public record DateLockRequest(Long storeId, LocalDate lockDate,
                              @Size(max = 255, message = "{dist.valid.remarkTooLong}") String remark) {
}
