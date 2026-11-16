package com.lemon.erp.framework.period;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 业务日期锁定（§14 规则 7）：锁定日期及以前的单据不能新增、修改、确认、作废。
 * 确认、作废时经 {@link BizPeriodGuard#checkOpen}（与会计期间结账一起检查）；只保存草稿、不影响账务的操作（新增、修改、删除草稿）
 * 直接调用本类。没有实现（未部署配送模块）或没有设置锁定日期时不限制
 */
@Component
@RequiredArgsConstructor
public class BizDateLocks {

    private final List<BizDateLockChecker> checkers;

    /**
     * @param warehouseId 单据仓库，为空只检查全公司的锁定日期
     * @param docName     单据名称（消息键或文字）
     */
    public void check(LocalDate bizDate, Long warehouseId, String docName) {
        if (bizDate == null) {
            return;
        }
        checkers.forEach(c -> c.check(bizDate, warehouseId, docName));
    }
}
