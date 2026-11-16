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
import com.lemon.erp.modules.fin.model.BillSource;
import com.lemon.erp.modules.fin.model.IoCategoryCode;
import com.lemon.erp.modules.fin.service.FinIoService;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.dto.PosRequests;
import com.lemon.erp.modules.pos.dto.PosViews;
import com.lemon.erp.modules.pos.entity.PosDailyClose;
import com.lemon.erp.modules.pos.entity.PosDailyClosePay;
import com.lemon.erp.modules.pos.mapper.PosDailyCloseMapper;
import com.lemon.erp.modules.pos.mapper.PosDailyClosePayMapper;
import com.lemon.erp.modules.pos.mapper.PosSaleMapper;
import com.lemon.erp.modules.pos.mapper.PosShiftMapper;
import com.lemon.erp.modules.pos.model.DailyCloseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 门店日结：按门店 + 营业日期汇总销售、退货、各支付方式的收款 / 退款、各班次的应有与实点现金。
 * <ul>
 *   <li>当天还有没交班的班次（含之前日期开班、跨过零点的）时不能日结</li>
 *   <li>日结后该门店当天的 POS 单据不能再新增（收银、退货）、作废，也不能再开班；反日结（{@code pos:dailyClose:reopen}）后恢复</li>
 *   <li>账务：按结算账户汇总，收款生成一张待确认的「其他收入单」（内置类别零售收入），退款生成一张「费用支出单」（零售退款），
 *       来源为日结记录，出纳确认后生成凭证；反日结时删除这些单据（已确认的需先取消确认）</li>
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
        return new PosViews.DailySummary(storeId, store.name(), date, sales, pays, net, toInt(shifts.get("shiftCount")),
                shiftMapper.countOpenUntil(storeId, date), toDecimal(shifts.get("expectedCash")), toDecimal(shifts.get("countedCash")),
                toDecimal(shifts.get("cashDiff")), isClosed(storeId, date));
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
        payMapper.delete(Wrappers.<PosDailyClosePay>lambdaQuery().eq(PosDailyClosePay::getCloseId, id));
        StatusTransitions.change(closeMapper, PosDailyClose::getId, id, PosDailyClose::getStatus, DailyCloseStatus.CLOSED,
                DailyCloseStatus.REOPENED, w -> w.set(PosDailyClose::getReopenedBy, SecurityUtils.getUserId())
                        .set(PosDailyClose::getReopenedTime, LocalDateTime.now()), "pos.error.dailyCloseNotClosed");
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
