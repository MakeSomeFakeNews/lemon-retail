package com.lemon.erp.framework.store;

import com.lemon.erp.common.exception.BizException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 门店查询，由门店模块实现（{@code StoreLookupImpl}）。仓库、结算账户、用户、单据只保存门店 ID，
 * 通过本接口校验门店存在、可以开单，不直接访问门店表
 */
public interface StoreLookup {

    Optional<StoreBrief> find(Long storeId);

    Map<Long, StoreBrief> findAll(Collection<Long> storeIds);

    /** 当前租户的总部（每个租户有且只有一个，新租户初始化时创建） */
    StoreBrief headquarters();

    /** 区域及其全部下级区域中的门店 ID */
    Set<Long> storesInRegion(Long regionId);

    /** 当前租户的全部门店（含停业、闭店），按排序 */
    List<StoreBrief> listAll();

    /** 门店必须存在 */
    default StoreBrief require(Long storeId) {
        return find(storeId).orElseThrow(() -> new BizException("framework.error.storeNotFound"));
    }

    /** 门店必须存在且可以开单（不是停业、闭店） */
    default StoreBrief requireTrading(Long storeId) {
        StoreBrief store = require(storeId);
        if (!store.status().canTrade()) {
            throw new BizException("framework.error.storeNotTrading", store.name());
        }
        return store;
    }
}
