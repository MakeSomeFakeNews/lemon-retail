package com.lemon.erp.framework.security.datascope;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * 一张表的数据权限过滤规则
 *
 * @param table           表名
 * @param deptColumn      数据所属部门列，可为空
 * @param userColumn      数据所属用户列，可为空
 * @param storeColumn     数据所属门店列（`store_id`），可为空：用户限定了门店时只能看到这些门店的数据
 * @param warehouseColumn 数据所在仓库列，可为空：按仓库所属门店过滤（单据的门店就是其仓库的门店）；多列用逗号分隔，任一列满足即可
 */
public record DataPermissionRule(String table, String deptColumn, String userColumn, String storeColumn, String warehouseColumn) {

    public DataPermissionRule {
        Assert.hasText(table, "数据权限规则必须指定表名"); // i18n-ignore 内部错误
        table = table.toLowerCase(Locale.ROOT);
    }

    /** 只按部门、用户过滤 */
    public DataPermissionRule(String table, String deptColumn, String userColumn) {
        this(table, deptColumn, userColumn, null, null);
    }

    /** 按部门、用户和门店过滤（表上有 `store_id`） */
    public static DataPermissionRule withStore(String table, String deptColumn, String userColumn) {
        return new DataPermissionRule(table, deptColumn, userColumn, "store_id", null);
    }

    /** 按部门、用户和仓库所属门店过滤（单据） */
    public static DataPermissionRule withWarehouse(String table, String deptColumn, String userColumn, String warehouseColumn) {
        return new DataPermissionRule(table, deptColumn, userColumn, null, warehouseColumn);
    }

    /** 只按仓库所属门店过滤（库存类数据没有部门、制单人） */
    public static DataPermissionRule byWarehouse(String table, String warehouseColumn) {
        return new DataPermissionRule(table, null, null, null, warehouseColumn);
    }

    public static DataPermissionRule of(DataPermission annotation) {
        return new DataPermissionRule(annotation.table(), annotation.deptColumn(), annotation.userColumn(),
                annotation.storeColumn(), annotation.warehouseColumn());
    }

    /** 是否按部门 / 用户过滤 */
    public boolean filtersDept() {
        return StringUtils.hasText(deptColumn) || StringUtils.hasText(userColumn);
    }

    /** 是否按门店过滤 */
    public boolean filtersStore() {
        return StringUtils.hasText(storeColumn) || StringUtils.hasText(warehouseColumn);
    }

    /** 是否作用于该表（忽略大小写和反引号） */
    public boolean matches(String tableName) {
        return tableName != null && table.equals(tableName.replace("`", "").toLowerCase(Locale.ROOT));
    }
}
