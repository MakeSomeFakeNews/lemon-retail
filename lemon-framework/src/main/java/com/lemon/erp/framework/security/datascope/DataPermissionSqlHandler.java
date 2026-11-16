package com.lemon.erp.framework.security.datascope;

import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.lemon.erp.framework.security.SecurityUtils;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 根据当前线程的 {@link DataPermissionRule} 和登录用户的 {@link DataScope} 生成 SQL 条件
 */
public class DataPermissionSqlHandler implements MultiDataPermissionHandler {

    /** 没有任何数据权限时的恒假条件 */
    private static final String DENY_ALL = "1 = 0";

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        DataPermissionRule rule = DataPermissionContext.current();
        if (rule == null || !rule.matches(table.getName())) {
            return null;
        }
        DataScope scope = SecurityUtils.getDataScope();
        if (scope.all()) {
            return null;
        }
        String condition = buildCondition(rule, scope, qualifier(table));
        if (condition == null) {
            return null;
        }
        try {
            return CCJSqlParserUtil.parseCondExpression(condition);
        } catch (JSQLParserException e) {
            throw new IllegalStateException("数据权限条件解析失败: " + condition, e); // i18n-ignore 内部错误
        }
    }

    /**
     * 部门维度（部门 OR 本人）与门店维度（门店 / 仓库所属门店）之间是 AND：规则声明了哪一维才按哪一维过滤，
     * 该维度不受限（全部部门 / 全部门店）时不加条件；拼接的只有开发者声明的列名和 Long 型 ID，不存在 SQL 注入
     *
     * @return 条件，不需要过滤时为 null
     */
    static String buildCondition(DataPermissionRule rule, DataScope scope, String qualifier) {
        List<String> parts = new ArrayList<>(2);
        if (rule.filtersDept() && !scope.allDepts()) {
            parts.add(deptCondition(rule, scope, qualifier));
        }
        if (rule.filtersStore() && !scope.allStores()) {
            parts.add(storeCondition(rule, scope, qualifier));
        }
        return parts.isEmpty() ? null : String.join(" AND ", parts);
    }

    private static String deptCondition(DataPermissionRule rule, DataScope scope, String qualifier) {
        List<String> conditions = new ArrayList<>(2);
        if (StringUtils.hasText(rule.deptColumn()) && !scope.deptIds().isEmpty()) {
            conditions.add(qualifier + rule.deptColumn() + " IN (" + ids(scope.deptIds()) + ")");
        }
        if (StringUtils.hasText(rule.userColumn()) && scope.selfUserId() != null) {
            conditions.add(qualifier + rule.userColumn() + " = " + scope.selfUserId());
        }
        return conditions.isEmpty() ? DENY_ALL : "(" + String.join(" OR ", conditions) + ")";
    }

    /** 仓库列按仓库所属门店过滤（仓库 ID 全局唯一，子查询不需要租户条件） */
    private static String storeCondition(DataPermissionRule rule, DataScope scope, String qualifier) {
        if (scope.storeIds().isEmpty()) {
            return DENY_ALL;
        }
        String stores = ids(scope.storeIds());
        if (StringUtils.hasText(rule.storeColumn())) {
            return qualifier + rule.storeColumn() + " IN (" + stores + ")";
        }
        // 多个仓库列（如调拨的调出、调入仓库）任一属于可管辖门店即可见
        String sub = " IN (SELECT dpw.id FROM wms_warehouse dpw WHERE dpw.store_id IN (" + stores + "))";
        String[] columns = rule.warehouseColumn().split(",");
        if (columns.length == 1) {
            return qualifier + columns[0].trim() + sub;
        }
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) sb.append(" OR ");
            sb.append(qualifier).append(columns[i].trim()).append(sub);
        }
        return sb.append(')').toString();
    }

    private static String ids(java.util.Set<Long> ids) {
        return ids.stream().sorted().map(String::valueOf).collect(Collectors.joining(", "));
    }

    private static String qualifier(Table table) {
        return (table.getAlias() != null ? table.getAlias().getName() : table.getName()) + ".";
    }
}
