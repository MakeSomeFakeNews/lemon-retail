package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.period.BizPeriodGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

import static com.lemon.erp.modules.fin.model.FinConstants.periodOf;

/**
 * 业务单据的期间锁：已结账的期间不能再录入业务
 */
@Component
@RequiredArgsConstructor
public class FinBizPeriodGuard implements BizPeriodGuard {

    private final FinPeriodService periodService;

    /** docName 为调用方传入的单据名称的消息键（由调用方的模块登记），在此按请求语言取消息；不是键的文字原样显示 */
    @Override
    public void checkOpen(LocalDate bizDate, String docName) {
        if (bizDate == null) {
            return;
        }
        int period = periodOf(bizDate);
        if (periodService.isClosed(period)) {
            throw new BizException("fin.error.hasClosedBusinessDatePeriodNo", format(period), Messages.get(docName));
        }
    }

    private static String format(int period) {
        return Messages.get("fin.text.message", period / 100, period % 100);
    }
}
