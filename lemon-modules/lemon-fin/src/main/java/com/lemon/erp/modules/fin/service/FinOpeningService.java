package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.fin.model.PeriodStatus;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.fin.dto.AccountOpeningRequest;
import com.lemon.erp.modules.fin.dto.AccountOpeningRow;
import com.lemon.erp.modules.fin.entity.FinAccount;
import com.lemon.erp.modules.fin.entity.FinAccountOpening;
import com.lemon.erp.modules.fin.entity.FinPeriod;
import com.lemon.erp.modules.fin.mapper.FinAccountOpeningMapper;
import com.lemon.erp.modules.fin.mapper.FinPeriodMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 期初余额：系统启用时录入科目期初（末级科目的借方 / 贷方余额，借贷必须平衡）；计入余额表、明细账、资产负债表的期初
 * （汇总查询中合并）。已有结账期间后不能修改
 */
@Service
@RequiredArgsConstructor
public class FinOpeningService {

    private final FinAccountOpeningMapper openingMapper;
    private final FinAccountService accountService;
    private final FinAccountRuleService accountRules;
    private final FinPeriodMapper periodMapper;

    // ---------------------------------------------------------------- 科目期初

    public List<AccountOpeningRow> accountOpenings() {
        Map<Long, FinAccountOpening> saved = openingMapper.selectList(null).stream()
                .collect(Collectors.toMap(FinAccountOpening::getAccountId, Function.identity()));
        List<FinAccount> all = accountService.listAll();
        Set<Long> parents = all.stream().map(FinAccount::getParentId).collect(Collectors.toSet());
        List<AccountOpeningRow> rows = new ArrayList<>();
        for (FinAccount a : all) {
            if (parents.contains(a.getId()) || (a.getStatus() != EnableStatus.ENABLED && !saved.containsKey(a.getId()))) {
                continue;
            }
            FinAccountOpening o = saved.get(a.getId());
            AccountOpeningRow r = new AccountOpeningRow();
            r.setAccountId(a.getId());
            r.setCode(a.getCode());
            r.setName(a.getName());
            r.setCategory(a.getCategory());
            r.setDirection(a.getDirection());
            r.setDebit(o == null ? BigDecimal.ZERO : o.getDebit());
            r.setCredit(o == null ? BigDecimal.ZERO : o.getCredit());
            rows.add(r);
        }
        return rows;
    }

    @Transactional
    public void saveAccountOpenings(AccountOpeningRequest req) {
        if (periodMapper.exists(Wrappers.<FinPeriod>lambdaQuery().eq(FinPeriod::getStatus, PeriodStatus.CLOSED))) {
            throw new BizException("fin.error.somePeriodsClosedAccountOpeningBalances");
        }
        List<FinAccountOpening> list = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        for (AccountOpeningRequest.Item item : req.items()) {
            BigDecimal d = Decimals.nz(item.debit());
            BigDecimal c = Decimals.nz(item.credit());
            if (d.signum() == 0 && c.signum() == 0) {
                continue;
            }
            if (!seen.add(item.accountId())) {
                throw new BizException("fin.error.duplicateAccount");
            }
            FinAccountOpening o = new FinAccountOpening();
            o.setAccountId(item.accountId());
            o.setDebit(d);
            o.setCredit(c);
            list.add(o);
            debit = debit.add(d);
            credit = credit.add(c);
        }
        if (debit.compareTo(credit) != 0) {
            throw new BizException("fin.error.openingBalancesDoNotBalanceDebit", debit.toPlainString(), credit.toPlainString());
        }
        if (!list.isEmpty()) {
            accountService.requireLeaves(seen);
        }
        openingMapper.delete(Wrappers.<FinAccountOpening>lambdaQuery().isNotNull(FinAccountOpening::getId));
        if (!list.isEmpty()) {
            openingMapper.insertBatch(list);
        }
    }
}
