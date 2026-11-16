package com.lemon.erp.modules.pos.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataPermissionRule;
import com.lemon.erp.framework.security.datascope.DataScopes;
import com.lemon.erp.framework.store.StoreBrief;
import com.lemon.erp.framework.store.StoreLookup;
import com.lemon.erp.modules.fin.entity.FinIoCategory;
import com.lemon.erp.modules.fin.model.BillSource;
import com.lemon.erp.modules.fin.model.IoCategoryCode;
import com.lemon.erp.modules.fin.model.VoucherLine;
import com.lemon.erp.modules.fin.model.VoucherSource;
import com.lemon.erp.modules.fin.service.FinIoCategoryService;
import com.lemon.erp.modules.fin.service.FinIoService;
import com.lemon.erp.modules.fin.service.FinVoucherService;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.dto.PosRequests;
import com.lemon.erp.modules.pos.dto.PosViews;
import com.lemon.erp.modules.pos.entity.PosDailyClose;
import com.lemon.erp.modules.pos.entity.PosDailyClosePay;
import com.lemon.erp.modules.pos.mapper.PosDailyCloseMapper;
import com.lemon.erp.modules.pos.mapper.PosDailyClosePayMapper;
import com.lemon.erp.modules.pos.mapper.PosSaleMapper;
import com.lemon.erp.modules.pos.mapper.PosShiftMapper;
import com.lemon.erp.modules.pos.mapper.PosStoredValueLogMapper;
import com.lemon.erp.modules.pos.mapper.PosTopupMapper;
import com.lemon.erp.modules.pos.model.PayMethod;
import com.lemon.erp.modules.pos.model.DailyCloseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 门店日结：按门店 + 营业日期汇总销售、退货、各支付方式的收款 / 退款、各班次的应有与实点现金。
 * <ul>
 *   <li>当天还有没交班的班次（含之前日期开班、跨过零点的）时不能日结</li>
 *   <li>日结后该门店当天的 POS 单据不能再新增（收银、退货）、作废，也不能再开班；反日结（{@code pos:dailyClose:reopen}）后恢复</li>
 *   <li>账务：按结算账户汇总，收款生成一张待确认的「其他收入单」（内置类别零售收入），退款生成一张「费用支出单」（零售退款），
 *       来源为日结记录，出纳确认后生成凭证；反日结时删除这些单据（已确认的需先取消确认）。储值、积分抵现没有结算账户，不生成收支单
 *       （积分抵现视为折让，收入只按实收确认）</li>
 *   <li>储值：充值收款按结算账户生成「其他收入单」（会员储值，科目预收账款，负债）；另生成一张未审核凭证：本店储值消费净额
 *       借 会员储值 / 贷 零售收入，充值赠送 借 储值赠送（销售费用）/ 贷 会员储值；反日结时删除（凭证须未审核）</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PosDailyCloseService {

    static final DataPermissionRule RULE = new DataPermissionRule("pos_daily_close", null, null, "store_id", null);

    private final PosDailyCloseMapper closeMapper;
    private final PosDailyClosePayMapper payMapper;
    private final PosSaleMapper saleMapper;
    private final PosShiftMapper shiftMapper;
    private final StoreLookup storeLookup;
    private final FinIoService ioService;
    private final FinIoCategoryService categoryService;
    private final FinVoucherService voucherService;
    private final PosTopupMapper topupMapper;
    private final PosStoredValueLogMapper storedValueLogMapper;

    @DataPermission(table = "pos_daily_close", deptColumn = "", userColumn = "", storeColumn = "store_id")
    public PageResult<PosDailyClose> page(long current, long size, PosQueries.DailyClose q) {
        Page<PosDailyClose> page = Page.of(current, size);
        closeMapper.selectPageList(page, q);
        return PageResult.of(page);
    }

    public PosDailyClose detail(Long id) {
        PosDailyClose c = DataScopes.require(RULE, () -> id == null ? null : closeMapper.selectDetail(id), "pos.error.dailyCloseNotFound");
        c.setPays(payMapper.selectByCloseId(id));
        return c;
    }

    /** 门店该营业日期是否已日结 */
    public boolean isClosed(Long storeId, LocalDate date) {
        return closeMapper.exists(Wrappers.<PosDailyClose>lambdaQuery().eq(PosDailyClose::getStoreId, storeId)
                .eq(PosDailyClose::getBusinessDate, date).eq(PosDailyClose::getStatus, DailyCloseStatus.CLOSED));
    }

    /** 日期锁定：门店该营业日期已日结时报错（POS 单据不能新增、作废，不能开班） */
    public void checkNotClosed(Long storeId, LocalDate date) {
        if (isClosed(storeId, date)) {
            throw new BizException("pos.error.dayClosed", storeName(storeId), date.toString());
        }
    }

    /** 日结试算 */
    public PosViews.DailySummary summary(Long storeId, LocalDate date) {
        PosTerminalService.checkStore(storeId);
        StoreBrief store = storeLookup.require(storeId);
        PosViews.SaleSummary sales = saleMapper.selectDaySales(storeId, date);
        List<PosViews.PayRow> pays = saleMapper.selectDayPays(storeId, date);
        BigDecimal net = pays.stream().map(PosViews.PayRow::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> shifts = saleMapper.selectDayShifts(storeId, date);
        Map<String, Object> topups = topupMapper.selectDaySummary(storeId, date);
        BigDecimal points = pays.stream().filter(r -> r.getPayMethod() == PayMethod.POINTS).map(PosViews.PayRow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PosViews.DailySummary(storeId, store.name(), date, sales, pays, net, toInt(shifts.get("shiftCount")),
                shiftMapper.countOpenUntil(storeId, date), toDecimal(shifts.get("expectedCash")), toDecimal(shifts.get("countedCash")),
                toDecimal(shifts.get("cashDiff")), isClosed(storeId, date), toInt(topups.get("topupCount")), toDecimal(topups.get("topupAmount")),
                toDecimal(topups.get("bonusAmount")), saleMapper.selectDayTopupPays(storeId, date), storedValueLogMapper.selectDayConsume(storeId, date),
                points);
    }

    /** 日结：生成日结记录和收支单（同一事务） */
    @Transactional
    public Long close(PosRequests.DailyClose req) {
        LocalDate date = req.businessDate();
        if (date.isAfter(LocalDate.now())) {
            throw new BizException("pos.error.dailyCloseFuture");
        }
        PosViews.DailySummary s = summary(req.storeId(), date);
        if (s.closed()) {
            throw new BizException("pos.error.dayClosed", s.storeName(), date.toString());
        }
        if (s.openShifts() > 0) {
            throw new BizException("pos.error.dailyCloseOpenShifts", s.openShifts());
        }
        PosDailyClose old = closeMapper.selectOne(Wrappers.<PosDailyClose>lambdaQuery().eq(PosDailyClose::getStoreId, req.storeId())
                .eq(PosDailyClose::getBusinessDate, date));
        PosDailyClose c = old != null ? old : new PosDailyClose();
        c.setStoreId(req.storeId());
        c.setBusinessDate(date);
        c.setStatus(DailyCloseStatus.CLOSED);
        c.setShiftCount(s.shiftCount());
        c.setSaleCount(s.sales().getSaleCount());
        c.setSaleAmount(s.sales().getSaleAmount());
        c.setReturnCount(s.sales().getReturnCount());
        c.setReturnAmount(s.sales().getReturnAmount());
        c.setNetAmount(s.netAmount());
        c.setTopupCount(s.topupCount());
        c.setTopupAmount(s.topupAmount());
        c.setBonusAmount(s.bonusAmount());
        c.setStoredValueAmount(s.storedValueAmount());
        c.setPointsAmount(s.pointsAmount());
        c.setExpectedCash(s.expectedCash());
        c.setCountedCash(s.countedCash());
        c.setCashDiff(s.cashDiff());
        c.setClosedBy(SecurityUtils.getUserId());
        c.setClosedTime(LocalDateTime.now());
        c.setRemark(req.remark());
        if (old == null) {
            closeMapper.insert(c);
        } else {
            // 反日结后再次日结：状态从已反日结改为已日结（并发日结时只有一个成功）
            StatusTransitions.change(closeMapper, PosDailyClose::getId, old.getId(), PosDailyClose::getStatus, DailyCloseStatus.REOPENED,
                    DailyCloseStatus.CLOSED, "pos.error.dayClosed", s.storeName(), date.toString());
            closeMapper.updateById(c);
        }
        savePays(c, s.pays());
        saveTopups(c, s.topupPays());
        Long voucherId = storedValueVoucher(c);
        if (voucherId != null) {
            closeMapper.update(Wrappers.<PosDailyClose>lambdaUpdate().set(PosDailyClose::getVoucherId, voucherId).eq(PosDailyClose::getId, c.getId()));
        }
        return c.getId();
    }

    /** 反日结：删除生成的收支单（须待确认），恢复当天的收银、退货、作废 */
    @Transactional
    public void reopen(Long id) {
        PosDailyClose c = detail(id);
        if (c.getStatus() != DailyCloseStatus.CLOSED) {
            throw new BizException("pos.error.dailyCloseNotClosed");
        }
        ioService.deleteGenerated(BillSource.POS_DAILY_CLOSE, id);
        if (c.getVoucherId() != null) {
            voucherService.deleteOwned(c.getVoucherId());
        }
        payMapper.delete(Wrappers.<PosDailyClosePay>lambdaQuery().eq(PosDailyClosePay::getCloseId, id));
        StatusTransitions.change(closeMapper, PosDailyClose::getId, id, PosDailyClose::getStatus, DailyCloseStatus.CLOSED,
                DailyCloseStatus.REOPENED, w -> w.set(PosDailyClose::getReopenedBy, SecurityUtils.getUserId())
                        .set(PosDailyClose::getReopenedTime, LocalDateTime.now()).set(PosDailyClose::getVoucherId, null),
                "pos.error.dailyCloseNotClosed");
    }

    /** 收款明细，并按结算账户生成收入 / 支出单 */
    private void savePays(PosDailyClose c, List<PosViews.PayRow> rows) {
        payMapper.delete(Wrappers.<PosDailyClosePay>lambdaQuery().eq(PosDailyClosePay::getCloseId, c.getId()));
        if (rows.isEmpty()) {
            return;
        }
        List<PosDailyClosePay> pays = rows.stream().map(r -> {
            PosDailyClosePay p = new PosDailyClosePay();
            p.setCloseId(c.getId());
            p.setPayMethod(r.getPayMethod());
            p.setAccountId(r.getAccountId());
            p.setReceiptAmount(r.getReceiptAmount());
            p.setRefundAmount(r.getRefundAmount());
            p.setPayCount(r.getPayCount());
            return p;
        }).toList();
        payMapper.insertBatch(pays);
        // 按结算账户合并（同一账户可能对应多种支付方式）
        Map<Long, BigDecimal[]> byAccount = new LinkedHashMap<>();
        for (PosViews.PayRow r : rows) {
            if (r.getAccountId() == null) {
                continue;
            }
            BigDecimal[] sum = byAccount.computeIfAbsent(r.getAccountId(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            sum[0] = sum[0].add(r.getReceiptAmount());
            sum[1] = sum[1].add(r.getRefundAmount());
        }
        String sourceNo = storeCode(c.getStoreId()) + "-" + c.getBusinessDate();
        String remark = Messages.get("pos.text.dailyCloseIoRemark", storeName(c.getStoreId()), c.getBusinessDate().toString());
        byAccount.forEach((accountId, sum) -> {
            if (sum[0].signum() > 0) {
                ioService.createGenerated(IoCategoryCode.RETAIL_SALES, c.getBusinessDate(), accountId, sum[0], BillSource.POS_DAILY_CLOSE,
                        c.getId(), sourceNo, remark);
            }
            if (sum[1].signum() > 0) {
                ioService.createGenerated(IoCategoryCode.RETAIL_REFUND, c.getBusinessDate(), accountId, sum[1], BillSource.POS_DAILY_CLOSE,
                        c.getId(), sourceNo, remark);
            }
        });
    }

    /** 储值充值收款按结算账户生成其他收入单（会员储值：借 结算账户 / 贷 预收账款） */
    private void saveTopups(PosDailyClose c, List<PosViews.PayRow> rows) {
        Map<Long, BigDecimal> byAccount = new LinkedHashMap<>();
        for (PosViews.PayRow r : rows) {
            if (r.getAccountId() != null) {
                byAccount.merge(r.getAccountId(), r.getAmount(), BigDecimal::add);
            }
        }
        String sourceNo = storeCode(c.getStoreId()) + "-" + c.getBusinessDate();
        String remark = Messages.get("pos.text.dailyCloseTopupRemark", storeName(c.getStoreId()), c.getBusinessDate().toString());
        byAccount.forEach((accountId, amount) -> {
            if (amount.signum() > 0) {
                ioService.createGenerated(IoCategoryCode.MEMBER_TOPUP, c.getBusinessDate(), accountId, amount, BillSource.POS_DAILY_CLOSE,
                        c.getId(), sourceNo, remark);
            }
        });
    }

    /**
     * 储值凭证（未审核，来源门店日结）：本店储值消费净额 借 会员储值科目 / 贷 零售收入科目；充值赠送 借 储值赠送科目 / 贷 会员储值科目。
     * 科目取内置收支类别（会员储值、零售收入、储值赠送）上设置的科目；都为 0 时不生成
     *
     * @return 凭证 ID，没有生成时为空
     */
    private Long storedValueVoucher(PosDailyClose c) {
        BigDecimal consume = c.getStoredValueAmount() == null ? BigDecimal.ZERO : c.getStoredValueAmount();
        BigDecimal bonus = c.getBonusAmount() == null ? BigDecimal.ZERO : c.getBonusAmount();
        if (consume.signum() == 0 && bonus.signum() == 0) {
            return null;
        }
        Long liability = categoryAccount(IoCategoryCode.MEMBER_TOPUP);
        String summary = Messages.get("pos.text.dailyCloseVoucher", storeName(c.getStoreId()), c.getBusinessDate().toString());
        List<VoucherLine> lines = new ArrayList<>();
        if (consume.signum() != 0) {
            lines.add(VoucherLine.debit(summary, liability, null, consume));
            lines.add(VoucherLine.credit(summary, categoryAccount(IoCategoryCode.RETAIL_SALES), null, consume));
        }
        if (bonus.signum() != 0) {
            lines.add(VoucherLine.debit(summary, categoryAccount(IoCategoryCode.MEMBER_BONUS), null, bonus));
            lines.add(VoucherLine.credit(summary, liability, null, bonus));
        }
        return voucherService.createGenerated(c.getBusinessDate(), VoucherSource.POS_DAILY_CLOSE,
                storeCode(c.getStoreId()) + "-" + c.getBusinessDate(), lines);
    }

    private Long categoryAccount(IoCategoryCode code) {
        FinIoCategory category = categoryService.getBuiltin(code);
        if (category.getAccountId() == null) {
            throw new BizException("pos.error.ioCategoryAccountMissing", category.getName());
        }
        return category.getAccountId();
    }

    private String storeName(Long storeId) {
        return storeLookup.find(storeId).map(StoreBrief::name).orElse(String.valueOf(storeId));
    }

    private String storeCode(Long storeId) {
        return storeLookup.find(storeId).map(StoreBrief::code).orElse(String.valueOf(storeId));
    }

    private static int toInt(Object v) {
        return v == null ? 0 : ((Number) v).intValue();
    }

    private static BigDecimal toDecimal(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }
}
