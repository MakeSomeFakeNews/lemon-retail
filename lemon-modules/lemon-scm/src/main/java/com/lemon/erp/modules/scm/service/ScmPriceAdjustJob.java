package com.lemon.erp.modules.scm.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 调价单定时生效：到达生效时间的待生效调价单逐张生效，每张一个事务，一张失败不影响其他（下次执行时重试）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScmPriceAdjustJob implements JobHandler {

    private final ScmPriceAdjustService adjustService;
    private final TransactionTemplate transactionTemplate;

    @Override
    public String key() {
        return "scm.priceAdjust";
    }

    @Override
    public String name() {
        return "scm.label.priceAdjustJob";
    }

    @Override
    public String defaultCron() {
        return "0 */5 * * * *";
    }

    @Override
    public String run(JobParams params) {
        LocalDateTime now = LocalDateTime.now();
        List<Long> ids = adjustService.dueIds(now);
        int done = 0;
        int failed = 0;
        for (Long id : ids) {
            try {
                if (Boolean.TRUE.equals(transactionTemplate.execute(s -> adjustService.applyDue(id, now)))) {
                    done++;
                }
            } catch (RuntimeException e) {
                failed++;
                log.warn("调价单 {} 定时生效失败", id, e);
            }
        }
        return "生效 " + done + " 张调价单" + (failed > 0 ? "，失败 " + failed + " 张" : ""); // i18n-ignore 存入执行日志
    }
}
