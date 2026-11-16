package com.lemon.erp.modules.chain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.chain.dto.ChainViews;
import com.lemon.erp.modules.chain.dto.FranchiseRequests;
import com.lemon.erp.modules.chain.entity.ChainFranchiseAccount;
import com.lemon.erp.modules.chain.entity.ChainFranchiseStatement;
import com.lemon.erp.modules.chain.mapper.ChainFranchiseAccountMapper;
import com.lemon.erp.modules.chain.mapper.ChainFranchiseDepositMapper;
import com.lemon.erp.modules.chain.mapper.ChainFranchiseStatementMapper;
import com.lemon.erp.modules.chain.model.FranchiseStatementStatus;
import com.lemon.erp.modules.fin.dto.FinAnalysis;
import com.lemon.erp.modules.fin.model.BillKind;
import com.lemon.erp.modules.fin.model.BillSource;
import com.lemon.erp.modules.fin.model.PartnerLedgerType;
import com.lemon.erp.modules.fin.service.FinPartnerLedgerService;
import com.lemon.erp.modules.store.model.FranchiseSettleMode;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 加盟对账单（月结，FI-07）与加盟结算报表。数字取自财务的往来对账单（加盟商账户的客户）：
 * 期初 + 配送（FRANCHISE_DELIVERY 应收）− 退货（FRANCHISE_RETURN 红字应收，取正数）+ 其他应收（期初 / 手工、冲账单等，带符号）
 * − 收款（收款单、预收款、应收应付对冲）= 期末。预收款在收到时即冲减余额（与往来对账单一致），所以预付货款的加盟店期末可以为负数（预付结余）
 */
@Service
@RequiredArgsConstructor
public class FranchiseStatementService {

    private final ChainFranchiseStatementMapper statementMapper;
    private final ChainFranchiseAccountMapper accountMapper;
    private final ChainFranchiseDepositMapper depositMapper;
    private final FranchiseAccountService accountService;
    private final FinPartnerLedgerService ledgerService;
    private final DocNoService docNoService;

    public PageResult<ChainFranchiseStatement> page(long current, long size, Long storeId, String period, FranchiseStatementStatus status) {
        Page<ChainFranchiseStatement> page = Page.of(current, size);
        statementMapper.selectPageList(page, storeId, period, status == null ? null : status.name(), ChainSupport.scopeStores());
        return PageResult.of(page);
    }

    /** 详情（含逐笔明细，打印用） */
    public ChainFranchiseStatement detail(Long id) {
        ChainFranchiseStatement s = statementMapper.selectDetail(id);
        if (s == null) {
            throw new BizException("chain.error.statementNotFound");
        }
        ChainSupport.checkStore(s.getStoreId());
        s.setRows(ledgerService.statementOf(BillKind.AR, s.getCustomerId(), s.getBeginDate(), s.getEndDate(), false).rows());
        return s;
    }

    /**
     * 生成（或重新生成待确认的）对账单：一个加盟店一个月一张，已确认的不能重新生成
     *
     * @return 对账单 ID
     */
    @Transactional
    public Long generate(FranchiseRequests.Statement req) {
        ChainSupport.checkStore(req.storeId());
        ChainFranchiseAccount account = accountService.ensure(req.storeId());
        YearMonth ym = YearMonth.parse(req.period());
        LocalDate begin = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        Figures f = figures(account.getCustomerId(), begin, end);
        ChainFranchiseStatement s = statementMapper.selectOne(Wrappers.<ChainFranchiseStatement>lambdaQuery()
                .eq(ChainFranchiseStatement::getStoreId, req.storeId()).eq(ChainFranchiseStatement::getPeriod, req.period()));
        if (s != null && s.getStatus() == FranchiseStatementStatus.CONFIRMED) {
            throw new BizException("chain.error.statementConfirmed", s.getStatementNo());
        }
        boolean create = s == null;
        if (create) {
            s = new ChainFranchiseStatement();
            s.setStatementNo(docNoService.next(ChainDocNos.STATEMENT));
            s.setStoreId(req.storeId());
            s.setPeriod(req.period());
            s.setStatus(FranchiseStatementStatus.DRAFT);
        }
        s.setCustomerId(account.getCustomerId());
        s.setBeginDate(begin);
        s.setEndDate(end);
        s.setOpening(f.opening());
        s.setDeliveryAmount(f.delivery());
        s.setReturnAmount(f.returned());
        s.setPaymentAmount(f.payment());
        s.setOtherAmount(f.other());
        s.setClosing(f.closing());
        s.setRemark(req.remark());
        if (create) {
            statementMapper.insert(s);
        } else {
            statementMapper.updateById(s);
        }
        return s.getId();
    }

    @Transactional
    public void confirm(Long id) {
        ChainFranchiseStatement s = get(id);
        StatusTransitions.change(statementMapper, ChainFranchiseStatement::getId, id, ChainFranchiseStatement::getStatus,
                FranchiseStatementStatus.DRAFT, FranchiseStatementStatus.CONFIRMED, w -> w
                        .set(ChainFranchiseStatement::getConfirmBy, SecurityUtils.getUserId())
                        .set(ChainFranchiseStatement::getConfirmTime, LocalDateTime.now()),
                "chain.error.statementConfirmed", s.getStatementNo());
    }

    /** 取消确认：已确认 → 待确认（可以重新生成） */
    @Transactional
    public void unconfirm(Long id) {
        ChainFranchiseStatement s = get(id);
        StatusTransitions.change(statementMapper, ChainFranchiseStatement::getId, id, ChainFranchiseStatement::getStatus,
                FranchiseStatementStatus.CONFIRMED, FranchiseStatementStatus.DRAFT, w -> w
                        .set(ChainFranchiseStatement::getConfirmBy, null)
                        .set(ChainFranchiseStatement::getConfirmTime, null),
                "chain.error.statementNotConfirmed", s.getStatementNo());
    }

    /** 只能删除待确认的 */
    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            ChainFranchiseStatement s = get(id);
            if (s.getStatus() != FranchiseStatementStatus.DRAFT) {
                throw new BizException("chain.error.statementConfirmed", s.getStatementNo());
            }
            statementMapper.deleteById(id);
        }
    }

    /**
     * 加盟商往来明细：加盟店在日期范围内的财务往来对账单（应收、收款、预收逐笔与余额）
     */
    public FinAnalysis.PartnerStatement ledger(Long storeId, LocalDate begin, LocalDate end) {
        ChainSupport.checkStore(storeId);
        ChainFranchiseAccount account = accountService.ensure(storeId);
        return ledgerService.statementOf(BillKind.AR, account.getCustomerId(), begin, end, false);
    }

    /**
     * 加盟结算汇总：每个加盟店（可管辖的、已有账户的）在日期范围内的期初、配送、退货、收款、其他、期末与保证金余额
     */
    public List<ChainViews.FranchiseSummaryRow> summary(LocalDate begin, LocalDate end, FranchiseSettleMode settleMode) {
        if (begin == null || end == null || end.isBefore(begin)) {
            throw new BizException("chain.error.dateRangeInvalid");
        }
        Set<Long> scope = ChainSupport.scopeStores();
        Page<ChainFranchiseAccount> page = Page.of(1, 500);
        accountMapper.selectPageList(page, null, null, settleMode == null ? null : settleMode.name(), scope);
        List<ChainViews.FranchiseSummaryRow> rows = new ArrayList<>();
        for (ChainFranchiseAccount a : page.getRecords()) {
            if (a.getCustomerId() == null) {
                continue;
            }
            Figures f = figures(a.getCustomerId(), begin, end);
            ChainViews.FranchiseSummaryRow r = new ChainViews.FranchiseSummaryRow();
            r.setStoreId(a.getStoreId());
            r.setStoreCode(a.getStoreCode());
            r.setStoreName(a.getStoreName());
            r.setFranchisee(a.getFranchisee());
            r.setSettleMode(a.getSettleMode() == null ? null : a.getSettleMode().name());
            r.setOpening(f.opening());
            r.setDeliveryAmount(f.delivery());
            r.setReturnAmount(f.returned());
            r.setPaymentAmount(f.payment());
            r.setOtherAmount(f.other());
            r.setClosing(f.closing());
            r.setDepositBalance(Decimals.nz(depositMapper.selectBalance(a.getStoreId())));
            rows.add(r);
        }
        return rows;
    }

    private record Figures(BigDecimal opening, BigDecimal delivery, BigDecimal returned, BigDecimal payment, BigDecimal other, BigDecimal closing) {
    }

    /** 往来对账单按来源分类：配送、退货、其他应收、收款 */
    private Figures figures(Long customerId, LocalDate begin, LocalDate end) {
        FinAnalysis.PartnerStatement st = ledgerService.statementOf(BillKind.AR, customerId, begin, end, false);
        BigDecimal delivery = BigDecimal.ZERO;
        BigDecimal returned = BigDecimal.ZERO;
        BigDecimal other = BigDecimal.ZERO;
        BigDecimal payment = BigDecimal.ZERO;
        for (FinAnalysis.StatementLine l : st.rows()) {
            if (l.type() == PartnerLedgerType.BILL) {
                BigDecimal v = Decimals.nz(l.increase());
                if (BillSource.FRANCHISE_DELIVERY.name().equals(l.sourceType())) {
                    delivery = delivery.add(v);
                } else if (BillSource.FRANCHISE_RETURN.name().equals(l.sourceType())) {
                    returned = returned.subtract(v);
                } else {
                    other = other.add(v);
                }
            } else {
                payment = payment.add(Decimals.nz(l.decrease()));
            }
        }
        return new Figures(Decimals.nz(st.opening()), delivery, returned, payment, other, Decimals.nz(st.closing()));
    }

    private ChainFranchiseStatement get(Long id) {
        ChainFranchiseStatement s = statementMapper.selectById(id);
        if (s == null) {
            throw new BizException("chain.error.statementNotFound");
        }
        ChainSupport.checkStore(s.getStoreId());
        return s;
    }
}
