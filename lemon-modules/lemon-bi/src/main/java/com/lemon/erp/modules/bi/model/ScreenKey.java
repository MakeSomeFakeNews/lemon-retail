package com.lemon.erp.modules.bi.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 数据大屏
 */
@Getter
@RequiredArgsConstructor
public enum ScreenKey {
    WAREHOUSE("bi.label.warehouse"),
    SALES("bi.label.sales");

    /** 名称的消息键 */
    private final String label;
}
