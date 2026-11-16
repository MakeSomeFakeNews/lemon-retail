package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.fin.model.AccountRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.modules.fin.dto.FundTransferRequest;
import com.lemon.erp.modules.fin.dto.IoRequests;
import com.lemon.erp.modules.fin.entity.FinAccount;
import com.lemon.erp.modules.fin.entity.FinFundTransfer;
import com.lemon.erp.modules.fin.mapper.FinFundTransferMapper;
import com.lemon.erp.modules.fin.model.FeeBearer;
import com.lemon.erp.modules.fin.model.FundDirection;
import com.lemon.erp.modules.fin.model.VoucherLine;
import com.lemon.erp.modules.fin.model.VoucherSource;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 资金管理：账户调拨
 */
@Service
@RequiredArgsConstructor
public class FinFundService {

    /** 账户调拨单号的编码规则标识 */
    public static final String TRANSFER_NO = "HB";

    private final FinFundTransferMapper transferMapper;
    private final FinAccountService accountService;
    private final FinAccountRuleService accountRules;
    private final FinVoucherService voucherService;
    private final FinBizPeriodGuard periodGuard;
    private final DocNoService docNoService;

    // ---------------------------------------------------------------- 账户调拨

    public PageResult<FinFundTransfer> pageTransfers(long current, long size, String keyword, LocalDate begin, LocalDate end) {
        Page<FinFundTransfer> page = Page.of(current, size);
        transferMapper.selectPageList(page, keyword, begin, end);
        return PageResult.of(page);
    }

    public FinFundTransfer getTransfer(Long id) {
        FinFundTransfer t = transferMapper.selectDetail(id);
        if (t == null) {
            throw new BizException("fin.error.transferNotFound");
        }
        return t;
    }

    /**
     * 调拨：借 转入账户 / 贷 转出账户（调拨金额）；有手续费时另记一笔 借 财务费用 / 贷 支付手续费的账户（手续费），
     * 出纳日记账、银行对账中手续费作为支付账户单独的一笔支出。合计效果：转出账户支付时 转出 金额 + 手续费、转入 金额；
     * 转入账户支付时 转出 金额、转入 金额 - 手续费（手续费须小于金额）
     */
    @Transactional
    public Long createTransfer(FundTransferRequest req) {
        if (req.fromAccountId().equals(req.toAccountId())) {
            throw new BizException("fin.error.sourceTargetAccountsCannotSame");
        }
        accountService.requireCashAccount(req.fromAccountId());
        accountService.requireCashAccount(req.toAccountId());
        periodGuard.checkOpen(req.transferDate(), "fin.label.fundTransfer");
        BigDecimal fee = Decimals.nz(req.fee());
        FeeBearer bearer = req.feeBearer() == null ? FeeBearer.FROM : req.feeBearer();
        if (bearer == FeeBearer.TO && fee.compareTo(req.amount()) >= 0) {
            throw new BizException("fin.error.transferFeeExceedsAmount");
        }
        FinFundTransfer t = new FinFundTransfer();
        t.setTransferNo(docNoService.next(TRANSFER_NO));
        t.setTransferDate(req.transferDate());
        t.setFromAccountId(req.fromAccountId());
        t.setToAccountId(req.toAccountId());
        t.setAmount(req.amount());
        t.setFee(fee);
        t.setFeeBearer(bearer);
        t.setHandlerId(req.handlerId());
        t.setTicketNo(blankToNull(req.ticketNo()));
        t.setRemark(req.remark());
        String summary = "账户调拨" + (req.remark() // i18n-ignore
                == null || req.remark().isBlank() ? "" : " " + req.remark());
        List<VoucherLine> lines = new ArrayList<>();
        lines.add(VoucherLine.debit(summary, req.toAccountId(), null, req.amount()));
        lines.add(VoucherLine.credit(summary, req.fromAccountId(), null, req.amount()));
        if (fee.signum() > 0) {
            String feeSummary = summary + "-手续费"; // i18n-ignore
            lines.add(VoucherLine.debit(feeSummary, accountRules.accountId(AccountRole.FINANCE_EXPENSE), null, fee));
            lines.add(VoucherLine.credit(feeSummary, bearer == FeeBearer.TO ? req.toAccountId() : req.fromAccountId(), null, fee));
        }
        t.setVoucherId(voucherService.createGenerated(req.transferDate(), VoucherSource.TRANSFER, t.getTransferNo(), lines));
        transferMapper.insert(t);
        return t.getId();
    }

    /** 期间内的调拨手续费（收支汇总中作为一项费用支出）：笔数、金额，没有时为 null */
    public IoRequests.SummaryRow feeSummary(LocalDate begin, LocalDate end) {
        IoRequests.SummaryRow row = transferMapper.selectFeeSummary(begin, end);
        if (row == null || row.getCount() == null || row.getCount() == 0) {
            return null;
        }
        FinAccount account = accountService.get(accountRules.accountId(AccountRole.FINANCE_EXPENSE));
        row.setTransferFee(true);
        row.setDirection(FundDirection.OUT);
        row.setAccountCode(account.getCode());
        row.setAccountName(account.getName());
        return row;
    }

    /** 删除调拨，同时删除其凭证（须未审核） */
    @Transactional
    public void deleteTransfers(List<Long> ids) {
        for (FinFundTransfer t : transferMapper.selectByIds(ids)) {
            periodGuard.checkOpen(t.getTransferDate(), "fin.label.fundTransfer");
            if (t.getVoucherId() != null) {
                voucherService.deleteOwned(t.getVoucherId());
            }
            transferMapper.deleteById(t.getId());
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
