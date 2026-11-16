package com.lemon.erp.modules.bi.model;

/**
 * 预警规则可以监控的指标（取值 SQL 见 BiInsightMapper.selectMetric）
 */
public enum BiAlertMetric {

    AR_OVERDUE("bi.label.overdueReceivablesAmount", "bi.label.cny"),
    STOCK_BELOW_SAFETY("bi.label.materialsBelowSafetyStock", "bi.label.items"),
    STOCK_ABOVE_MAX("bi.label.materialsAboveMaxStock", "bi.label.items"),
    SLOW_MOVING("bi.label.slowMovingMaterialsNoOutboundDays", "bi.label.items"),
    GROSS_MARGIN("bi.label.grossMarginMonth", "bi.label.percent");

    /** 名称、单位的消息键，输出时按请求语言取消息 */
    private final String label;
    private final String unit;

    BiAlertMetric(String label, String unit) {
        this.label = label;
        this.unit = unit;
    }

    public String getLabel() {
        return label;
    }

    public String getUnit() {
        return unit;
    }

    public static BiAlertMetric of(String name) {
        for (BiAlertMetric m : values()) {
            if (m.name().equals(name)) {
                return m;
            }
        }
        return null;
    }
}
