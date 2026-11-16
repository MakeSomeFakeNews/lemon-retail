package com.lemon.erp.modules.store.dto;

import com.lemon.erp.framework.store.StoreStatus;
import jakarta.validation.constraints.NotNull;

public record StoreStatusRequest(@NotNull(message = "{store.valid.statusRequired}") StoreStatus status) {
}
