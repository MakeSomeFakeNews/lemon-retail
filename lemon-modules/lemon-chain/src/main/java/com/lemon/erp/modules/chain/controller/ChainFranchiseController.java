package com.lemon.erp.modules.chain.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.chain.dto.ChainViews;
import com.lemon.erp.modules.chain.dto.FranchiseRequests;
import com.lemon.erp.modules.chain.entity.ChainFranchiseAccount;
import com.lemon.erp.modules.chain.entity.ChainFranchiseDeposit;
import com.lemon.erp.modules.chain.entity.ChainFranchiseLedger;
import com.lemon.erp.modules.chain.entity.ChainFranchiseStatement;
import com.lemon.erp.modules.chain.model.FranchiseDepositKind;
import com.lemon.erp.modules.chain.model.FranchiseStatementStatus;
import com.lemon.erp.modules.chain.service.FranchiseAccountService;
import com.lemon.erp.modules.chain.service.FranchiseDepositService;
import com.lemon.erp.modules.chain.service.FranchiseSettlementService;
import com.lemon.erp.modules.chain.service.FranchiseStatementService;
import com.lemon.erp.modules.fin.dto.FinAnalysis;
import com.lemon.erp.modules.store.model.FranchiseSettleMode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 加盟管理：加盟商账户（余额、额度、充值）、加盟往来、对账单、保证金、加盟结算报表
 */
@RestController
@RequestMapping("/chain/franchise")
@RequiredArgsConstructor
public class ChainFranchiseController {

    private final FranchiseAccountService accountService;
    private final FranchiseSettlementService settlementService;
    private final FranchiseStatementService statementService;
    private final FranchiseDepositService depositService;

    // ---------------------------------------------------------------- 加盟商账户

    @GetMapping("/account")
    @PreAuthorize("@ss.hasPerm('chain:franchise:list')")
    public R<PageResult<ChainFranchiseAccount>> accounts(@RequestParam(defaultValue = "1") long current,
                                                         @RequestParam(defaultValue = "10") long size,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) Long storeId,
                                                         @RequestParam(required = false) FranchiseSettleMode settleMode) {
        return R.ok(accountService.page(current, size, keyword, storeId, settleMode));
    }

    @GetMapping("/account/{storeId}")
    @PreAuthorize("@ss.hasAnyPerm('chain:franchise:list', 'chain:franchise:report')")
    public R<ChainFranchiseAccount> account(@PathVariable Long storeId) {
        return R.ok(accountService.detail(storeId));
    }

    @PutMapping("/account/{storeId}")
    @PreAuthorize("@ss.hasPerm('chain:franchise:edit')")
    @OperLog(title = "加盟商账户", type = OperType.UPDATE) // i18n-ignore
    public R<Void> updateAccount(@PathVariable Long storeId, @Valid @RequestBody FranchiseRequests.Account req) {
        accountService.update(storeId, req);
        return R.ok();
    }

    /** 充值（预付货款）：登记预收款并自动冲抵未核销的加盟应收，返回预收款单 ID */
    @PostMapping("/account/{storeId}/topup")
    @PreAuthorize("@ss.hasPerm('chain:franchise:topup')")
    @OperLog(title = "加盟商充值", type = OperType.INSERT) // i18n-ignore
    public R<Long> topup(@PathVariable Long storeId, @Valid @RequestBody FranchiseRequests.Topup req) {
        return R.ok(accountService.topup(storeId, req));
    }

    /** 加盟往来：配送、退回生成的加盟应收 */
    @GetMapping("/ledger")
    @PreAuthorize("@ss.hasAnyPerm('chain:franchise:list', 'chain:franchise:report')")
    public R<PageResult<ChainFranchiseLedger>> ledger(@RequestParam(defaultValue = "1") long current,
                                                      @RequestParam(defaultValue = "10") long size,
                                                      @RequestParam(required = false) Long storeId,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                                      @RequestParam(required = false) String keyword) {
        return R.ok(settlementService.page(current, size, storeId, begin, end, keyword));
    }

    // ---------------------------------------------------------------- 报表

    /** 加盟商往来明细：一个加盟店在日期范围内的应收、收款、预收逐笔与余额 */
    @GetMapping("/account/{storeId}/statement")
    @PreAuthorize("@ss.hasAnyPerm('chain:franchise:list', 'chain:franchise:report')")
    public R<FinAnalysis.PartnerStatement> partnerStatement(@PathVariable Long storeId,
                                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return R.ok(statementService.ledger(storeId, begin, end));
    }

    /** 加盟结算汇总 */
    @GetMapping("/summary")
    @PreAuthorize("@ss.hasPerm('chain:franchise:report')")
    public R<List<ChainViews.FranchiseSummaryRow>> summary(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                                           @RequestParam(required = false) FranchiseSettleMode settleMode) {
        return R.ok(statementService.summary(begin, end, settleMode));
    }

    // ---------------------------------------------------------------- 对账单

    @GetMapping("/statement")
    @PreAuthorize("@ss.hasPerm('chain:statement:list')")
    public R<PageResult<ChainFranchiseStatement>> statements(@RequestParam(defaultValue = "1") long current,
                                                             @RequestParam(defaultValue = "10") long size,
                                                             @RequestParam(required = false) Long storeId,
                                                             @RequestParam(required = false) String period,
                                                             @RequestParam(required = false) FranchiseStatementStatus status) {
        return R.ok(statementService.page(current, size, storeId, period, status));
    }

    @GetMapping("/statement/{id}")
    @PreAuthorize("@ss.hasPerm('chain:statement:list')")
    public R<ChainFranchiseStatement> statement(@PathVariable Long id) {
        return R.ok(statementService.detail(id));
    }

    /** 生成（重新生成待确认的）对账单，返回 ID */
    @PostMapping("/statement")
    @PreAuthorize("@ss.hasPerm('chain:statement:add')")
    @OperLog(title = "加盟对账单", type = OperType.INSERT) // i18n-ignore
    public R<Long> generateStatement(@Valid @RequestBody FranchiseRequests.Statement req) {
        return R.ok(statementService.generate(req));
    }

    @PostMapping("/statement/{id}/confirm")
    @PreAuthorize("@ss.hasPerm('chain:statement:confirm')")
    @OperLog(title = "加盟对账单-确认", type = OperType.UPDATE) // i18n-ignore
    public R<Void> confirmStatement(@PathVariable Long id) {
        statementService.confirm(id);
        return R.ok();
    }

    @PostMapping("/statement/{id}/unconfirm")
    @PreAuthorize("@ss.hasPerm('chain:statement:confirm')")
    @OperLog(title = "加盟对账单-取消确认", type = OperType.UPDATE) // i18n-ignore
    public R<Void> unconfirmStatement(@PathVariable Long id) {
        statementService.unconfirm(id);
        return R.ok();
    }

    @DeleteMapping("/statement/{ids}")
    @PreAuthorize("@ss.hasPerm('chain:statement:remove')")
    @OperLog(title = "加盟对账单", type = OperType.DELETE) // i18n-ignore
    public R<Void> deleteStatements(@PathVariable List<Long> ids) {
        statementService.delete(ids);
        return R.ok();
    }

    // ---------------------------------------------------------------- 保证金

    @GetMapping("/deposit")
    @PreAuthorize("@ss.hasPerm('chain:deposit:list')")
    public R<PageResult<ChainFranchiseDeposit>> deposits(@RequestParam(defaultValue = "1") long current,
                                                         @RequestParam(defaultValue = "10") long size,
                                                         @RequestParam(required = false) Long storeId,
                                                         @RequestParam(required = false) FranchiseDepositKind kind,
                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return R.ok(depositService.page(current, size, storeId, kind, begin, end));
    }

    @GetMapping("/deposit/balance")
    @PreAuthorize("@ss.hasPerm('chain:deposit:list')")
    public R<BigDecimal> depositBalance(@RequestParam Long storeId) {
        return R.ok(depositService.balance(storeId));
    }

    @PostMapping("/deposit")
    @PreAuthorize("@ss.hasPerm('chain:deposit:add')")
    @OperLog(title = "加盟保证金", type = OperType.INSERT) // i18n-ignore
    public R<Long> addDeposit(@Valid @RequestBody FranchiseRequests.Deposit req) {
        return R.ok(depositService.create(req));
    }

    @DeleteMapping("/deposit/{ids}")
    @PreAuthorize("@ss.hasPerm('chain:deposit:remove')")
    @OperLog(title = "加盟保证金", type = OperType.DELETE) // i18n-ignore
    public R<Void> deleteDeposits(@PathVariable List<Long> ids) {
        depositService.delete(ids);
        return R.ok();
    }
}
