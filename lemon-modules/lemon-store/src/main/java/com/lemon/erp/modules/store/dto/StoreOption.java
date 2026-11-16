package com.lemon.erp.modules.store.dto;

import com.lemon.erp.framework.store.StoreStatus;
import com.lemon.erp.framework.store.StoreType;
import com.lemon.erp.modules.store.entity.StoreInfo;

/**
 * 门店下拉选项：开单页面选择门店后带出默认仓库、账户
 */
public record StoreOption(Long id, String code, String name, String shortName, StoreType type, StoreStatus status,
                          Long regionId, Long defaultWarehouseId, Long defaultAccountId) {

    public static StoreOption of(StoreInfo s) {
        return new StoreOption(s.getId(), s.getCode(), s.getName(), s.getShortName(), s.getType(), s.getStatus(),
                s.getRegionId(), s.getDefaultWarehouseId(), s.getDefaultAccountId());
    }
}
