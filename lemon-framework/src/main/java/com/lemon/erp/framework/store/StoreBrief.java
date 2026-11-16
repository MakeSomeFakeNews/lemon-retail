package com.lemon.erp.framework.store;

/**
 * 门店的简要信息，供其他模块校验、显示
 *
 * @param supplyStoreId  上级配送门店（要货默认由它供货），总部、配送中心可为空
 * @param priceSchemeId  门店使用的零售价方案，为空按总部价格
 * @param regionId       所属区域，可为空
 */
public record StoreBrief(Long id, String code, String name, StoreType type, StoreStatus status, Long regionId,
                         Long supplyStoreId, Long priceSchemeId, Long defaultWarehouseId, Long defaultAccountId) {
}
