package com.lemon.erp.modules.pos.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataPermissionRule;
import com.lemon.erp.framework.security.datascope.DataScopes;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.dto.PosRequests;
import com.lemon.erp.modules.pos.dto.PosViews;
import com.lemon.erp.modules.pos.entity.PosShift;
import com.lemon.erp.modules.pos.entity.PosTerminal;
import com.lemon.erp.modules.pos.mapper.PosShiftMapper;
import com.lemon.erp.modules.pos.model.PayMethod;
import com.lemon.erp.modules.pos.model.ShiftStatus;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 班次：收银员在收银台开班（登记备用金）后才能收银、退货；每台收银台同时只有一个当班的班次。
 * 交班时按支付方式汇总本班次的收款 / 退款，应有现金 = 备用金 + 现金净收款，与实点现金的差额记在班次上（长款为正、短款为负）
 */
@Service
@RequiredArgsConstructor
public class PosShiftService {

    /** 班次的数据权限：按门店 */
    static final DataPermissionRule RULE = new DataPermissionRule("pos_shift", null, null, "store_id", null);
    /** 代其他收银员交班 */
    static final String CLOSE_PERM = "pos:shift:close";

    private final PosShiftMapper shiftMapper;
    private final PosTerminalService terminalService;
    private final PosDailyCloseService dailyCloseService;
    private final DocNoService docNoService;
    private final PermissionService permissionService;

    @DataPermission(table = "pos_shift", deptColumn = "", userColumn = "", storeColumn = "store_id")
    public PageResult<PosShift> page(long current, long size, PosQueries.Shift q) {
        Page<PosShift> page = Page.of(current, size);
        shiftMapper.selectPageList(page, q);
        return PageResult.of(page);
    }

    public PosShift get(Long id) {
        return DataScopes.require(RULE, () -> id == null ? null : shiftMapper.selectDetail(id), "pos.error.shiftNotFound");
    }

    /** 收银台当班的班次，没有时为空 */
    public PosShift current(Long terminalId) {
        PosShift s = shiftMapper.selectOne(Wrappers.<PosShift>lambdaQuery().eq(PosShift::getTerminalId, terminalId)
                .eq(PosShift::getStatus, ShiftStatus.OPEN));
        return s == null ? null : shiftMapper.selectDetail(s.getId());
    }

    /**
     * 收银、退货使用的班次：收银台当班、且是当前用户开的班次（其他收银员需要先交班）
     */
    public PosShift requireOwnOpen(PosTerminal terminal) {
        PosShift s = current(terminal.getId());
        if (s == null) {
            throw new BizException("pos.error.shiftNotOpen", terminal.getName());
        }
        if (!Objects.equals(s.getCashierId(), SecurityUtils.getUserId())) {
            throw new BizException("pos.error.shiftOfOther", s.getCashierName());
        }
        return s;
    }

    /** 开班：同一收银台已有当班的班次时不能再开；门店当日已日结时不能开班 */
    @Transactional
    public PosShift open(PosRequests.ShiftOpen req) {
        PosTerminal terminal = terminalService.requireForCashier(req.terminalId());
        terminalService.lock(terminal.getId());
        PosShift existing = current(terminal.getId());
        if (existing != null) {
            throw new BizException("pos.error.shiftAlreadyOpen", terminal.getName(), existing.getCashierName());
        }
        LocalDate today = LocalDate.now();
        dailyCloseService.checkNotClosed(terminal.getStoreId(), today);
        PosShift s = new PosShift();
        s.setShiftNo(docNoService.next(PosDocNos.SHIFT));
        s.setStoreId(terminal.getStoreId());
        s.setTerminalId(terminal.getId());
        s.setCashierId(SecurityUtils.getUserId());
        s.setBusinessDate(today);
        s.setOpenTime(LocalDateTime.now());
        s.setOpeningCash(Decimals.money(req.openingCash()));
        s.setStatus(ShiftStatus.OPEN);
        s.setSaleCount(0);
        s.setSaleAmount(BigDecimal.ZERO);
        s.setReturnCount(0);
        s.setReturnAmount(BigDecimal.ZERO);
        s.setCashAmount(BigDecimal.ZERO);
        s.setExpectedCash(s.getOpeningCash());
        shiftMapper.insert(s);
        return shiftMapper.selectDetail(s.getId());
    }

    /** 交班单（当班时为实时汇总） */
    public PosViews.ShiftReport report(Long id) {
        PosShift s = get(id);
        List<PosViews.PayRow> pays = shiftMapper.selectPays(id);
        BigDecimal cash = pays.stream().filter(p -> p.getPayMethod() == PayMethod.CASH).map(PosViews.PayRow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PosViews.ShiftReport(s, pays, shiftMapper.selectSales(id), cash, s.getOpeningCash().add(cash), shiftMapper.countUnsettled(id));
    }

    /**
     * 交班：本人的班次，或有代交班权限。有已收款未结算的聚合支付时不能交班（先结算或退款），保证收款都在单据上
     */
    @Transactional
    public PosViews.ShiftReport close(Long id, PosRequests.ShiftClose req) {
        PosShift s = get(id);
        if (s.getStatus() != ShiftStatus.OPEN) {
            throw new BizException("pos.error.shiftClosed", s.getShiftNo());
        }
        if (!Objects.equals(s.getCashierId(), SecurityUtils.getUserId()) && !permissionService.hasPerm(CLOSE_PERM)) {
            throw new BizException("pos.error.shiftOfOther", s.getCashierName());
        }
        PosViews.ShiftReport r = report(id);
        if (r.unsettled() > 0) {
            throw new BizException("pos.error.shiftUnsettledPayments", r.unsettled());
        }
        BigDecimal counted = Decimals.money(req.countedCash());
        StatusTransitions.change(shiftMapper, PosShift::getId, id, PosShift::getStatus, ShiftStatus.OPEN, ShiftStatus.CLOSED,
                w -> w.set(PosShift::getCloseTime, LocalDateTime.now())
                        .set(PosShift::getSaleCount, r.sales().getSaleCount())
                        .set(PosShift::getSaleAmount, r.sales().getSaleAmount())
                        .set(PosShift::getReturnCount, r.sales().getReturnCount())
                        .set(PosShift::getReturnAmount, r.sales().getReturnAmount())
                        .set(PosShift::getCashAmount, r.cashAmount())
                        .set(PosShift::getExpectedCash, r.expectedCash())
                        .set(PosShift::getCountedCash, counted)
                        .set(PosShift::getCashDiff, counted.subtract(r.expectedCash()))
                        .set(PosShift::getClosedBy, SecurityUtils.getUserId())
                        .set(PosShift::getRemark, req.remark())
                        .set(PosShift::getUpdateTime, LocalDateTime.now()),
                "pos.error.shiftClosed", s.getShiftNo());
        return report(id);
    }
}
