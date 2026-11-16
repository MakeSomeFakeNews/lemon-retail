package com.lemon.erp.framework.security.datascope;

import java.util.Set;

/**
 * 登录用户的数据权限范围，由其全部启用角色的数据权限取并集得到，随 LoginUser 一起缓存。
 * 部门维度（角色的数据权限）与门店维度（用户的可管辖门店）同时生效：两者都满足的数据才可见
 *
 * @param allDepts   部门维度不受限（超级管理员，或任一角色为「全部数据」）
 * @param deptIds    可访问的部门 ID
 * @param selfUserId 可访问本人数据时为当前用户 ID，否则为 null
 * @param storeIds   可管辖的门店 ID，null 表示全部门店（不按门店过滤）
 */
public record DataScope(boolean allDepts, Set<Long> deptIds, Long selfUserId, Set<Long> storeIds) {

    public static final DataScope ALL = new DataScope(true, Set.of(), null, null);

    public DataScope {
        deptIds = deptIds == null ? Set.of() : Set.copyOf(deptIds);
        storeIds = storeIds == null ? null : Set.copyOf(storeIds);
    }

    /** 不限门店 */
    public DataScope(boolean allDepts, Set<Long> deptIds, Long selfUserId) {
        this(allDepts, deptIds, selfUserId, null);
    }

    /** 「全部数据」：部门、门店都不受限（汇总全公司的报表、驾驶舱等据此判断） */
    public boolean all() {
        return allDepts && storeIds == null;
    }

    /** 不按门店过滤 */
    public boolean allStores() {
        return storeIds == null;
    }

    public boolean canAccessDept(Long deptId) {
        return allDepts || (deptId != null && deptIds.contains(deptId));
    }

    /** 能否访问门店的数据；门店为空（不属于任何门店）的数据只有不限门店的用户可以访问 */
    public boolean canAccessStore(Long storeId) {
        return storeIds == null || (storeId != null && storeIds.contains(storeId));
    }

    /** 同样的部门范围，限定门店 */
    public DataScope withStores(Set<Long> stores) {
        return new DataScope(allDepts, deptIds, selfUserId, stores);
    }
}
