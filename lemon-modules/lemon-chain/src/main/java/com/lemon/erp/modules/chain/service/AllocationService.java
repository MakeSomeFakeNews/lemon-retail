package com.lemon.erp.modules.chain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.chain.entity.ChainAllocRecord;
import com.lemon.erp.modules.chain.entity.ChainAllocRule;
import com.lemon.erp.modules.chain.entity.ChainAllocRun;
import com.lemon.erp.modules.chain.mapper.ChainAllocRecordMapper;
import com.lemon.erp.modules.chain.mapper.ChainAllocRuleMapper;
import com.lemon.erp.modules.chain.mapper.ChainAllocRunMapper;
import com.lemon.erp.modules.chain.model.AllocBasis;
import com.lemon.erp.modules.store.entity.StoreInfo;
import com.lemon.erp.modules.store.service.StoreInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 总部费用分摊（需求 §14 规则 4）：按月运行，待分摊的总部费用 = 当月已确认的费用支出单中结算账户属于总部或各门店共用的部分，
 * 按设置的依据分摊到参与的门店（直营店，可选配送中心、加盟店，可限定区域；只含筹备、营业的门店）：
 * 按销售额（当月零售 + 批发净额）/ 按营业面积 / 平均；金额按比例舍入到分，尾差计入最后一个门店。
 * 同一个月重新运行时替换上次的结果。门店损益中门店扣减分摊额，总部加回已分摊的部分
 */
@Service
@RequiredArgsConstructor
public class AllocationService {

    private final ChainAllocRuleMapper ruleMapper;
    private final ChainAllocRunMapper runMapper;
    private final ChainAllocRecordMapper recordMapper;
    private final StorePnlService pnlService;
    private final StoreInfoService storeService;

    /** 分摊设置，没有保存过时为默认值（按销售额，只分摊到直营店） */
    public ChainAllocRule rule() {
        ChainAllocRule rule = ruleMapper.selectOne(Wrappers.emptyWrapper());
        if (rule == null) {
            rule = new ChainAllocRule();
            rule.setBasis(AllocBasis.SALES);
            rule.setIncludeDc(false);
            rule.setIncludeFranchise(false);
        }
        return rule;
    }

    @Transactional
    public void saveRule(ChainAllocRule req) {
        ChainAllocRule rule = ruleMapper.selectOne(Wrappers.emptyWrapper());
        req.setIncludeDc(Boolean.TRUE.equals(req.getIncludeDc()));
        req.setIncludeFranchise(Boolean.TRUE.equals(req.getIncludeFranchise()));
        if (rule == null) {
            req.setId(null);
            ruleMapper.insert(req);
        } else {
            req.setId(rule.getId());
            ruleMapper.updateById(req);
        }
    }

    public PageResult<ChainAllocRun> page(long current, long size) {
        Page<ChainAllocRun> page = runMapper.selectPage(Page.of(current, size), Wrappers.<ChainAllocRun>lambdaQuery()
                .orderByDesc(ChainAllocRun::getPeriod));
        return PageResult.of(page);
    }

    public ChainAllocRun detail(Long id) {
        ChainAllocRun run = runMapper.selectById(id);
        if (run == null) {
            throw new BizException("chain.error.allocRunNotFound");
        }
        run.setRecords(recordMapper.selectByRun(id));
        return run;
    }

    /**
     * 运行某月的分摊（替换该月已有的结果）。需要全部门店的门店范围
     *
     * @param period yyyy-MM
     * @return 分摊记录 ID
     */
    @Transactional
    public Long run(String period) {
        if (!SecurityUtils.getDataScope().allStores()) {
            throw new BizException("chain.error.allocNeedsAllStores");
        }
        YearMonth month = YearMonth.parse(period);
        ChainAllocRule rule = rule();
        List<StoreInfo> targets = storeService.page(1, 500, null, null, null, rule.getRegionId()).records().stream()
                .filter(s -> s.getStatus() != null && s.getStatus().canTrade())
                .filter(s -> StorePnlService.allocatable(s.getType(), Boolean.TRUE.equals(rule.getIncludeDc()), Boolean.TRUE.equals(rule.getIncludeFranchise())))
                .toList();
        if (targets.isEmpty()) {
            throw new BizException("chain.error.allocNoStores");
        }
        BigDecimal pool = pnlService.hqExpense(month);
        Map<Long, BigDecimal> sales = rule.getBasis() == AllocBasis.SALES ? pnlService.monthSales(month) : Map.of();
        List<BigDecimal> values = targets.stream().map(s -> switch (rule.getBasis()) {
            case SALES -> sales.getOrDefault(s.getId(), BigDecimal.ZERO).max(BigDecimal.ZERO);
            case AREA -> Decimals.nz(s.getArea());
            case EQUAL -> BigDecimal.ONE;
        }).toList();
        BigDecimal total = Decimals.sum(values);
        if (total.signum() <= 0) {
            throw new BizException("chain.error.allocBasisZero");
        }

        // 替换该月上次的结果
        ChainAllocRun old = runMapper.selectOne(Wrappers.<ChainAllocRun>lambdaQuery().eq(ChainAllocRun::getPeriod, period));
        if (old != null) {
            recordMapper.delete(Wrappers.<ChainAllocRecord>lambdaQuery().eq(ChainAllocRecord::getRunId, old.getId()));
            runMapper.deleteById(old.getId());
        }
        ChainAllocRun run = new ChainAllocRun();
        run.setPeriod(period);
        run.setBasis(rule.getBasis());
        run.setPoolAmount(pool);
        run.setStoreCount(targets.size());
        runMapper.insert(run);

        List<ChainAllocRecord> records = new ArrayList<>();
        BigDecimal allocated = BigDecimal.ZERO;
        int last = -1;
        for (int i = 0; i < targets.size(); i++) {
            if (values.get(i).signum() > 0) {
                last = i;
            }
        }
        for (int i = 0; i < targets.size(); i++) {
            BigDecimal v = values.get(i);
            if (v.signum() <= 0) {
                continue;
            }
            BigDecimal amount = i == last ? pool.subtract(allocated) : pool.multiply(v).divide(total, Decimals.MONEY_SCALE, Decimals.ROUNDING);
            allocated = allocated.add(amount);
            ChainAllocRecord r = new ChainAllocRecord();
            r.setRunId(run.getId());
            r.setPeriod(period);
            r.setStoreId(targets.get(i).getId());
            r.setBasisValue(Decimals.money(v));
            r.setRatio(v.divide(total, 6, RoundingMode.HALF_UP));
            r.setAmount(amount);
            records.add(r);
        }
        recordMapper.insertBatch(records);
        return run.getId();
    }

    @Transactional
    public void delete(Long id) {
        if (!SecurityUtils.getDataScope().allStores()) {
            throw new BizException("chain.error.allocNeedsAllStores");
        }
        recordMapper.delete(Wrappers.<ChainAllocRecord>lambdaQuery().eq(ChainAllocRecord::getRunId, id));
        runMapper.deleteById(id);
    }
}
