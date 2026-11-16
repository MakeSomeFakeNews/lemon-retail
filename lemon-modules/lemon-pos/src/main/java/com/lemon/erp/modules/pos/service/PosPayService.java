package com.lemon.erp.modules.pos.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.pos.dto.PosRequests;
import com.lemon.erp.modules.pos.dto.PosViews;
import com.lemon.erp.modules.pos.entity.PosPayConfig;
import com.lemon.erp.modules.pos.entity.PosPayment;
import com.lemon.erp.modules.pos.entity.PosShift;
import com.lemon.erp.modules.pos.entity.PosTerminal;
import com.lemon.erp.modules.pos.mapper.PosPayConfigMapper;
import com.lemon.erp.modules.pos.mapper.PosPaymentMapper;
import com.lemon.erp.modules.pos.model.PayProviderType;
import com.lemon.erp.modules.pos.model.PaymentStatus;
import com.lemon.erp.modules.pos.pay.PayProvider;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 聚合支付：微信、支付宝收款先经通道收款（结算前），成功后结算时关联到 POS 单据；退款按原收款原路退回。
 * <ul>
 *   <li>通道按租户的支付通道设置选用（没有设置时为线下登记）；通道的外部调用都在事务之外，收款记录的写入各自一个短事务</li>
 *   <li>商户订单号 out_trade_no 是幂等键（唯一约束）：前端生成、重试时沿用，同一订单号不会重复收款</li>
 *   <li>已收款但结算失败（库存不足等）的收款可以「撤销」原路退回；未付款的可以关闭</li>
 * </ul>
 */
@Slf4j
@Service
public class PosPayService {

    private final PosPaymentMapper paymentMapper;
    private final PosPayConfigMapper configMapper;
    private final PosTerminalService terminalService;
    private final PosShiftService shiftService;
    private final DocNoService docNoService;
    private final TransactionTemplate tx;
    private final Map<PayProviderType, PayProvider> providers = new EnumMap<>(PayProviderType.class);

    public PosPayService(PosPaymentMapper paymentMapper, PosPayConfigMapper configMapper, PosTerminalService terminalService,
                         PosShiftService shiftService, DocNoService docNoService, TransactionTemplate tx, List<PayProvider> providerBeans) {
        this.paymentMapper = paymentMapper;
        this.configMapper = configMapper;
        this.terminalService = terminalService;
        this.shiftService = shiftService;
        this.docNoService = docNoService;
        this.tx = tx;
        providerBeans.forEach(p -> providers.put(p.type(), p));
    }

    // ---------------------------------------------------------------- 通道设置

    /** 当前租户的通道设置，没有时为线下登记 */
    public PosPayConfig config() {
        PosPayConfig c = configMapper.selectOne(Wrappers.<PosPayConfig>lambdaQuery());
        if (c == null) {
            c = new PosPayConfig();
            c.setProvider(PayProviderType.OFFLINE);
        }
        return c;
    }

    /** 设置页面：密钥只返回是否已设置；可选的通道为已加载的实现 */
    public PosViews.PayConfig configView() {
        PosPayConfig c = config();
        return new PosViews.PayConfig(c.getProvider(), c.getAppId(), c.getMerchantNo(), StringUtils.hasText(c.getApiKey()),
                StringUtils.hasText(c.getApiSecret()), c.getNotifyUrl(), c.getRemark(), List.copyOf(providers.keySet()));
    }

    /** 保存通道设置：密钥为空时保持原值 */
    public void saveConfig(PosRequests.PayConfig req) {
        provider(req.provider());
        PosPayConfig old = configMapper.selectOne(Wrappers.<PosPayConfig>lambdaQuery());
        PosPayConfig c = old != null ? old : new PosPayConfig();
        c.setProvider(req.provider());
        c.setAppId(req.appId());
        c.setMerchantNo(req.merchantNo());
        if (StringUtils.hasText(req.apiKey())) c.setApiKey(req.apiKey());
        if (StringUtils.hasText(req.apiSecret())) c.setApiSecret(req.apiSecret());
        c.setNotifyUrl(req.notifyUrl());
        c.setRemark(req.remark());
        if (old == null) {
            configMapper.insert(c);
        } else {
            configMapper.updateById(c);
        }
    }

    public PayProviderType currentProvider() {
        return config().getProvider();
    }

    private PayProvider provider(PayProviderType type) {
        PayProvider p = providers.get(type);
        if (p == null) {
            throw new BizException("pos.error.payProviderUnavailable", type.name());
        }
        return p;
    }

    // ---------------------------------------------------------------- 收款

    /**
     * 聚合支付收款（结算前）：同一商户订单号重复提交时返回已有的记录（处理中的先查询结果）
     */
    public PosPayment pay(PosRequests.Pay req) {
        if (!req.payMethod().electronic()) {
            throw new BizException("pos.error.payMethodNotElectronic");
        }
        PosTerminal terminal = terminalService.requireForCashier(req.terminalId());
        PosShift shift = shiftService.requireOwnOpen(terminal);
        if (StringUtils.hasText(req.outTradeNo())) {
            PosPayment existing = findByNo(req.outTradeNo());
            if (existing != null) {
                if (existing.getPayMethod() != req.payMethod() || existing.getAmount().compareTo(req.amount()) != 0
                        || !Objects.equals(existing.getTerminalId(), terminal.getId())) {
                    throw new BizException("pos.error.outTradeNoConflict", req.outTradeNo());
                }
                return existing.getStatus() == PaymentStatus.PENDING ? query(existing.getOutTradeNo()) : existing;
            }
        }
        PosPayConfig config = config();
        PayProvider provider = provider(config.getProvider());
        PosPayment p;
        try {
            p = tx.execute(s -> insertPending(req, terminal, shift, config.getProvider()));
        } catch (DuplicateKeyException e) {
            // 并发重复提交同一订单号
            return findByNo(req.outTradeNo());
        }
        PayProvider.PayResult r;
        try {
            r = provider.pay(config, new PayProvider.PayCommand(p.getOutTradeNo(), p.getPayMethod(), p.getAmount(), req.authCode(),
                    Messages.get("pos.text.paySubject", terminal.getStoreName())));
        } catch (RuntimeException e) {
            // 网络异常等结果未知时保持处理中，可再查询或撤销
            log.warn("聚合支付收款调用失败 {}", p.getOutTradeNo(), e);
            return p;
        }
        return applyResult(p, r);
    }

    private PosPayment insertPending(PosRequests.Pay req, PosTerminal terminal, PosShift shift, PayProviderType type) {
        PosPayment p = new PosPayment();
        p.setStoreId(terminal.getStoreId());
        p.setTerminalId(terminal.getId());
        p.setShiftId(shift.getId());
        p.setBusinessDate(java.time.LocalDate.now());
        p.setPayMethod(req.payMethod());
        p.setAmount(req.amount());
        p.setChangeAmount(BigDecimal.ZERO);
        p.setRefundedAmount(BigDecimal.ZERO);
        p.setProvider(type);
        p.setStatus(PaymentStatus.PENDING);
        p.setOutTradeNo(StringUtils.hasText(req.outTradeNo()) ? req.outTradeNo() : docNoService.next(PosDocNos.PAYMENT));
        paymentMapper.insert(p);
        return p;
    }

    /** 线下登记：收银员确认顾客已付款，登记参考号 */
    public PosPayment confirmOffline(String outTradeNo, String refNo) {
        PosPayment p = requireAccessible(outTradeNo);
        if (p.getProvider() != PayProviderType.OFFLINE || p.getStatus() != PaymentStatus.PENDING) {
            throw new BizException("pos.error.paymentNotPending", outTradeNo);
        }
        update(p.getId(), PaymentStatus.PENDING, PaymentStatus.SUCCESS, null, refNo, null);
        return paymentMapper.selectById(p.getId());
    }

    /** 查询收款结果：处理中的向通道查询并更新 */
    public PosPayment query(String outTradeNo) {
        PosPayment p = requireAccessible(outTradeNo);
        if (p.getStatus() != PaymentStatus.PENDING || p.getAmount().signum() < 0) {
            return p;
        }
        PosPayConfig config = config();
        return applyResult(p, provider(p.getProvider()).query(config, outTradeNo, p.getAmount()));
    }

    /**
     * 撤销结算前的收款：处理中的关闭交易；已成功但还没有结算的原路全额退回。都记为已关闭
     */
    public PosPayment cancel(String outTradeNo) {
        PosPayment p = requireAccessible(outTradeNo);
        PosPayConfig config = config();
        PayProvider provider = provider(p.getProvider());
        if (p.getStatus() == PaymentStatus.PENDING) {
            provider.close(config, outTradeNo);
            update(p.getId(), PaymentStatus.PENDING, PaymentStatus.CLOSED, null, null, null);
        } else if (p.getStatus() == PaymentStatus.SUCCESS && p.getSaleId() == null && p.getAmount().signum() > 0) {
            String refundNo = tx.execute(s -> docNoService.next(PosDocNos.PAYMENT));
            PayProvider.PayResult r = provider.refund(config, outTradeNo, refundNo, p.getAmount(), p.getAmount());
            if (r.status() != PaymentStatus.SUCCESS) {
                throw new BizException("pos.error.refundFailed", r.message() == null ? "" : r.message());
            }
            // 结算与撤销互斥：只撤销仍未关联单据的
            int n = paymentMapper.update(Wrappers.<PosPayment>lambdaUpdate().set(PosPayment::getStatus, PaymentStatus.CLOSED)
                    .set(PosPayment::getFailReason, Messages.get("pos.text.paymentCancelled"))
                    .set(PosPayment::getUpdateTime, LocalDateTime.now())
                    .eq(PosPayment::getId, p.getId()).eq(PosPayment::getStatus, PaymentStatus.SUCCESS).isNull(PosPayment::getSaleId));
            if (n == 0) {
                throw new BizException("pos.error.paymentAlreadySettled", outTradeNo);
            }
        } else {
            throw new BizException("pos.error.paymentNotCancellable", outTradeNo);
        }
        return paymentMapper.selectById(p.getId());
    }

    // ---------------------------------------------------------------- 退款

    /**
     * 向通道办理退款（退货、作废的事务提交之后调用）：处理中的退款记录按原收款原路退回，更新为成功 / 失败；
     * 失败的可在单据详情中重试
     */
    public void processRefunds(Collection<Long> refundIds) {
        if (refundIds == null || refundIds.isEmpty()) {
            return;
        }
        PosPayConfig config = config();
        for (Long id : refundIds) {
            PosPayment refund = paymentMapper.selectById(id);
            if (refund == null || refund.getStatus() != PaymentStatus.PENDING || refund.getOrigPaymentId() == null) {
                continue;
            }
            PosPayment orig = paymentMapper.selectById(refund.getOrigPaymentId());
            PayProvider.PayResult r;
            try {
                r = provider(orig.getProvider()).refund(config, orig.getOutTradeNo(), refund.getOutTradeNo(), refund.getAmount().negate(),
                        orig.getAmount());
            } catch (RuntimeException e) {
                log.warn("聚合支付退款调用失败 {}", refund.getOutTradeNo(), e);
                r = PayProvider.PayResult.failed(e.getMessage());
            }
            update(id, PaymentStatus.PENDING, r.status() == PaymentStatus.SUCCESS ? PaymentStatus.SUCCESS : PaymentStatus.FAILED,
                    r.tradeNo(), null, r.message());
        }
    }

    /** 重试失败的退款 */
    public PosPayment retryRefund(Long id) {
        PosPayment refund = paymentMapper.selectById(id);
        if (refund == null || refund.getAmount().signum() >= 0) {
            throw new BizException("pos.error.paymentNotFound");
        }
        PosTerminalService.checkStore(refund.getStoreId());
        if (refund.getStatus() != PaymentStatus.FAILED) {
            throw new BizException("pos.error.refundNotFailed");
        }
        update(id, PaymentStatus.FAILED, PaymentStatus.PENDING, null, null, null);
        processRefunds(List.of(id));
        return paymentMapper.selectById(id);
    }

    // ---------------------------------------------------------------- 内部

    PosPayment findByNo(String outTradeNo) {
        return paymentMapper.selectOne(Wrappers.<PosPayment>lambdaQuery().eq(PosPayment::getOutTradeNo, outTradeNo));
    }

    private PosPayment requireAccessible(String outTradeNo) {
        PosPayment p = StringUtils.hasText(outTradeNo) ? findByNo(outTradeNo) : null;
        if (p == null) {
            throw new BizException("pos.error.paymentNotFound");
        }
        PosTerminalService.checkStore(p.getStoreId());
        return p;
    }

    private PosPayment applyResult(PosPayment p, PayProvider.PayResult r) {
        if (r.status() == PaymentStatus.PENDING) {
            if (r.qrCode() != null) {
                paymentMapper.update(Wrappers.<PosPayment>lambdaUpdate().set(PosPayment::getQrCode, r.qrCode()).eq(PosPayment::getId, p.getId()));
            }
        } else {
            update(p.getId(), PaymentStatus.PENDING, r.status(), r.tradeNo(), null, r.message());
        }
        return paymentMapper.selectById(p.getId());
    }

    /** 状态从 from 改为 to（成功时记录时间），只更新仍为 from 的记录 */
    @Transactional
    public void update(Long id, PaymentStatus from, PaymentStatus to, String tradeNo, String refNo, String failReason) {
        paymentMapper.update(Wrappers.<PosPayment>lambdaUpdate()
                .set(PosPayment::getStatus, to)
                .set(tradeNo != null, PosPayment::getTradeNo, tradeNo)
                .set(refNo != null, PosPayment::getRefNo, refNo)
                .set(failReason != null, PosPayment::getFailReason, failReason == null ? null : cut(failReason))
                .set(to == PaymentStatus.SUCCESS, PosPayment::getPayTime, LocalDateTime.now())
                .set(PosPayment::getUpdateTime, LocalDateTime.now())
                .eq(PosPayment::getId, id).eq(PosPayment::getStatus, from));
    }

    private static String cut(String s) {
        return s.length() <= 255 ? s : s.substring(0, 255);
    }
}
