package com.lemon.erp.modules.pos.model;

/**
 * 门店日结状态（字典 pos_daily_close_status）：已日结的门店当天不能再收银、退货、作废；反日结后可以再次日结
 */
public enum DailyCloseStatus {
    CLOSED,
    REOPENED
}
