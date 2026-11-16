package com.lemon.erp.modules.pos.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.modules.pos.dto.PosRequests;
import com.lemon.erp.modules.pos.entity.PosMember;
import com.lemon.erp.modules.pos.entity.PosMemberRule;
import com.lemon.erp.modules.pos.entity.PosPayment;
import com.lemon.erp.modules.pos.entity.PosShift;
import com.lemon.erp.modules.pos.entity.PosTerminal;
import com.lemon.erp.modules.pos.mapper.PosPaymentMapper;
import com.lemon.erp.modules.pos.model.PayMethod;
import com.lemon.erp.modules.pos.model.PaymentStatus;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 收银付款（销售结算、收银台充值共用）：校验付款组合并写收款记录。
 * <ul>
 *   <li>非现金方式合计不能超过应收；现金应收 = 应收 − 非现金合计，顾客付款不少于现金应收，多出的找零</li>
 *   <li>微信、支付宝必须是本收银台已收款成功、还没有结算的记录，金额一致（结算时条件更新关联，防止重复使用）</li>
 *   <li>储值、积分抵现需要识别会员，只能用于销售：储值需规则启用；积分抵现需规则启用、{@code pos:sale:redeem}，
 *       金额不超过应收 × 抵现上限，积分 = 金额 × 每元积分（须为整数）。余额、积分的扣减由调用方在写收款后进行（条件更新）</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class PosTenderService {

    static final String REDEEM_PERM = "pos:sale:redeem";

    private final PosPaymentMapper paymentMapper;
    private final PosTerminalService terminalService;
    private final DocNoService docNoService;
    private final PermissionService permissionService;

    /**
     * 付款校验结果
     *
     * @param cashApplied      现金应收（扣除找零后的现金实收）
     * @param electronic       商户订单号 → 已收款的聚合支付记录
     * @param storedValue      储值支付金额
     * @param pointsAmount     积分抵现金额
     * @param points           积分抵现使用的积分
     */
    public record Tenders(List<PosRequests.Tender> items, BigDecimal cashApplied, BigDecimal cashTendered, BigDecimal change,
                          BigDecimal paid, Map<String, PosPayment> electronic, BigDecimal storedValue, BigDecimal pointsAmount,
                          BigDecimal points) {
    }

    /**
     * @param member 识别的会员（储值、积分抵现需要），可为空
     * @param rule   积分与储值规则；为空表示不允许储值、积分抵现（充值）
     */
    public Tenders validate(List<PosRequests.Tender> list, BigDecimal payable, PosTerminal terminal, PosMember member, PosMemberRule rule) {
        List<PosRequests.Tender> items = list == null ? List.of() : list.stream().filter(t -> t.amount().signum() > 0).toList();
        BigDecimal nonCash = BigDecimal.ZERO;
        BigDecimal cashTendered = null;
        BigDecimal storedValue = BigDecimal.ZERO;
        BigDecimal pointsAmount = BigDecimal.ZERO;
        Map<String, PosPayment> electronic = new HashMap<>();
        for (PosRequests.Tender t : items) {
            PayMethod method = t.payMethod();
            if (method == PayMethod.CASH) {
                if (cashTendered != null) {
                    throw new BizException("pos.error.duplicateCashTender");
                }
                cashTendered = Decimals.money(t.amount());
                continue;
            }
            BigDecimal amount = Decimals.money(t.amount());
            nonCash = nonCash.add(amount);
            if (method.member()) {
                if (rule == null) {
                    throw new BizException("pos.error.memberTenderNotAllowed");
                }
                if (member == null) {
                    throw new BizException("pos.error.memberTenderNeedsMember");
                }
                if (method == PayMethod.STORED_VALUE) {
                    if (!Boolean.TRUE.equals(rule.getStoredValueEnabled())) {
                        throw new BizException("pos.error.storedValueUnavailable");
                    }
                    storedValue = storedValue.add(amount);
                } else {
                    if (!Boolean.TRUE.equals(rule.getRedeemEnabled())) {
                        throw new BizException("pos.error.redeemDisabled");
                    }
                    if (!permissionService.hasPerm(REDEEM_PERM)) {
                        throw new BizException("pos.error.noRedeemPerm");
                    }
                    pointsAmount = pointsAmount.add(amount);
                }
                continue;
            }
            if (method.electronic()) {
                PosPayment p = t.outTradeNo() == null ? null : paymentMapper.selectOne(Wrappers.<PosPayment>lambdaQuery()
                        .eq(PosPayment::getOutTradeNo, t.outTradeNo()));
                if (p == null || p.getStatus() != PaymentStatus.SUCCESS || p.getSaleId() != null || p.getTopupId() != null
                        || p.getPayMethod() != method || p.getAmount().compareTo(amount) != 0 || !Objects.equals(p.getTerminalId(), terminal.getId())) {
                    throw new BizException("pos.error.electronicPaymentInvalid", t.outTradeNo() == null ? "" : t.outTradeNo());
                }
                if (electronic.put(p.getOutTradeNo(), p) != null) {
                    throw new BizException("pos.error.electronicPaymentInvalid", p.getOutTradeNo());
                }
            }
        }
        BigDecimal points = BigDecimal.ZERO;
        if (pointsAmount.signum() > 0) {
            BigDecimal max = payable.multiply(rule.getRedeemMaxRate()).divide(Decimals.HUNDRED, 2, RoundingMode.DOWN);
            if (pointsAmount.compareTo(max) > 0) {
                throw new BizException("pos.error.redeemExceeded", max.toPlainString());
            }
            points = pointsAmount.multiply(BigDecimal.valueOf(rule.getRedeemPoints()));
            if (points.stripTrailingZeros().scale() > 0) {
                throw new BizException("pos.error.redeemNotInteger", rule.getRedeemPoints());
            }
            points = points.setScale(0, RoundingMode.UNNECESSARY);
        }
        if (nonCash.compareTo(payable) > 0) {
            throw new BizException("pos.error.overPaid", nonCash.toPlainString(), payable.toPlainString());
        }
        BigDecimal cashDue = payable.subtract(nonCash);
        BigDecimal tendered = cashTendered == null ? BigDecimal.ZERO : cashTendered;
        if (tendered.compareTo(cashDue) < 0) {
            throw new BizException("pos.error.underPaid", payable.toPlainString(), nonCash.add(tendered).toPlainString());
        }
        if (cashDue.signum() == 0 && tendered.signum() > 0) {
            throw new BizException("pos.error.cashNotNeeded");
        }
        BigDecimal change = tendered.subtract(cashDue);
        return new Tenders(items, cashDue, tendered, change, nonCash.add(tendered), electronic, storedValue, pointsAmount, points);
    }

    /**
     * 写收款记录（销售或充值）：聚合支付关联已收款的记录，其余新增成功的收款；储值、积分抵现没有结算账户
     *
     * @param saleId  销售单（与 topupId 二选一）
     * @param topupId 充值单
     */
    public void save(Long saleId, Long topupId, Long storeId, PosShift shift, PosTerminal terminal, Tenders tenders) {
        for (PosRequests.Tender t : tenders.items()) {
            Long account = t.payMethod().member() ? null : terminalService.accountFor(terminal, t.payMethod());
            if (t.payMethod().electronic()) {
                PosPayment p = tenders.electronic().get(t.outTradeNo());
                int n = saleId != null ? paymentMapper.bindSale(p.getId(), saleId, account) : paymentMapper.bindTopup(p.getId(), topupId, account);
                if (n == 0) {
                    throw new BizException("pos.error.electronicPaymentInvalid", t.outTradeNo());
                }
                continue;
            }
            PosPayment p = newPayment(saleId, storeId, shift, t.payMethod(), account);
            p.setTopupId(topupId);
            if (t.payMethod() == PayMethod.CASH) {
                p.setAmount(tenders.cashApplied());
                p.setTendered(tenders.cashTendered());
                p.setChangeAmount(tenders.change());
            } else {
                p.setAmount(Decimals.money(t.amount()));
                p.setRefNo(t.refNo());
                if (t.payMethod() == PayMethod.POINTS) {
                    p.setPoints(tenders.points());
                }
            }
            p.setStatus(PaymentStatus.SUCCESS);
            p.setPayTime(LocalDateTime.now());
            paymentMapper.insert(p);
        }
    }

    /** 新的收款 / 退款记录（商户订单号按编码规则 ZF） */
    public PosPayment newPayment(Long saleId, Long storeId, PosShift shift, PayMethod method, Long accountId) {
        PosPayment p = new PosPayment();
        p.setSaleId(saleId);
        p.setStoreId(storeId);
        p.setTerminalId(shift == null ? null : shift.getTerminalId());
        p.setShiftId(shift == null ? null : shift.getId());
        p.setBusinessDate(LocalDate.now());
        p.setPayMethod(method);
        p.setAccountId(accountId);
        p.setChangeAmount(BigDecimal.ZERO);
        p.setRefundedAmount(BigDecimal.ZERO);
        p.setOutTradeNo(docNoService.next(PosDocNos.PAYMENT));
        return p;
    }
}
