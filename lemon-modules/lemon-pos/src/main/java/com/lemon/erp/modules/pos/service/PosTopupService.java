package com.lemon.erp.modules.pos.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataPermissionRule;
import com.lemon.erp.framework.security.datascope.DataScopes;
import com.lemon.erp.framework.store.StoreBrief;
import com.lemon.erp.framework.store.StoreLookup;
import com.lemon.erp.modules.fin.service.FinAccountService;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.dto.PosRequests;
import com.lemon.erp.modules.pos.entity.PosMember;
import com.lemon.erp.modules.pos.entity.PosMemberRule;
import com.lemon.erp.modules.pos.entity.PosPayment;
import com.lemon.erp.modules.pos.entity.PosShift;
import com.lemon.erp.modules.pos.entity.PosTerminal;
import com.lemon.erp.modules.pos.entity.PosTopup;
import com.lemon.erp.modules.pos.mapper.PosPaymentMapper;
import com.lemon.erp.modules.pos.mapper.PosTopupMapper;
import com.lemon.erp.modules.pos.model.PayMethod;
import com.lemon.erp.modules.pos.model.PaymentStatus;
import com.lemon.erp.modules.pos.model.StoredValueType;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

/**
 * 储值充值（单号 CZ）：收银台充值（{@code pos:sale:topup}，在当班班次中收款，可组合现金、微信、支付宝、银行卡，聚合支付先收款再关联）
 * 或后台充值（{@code pos:topup:add}，登记收款方式与结算账户）。同一事务中写充值单、收款记录（topup_id）、储值流水（充值、赠送），
 * 收款门店当天已日结时不能充值。赠送金额默认按档位，与档位不同时需要「后台充值」权限。
 * 门店日结时充值收款按结算账户生成其他收入单（会员储值，贷 预收账款），赠送在日结凭证中 借 储值赠送科目 / 贷 会员储值科目
 */
@Service
@RequiredArgsConstructor
public class PosTopupService {

    static final String TOPUP_PERM = "pos:sale:topup";
    static final String ADMIN_TOPUP_PERM = "pos:topup:add";
    static final DataPermissionRule RULE = DataPermissionRule.withStore("pos_topup", "dept_id", "create_by");

    private final PosTopupMapper topupMapper;
    private final PosPaymentMapper paymentMapper;
    private final PosTerminalService terminalService;
    private final PosShiftService shiftService;
    private final PosDailyCloseService dailyCloseService;
    private final PosMemberService memberService;
    private final PosMemberRuleService ruleService;
    private final PosStoredValueService storedValueService;
    private final PosTenderService tenderService;
    private final FinAccountService accountService;
    private final StoreLookup storeLookup;
    private final DocNoService docNoService;
    private final PermissionService permissionService;

    @DataPermission(table = "pos_topup", deptColumn = "dept_id", userColumn = "create_by", storeColumn = "store_id")
    public PageResult<PosTopup> page(long current, long size, PosQueries.Topup q) {
        Page<PosTopup> page = Page.of(current, size);
        topupMapper.selectPageList(page, q);
        boolean sensitive = permissionService.hasPerm(PosMemberService.SENSITIVE_PERM);
        if (!sensitive) {
            page.getRecords().forEach(t -> t.setMemberPhone(PosMemberService.maskPhone(t.getMemberPhone())));
        }
        return PageResult.of(page);
    }

    public PosTopup detail(Long id) {
        PosTopup t = DataScopes.require(RULE, () -> id == null ? null : topupMapper.selectDetail(id), "pos.error.topupNotFound");
        t.setPayments(paymentMapper.selectByTopupId(id));
        if (!permissionService.hasPerm(PosMemberService.SENSITIVE_PERM)) {
            t.setMemberPhone(PosMemberService.maskPhone(t.getMemberPhone()));
        }
        return t;
    }

    /** 收银台充值：当班班次、门店未日结；付款不能用储值、积分 */
    @Transactional
    public Long topupAtPos(PosRequests.Topup req) {
        if (!permissionService.hasPerm(TOPUP_PERM)) {
            throw new BizException("pos.error.noTopupPerm");
        }
        PosTerminal terminal = terminalService.requireForCashier(req.terminalId());
        PosShift shift = shiftService.requireOwnOpen(terminal);
        LocalDate today = LocalDate.now();
        dailyCloseService.checkNotClosed(terminal.getStoreId(), today);
        PosMember member = memberService.requireActive(req.memberId());
        BigDecimal amount = Decimals.money(req.amount());
        BigDecimal bonus = bonus(amount, req.bonusAmount());
        PosTenderService.Tenders tenders = tenderService.validate(req.tenders(), amount, terminal, member, null);
        PosTopup t = newTopup(terminal.getStoreId(), member, amount, bonus, req.remark());
        t.setTerminalId(terminal.getId());
        t.setShiftId(shift.getId());
        t.setPayMethods(tenders.items().stream().map(x -> x.payMethod().name()).collect(Collectors.toCollection(LinkedHashSet::new))
                .stream().collect(Collectors.joining(",")));
        topupMapper.insert(t);
        tenderService.save(null, t.getId(), t.getStoreId(), shift, terminal, tenders);
        credit(t, member);
        return t.getId();
    }

    /** 后台充值：门店可管辖、可以开单、当天未日结；结算账户为空时取门店默认结算账户 */
    @Transactional
    public Long topupAtAdmin(PosRequests.AdminTopup req) {
        StoreBrief store = storeLookup.requireTrading(req.storeId());
        PosTerminalService.checkStore(store.id());
        LocalDate today = LocalDate.now();
        dailyCloseService.checkNotClosed(store.id(), today);
        if (req.payMethod().member()) {
            throw new BizException("pos.error.memberTenderNotAllowed");
        }
        Long accountId = req.accountId() != null ? req.accountId() : store.defaultAccountId();
        if (accountId == null) {
            throw new BizException("pos.error.topupAccountRequired");
        }
        accountService.checkStoreAccount(store.id(), accountId);
        PosMember member = memberService.requireActive(req.memberId());
        BigDecimal amount = Decimals.money(req.amount());
        BigDecimal bonus = bonus(amount, req.bonusAmount());
        PosTopup t = newTopup(store.id(), member, amount, bonus, req.remark());
        t.setPayMethods(req.payMethod().name());
        topupMapper.insert(t);
        PosPayment p = tenderService.newPayment(null, store.id(), null, req.payMethod(), accountId);
        p.setTopupId(t.getId());
        p.setAmount(amount);
        if (req.payMethod() == PayMethod.CASH) {
            p.setTendered(amount);
        }
        p.setRefNo(req.refNo());
        p.setStatus(PaymentStatus.SUCCESS);
        p.setPayTime(LocalDateTime.now());
        paymentMapper.insert(p);
        credit(t, member);
        return t.getId();
    }

    /** 赠送金额：为空按档位；与档位不同时需要「后台充值」权限 */
    private BigDecimal bonus(BigDecimal amount, BigDecimal requested) {
        PosMemberRule rule = ruleService.get();
        if (!Boolean.TRUE.equals(rule.getStoredValueEnabled())) {
            throw new BizException("pos.error.storedValueUnavailable");
        }
        BigDecimal byTier = ruleService.bonusFor(amount);
        if (requested == null) {
            return byTier;
        }
        BigDecimal b = Decimals.money(requested);
        if (b.compareTo(byTier) != 0 && !permissionService.hasPerm(ADMIN_TOPUP_PERM)) {
            throw new BizException("pos.error.bonusOverridePerm", byTier.toPlainString());
        }
        return b;
    }

    private PosTopup newTopup(Long storeId, PosMember member, BigDecimal amount, BigDecimal bonus, String remark) {
        LoginUser user = SecurityUtils.getLoginUser();
        PosTopup t = new PosTopup();
        t.setTopupNo(docNoService.next(PosDocNos.TOPUP));
        t.setStoreId(storeId);
        t.setMemberId(member.getId());
        t.setBusinessDate(LocalDate.now());
        t.setAmount(amount);
        t.setBonusAmount(bonus);
        t.setBalanceAfter(BigDecimal.ZERO);
        t.setRemark(remark);
        t.setDeptId(user.deptId());
        return t;
    }

    /** 余额增加：充值、赠送各一条流水（门店为收款门店），回写充值后余额 */
    private void credit(PosTopup t, PosMember member) {
        BigDecimal after = storedValueService.change(member.getId(), t.getStoreId(), StoredValueType.TOPUP, t.getAmount(), t.getBusinessDate(),
                null, t.getId(), t.getTopupNo(), t.getRemark());
        if (t.getBonusAmount().signum() > 0) {
            after = storedValueService.change(member.getId(), t.getStoreId(), StoredValueType.BONUS, t.getBonusAmount(), t.getBusinessDate(),
                    null, t.getId(), t.getTopupNo(), null);
        }
        PosTopup u = new PosTopup();
        u.setId(t.getId());
        u.setBalanceAfter(after);
        topupMapper.updateById(u);
    }
}
