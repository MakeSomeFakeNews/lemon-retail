package com.lemon.erp.framework.period;

import java.time.LocalDate;

/**
 * 业务单据的会计期间锁：业务日期所在期间已结账时，不能再新增、确认或删除影响账务的单据。
 * 由财务模块实现（{@code FinBizPeriodGuard}），仓库、供应链等上游模块只依赖本接口
 */
public interface BizPeriodGuard {

    /**
     * 期间已结账时报错：出入库、收付款、应收应付、费用等影响账务的单据
     *
     * @param docName 单据名称，用于提示，如「采购入库单」
     */
    void checkOpen(LocalDate bizDate, String docName);
}
