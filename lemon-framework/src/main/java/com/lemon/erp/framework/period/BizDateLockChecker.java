package com.lemon.erp.framework.period;

import java.time.LocalDate;

/**
 * 业务日期锁定（结账日 / 门店日结）的检查，由配送模块实现（{@code DistDateLockService}：全公司锁定日期 + 门店锁定日期）。
 * 业务代码不直接调用，经 {@link BizDateLocks} 或 {@link BizPeriodGuard} 使用
 */
public interface BizDateLockChecker {

    /**
     * 业务日期不晚于锁定日期时抛出 BizException
     *
     * @param warehouseId 单据仓库（按其所属门店取门店的锁定日期），为空只检查全公司的锁定日期
     * @param docName     单据名称（消息键或文字），用于提示
     */
    void check(LocalDate bizDate, Long warehouseId, String docName);
}
