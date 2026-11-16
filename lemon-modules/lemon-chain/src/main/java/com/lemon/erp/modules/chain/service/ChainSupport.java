package com.lemon.erp.modules.chain.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataScope;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 连锁经营的门店范围：列表、报表按用户可管辖的门店过滤（全部门店时不限）
 */
final class ChainSupport {

    private ChainSupport() {
    }

    /** 用户可管辖的门店，全部门店时为 null（不限）；未登录的系统操作不限 */
    static Set<Long> scopeStores() {
        DataScope scope = SecurityUtils.getDataScopeOrAll();
        return scope.allStores() ? null : new HashSet<>(scope.storeIds());
    }

    /** 与用户门店范围取交集：requested 为空表示不限 */
    static Set<Long> intersect(Collection<Long> requested) {
        Set<Long> scope = scopeStores();
        if (requested == null) {
            return scope;
        }
        Set<Long> result = new HashSet<>(requested);
        if (scope != null) {
            result.retainAll(scope);
        }
        return result;
    }

    static void checkStore(Long storeId) {
        if (!SecurityUtils.getDataScopeOrAll().canAccessStore(storeId)) {
            throw new BizException("framework.error.storeNotAccessible");
        }
    }
}
