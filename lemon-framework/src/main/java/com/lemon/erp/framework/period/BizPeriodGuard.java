package com.lemon.erp.framework.period;

import java.time.LocalDate;

/**
 * 业务单据的期间锁：业务日期所在会计期间已结账，或业务日期不晚于日期锁定（{@link BizDateLocks}）时，不能再新增、确认或删除影响账务的单据。
 * 由财务模块实现（{@code FinBizPeriodGuard}，同时检查日期锁定），仓库、供应链等上游模块只依赖本接口
 */
public interface BizPeriodGuard {

    /**
     * 期间已结账或日期已锁定（全公司的锁定日期）时报错：出入库、收付款、应收应付、费用等影响账务的单据
     *
     * @param docName 单据名称，用于提示，如「采购入库单」
     */
    void checkOpen(LocalDate bizDate, String docName);

    /**
     * 同 {@link #checkOpen(LocalDate, String)}，并检查单据仓库所属门店的锁定日期
     *
     * @param warehouseId 单据的仓库（单据门店 = 仓库所属门店），可为空
     */
    default void checkOpen(LocalDate bizDate, String docName, Long warehouseId) {
        checkOpen(bizDate, docName);
    }
}
