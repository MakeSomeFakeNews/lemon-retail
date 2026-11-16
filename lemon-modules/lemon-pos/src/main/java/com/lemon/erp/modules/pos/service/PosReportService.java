package com.lemon.erp.modules.pos.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataPermissionContext;
import com.lemon.erp.framework.security.datascope.DataPermissionRule;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.dto.PosViews;
import com.lemon.erp.modules.pos.mapper.PosReportMapper;
import com.lemon.erp.modules.pos.model.PayMethod;
import com.lemon.erp.modules.pos.model.PosReportDimension;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 收银报表：收银日报（按日期 / 门店 / 收银台 / 收银员 / 班次汇总单据与各支付方式的净收款）、POS 销售明细。
 * 都按用户的门店范围过滤（单据、收款表的 store_id）
 */
@Service
@RequiredArgsConstructor
public class PosReportService {

    private static final int MAX_DAYS = 366;
    private static final DataPermissionRule SALE_RULE = new DataPermissionRule("pos_sale", null, null, "store_id", null);
    private static final DataPermissionRule PAY_RULE = new DataPermissionRule("pos_payment", null, null, "store_id", null);

    private final PosReportMapper reportMapper;

    public PosViews.CashierReport cashier(PosQueries.CashierReport query) {
        LocalDate end = query.end() == null ? LocalDate.now() : query.end();
        LocalDate begin = query.begin() == null ? end : query.begin();
        if (begin.isAfter(end) || ChronoUnit.DAYS.between(begin, end) >= MAX_DAYS) {
            throw new BizException("pos.error.reportRangeInvalid", MAX_DAYS);
        }
        PosQueries.CashierReport q = new PosQueries.CashierReport(query.groupBy() == null ? PosReportDimension.DATE : query.groupBy(),
                query.storeId(), query.terminalId(), query.cashierId(), begin, end);
        List<PosViews.ReportSaleRow> sales = DataPermissionContext.execute(SALE_RULE, () -> reportMapper.selectCashierSales(q));
        List<PosViews.ReportPayRow> pays = DataPermissionContext.execute(PAY_RULE, () -> reportMapper.selectCashierPays(q));
        Map<String, PosViews.CashierRow> rows = new LinkedHashMap<>();
        for (PosViews.ReportSaleRow s : sales) {
            PosViews.CashierRow r = row(rows, s.getGroupKey(), s.getGroupName());
            r.setSaleCount(s.getSaleCount());
            r.setSaleAmount(s.getSaleAmount());
            r.setReturnCount(s.getReturnCount());
            r.setReturnAmount(s.getReturnAmount());
            r.setNetAmount(s.getSaleAmount().subtract(s.getReturnAmount()));
        }
        for (PosViews.ReportPayRow p : pays) {
            PosViews.CashierRow r = row(rows, p.getGroupKey(), p.getGroupName());
            r.getPays().merge(p.getPayMethod().name(), p.getAmount(), BigDecimal::add);
            r.setPayTotal(r.getPayTotal().add(p.getAmount()));
        }
        List<PosViews.CashierRow> list = new ArrayList<>(rows.values());
        list.sort((a, b) -> Objects.compare(a.getGroupKey(), b.getGroupKey(), String::compareTo));
        PosViews.CashierRow total = emptyRow(null, null);
        for (PosViews.CashierRow r : list) {
            total.setSaleCount(total.getSaleCount() + r.getSaleCount());
            total.setSaleAmount(total.getSaleAmount().add(r.getSaleAmount()));
            total.setReturnCount(total.getReturnCount() + r.getReturnCount());
            total.setReturnAmount(total.getReturnAmount().add(r.getReturnAmount()));
            total.setNetAmount(total.getNetAmount().add(r.getNetAmount()));
            r.getPays().forEach((k, v) -> total.getPays().merge(k, v, BigDecimal::add));
            total.setPayTotal(total.getPayTotal().add(r.getPayTotal()));
        }
        return new PosViews.CashierReport(list, total);
    }

    @DataPermission(table = "pos_sale", deptColumn = "", userColumn = "", storeColumn = "store_id")
    public PageResult<PosViews.DetailRow> detail(long current, long size, PosQueries.Sale q) {
        Page<PosViews.DetailRow> page = Page.of(current, size);
        reportMapper.selectDetail(page, q);
        return PageResult.of(page);
    }

    private static PosViews.CashierRow row(Map<String, PosViews.CashierRow> rows, String key, String name) {
        return rows.computeIfAbsent(key == null ? "" : key, k -> emptyRow(k, name));
    }

    private static PosViews.CashierRow emptyRow(String key, String name) {
        PosViews.CashierRow r = new PosViews.CashierRow();
        r.setGroupKey(key);
        r.setGroupName(name);
        r.setSaleCount(0);
        r.setSaleAmount(BigDecimal.ZERO);
        r.setReturnCount(0);
        r.setReturnAmount(BigDecimal.ZERO);
        r.setNetAmount(BigDecimal.ZERO);
        Map<String, BigDecimal> pays = new LinkedHashMap<>();
        for (PayMethod m : PayMethod.values()) {
            pays.put(m.name(), BigDecimal.ZERO);
        }
        r.setPays(pays);
        r.setPayTotal(BigDecimal.ZERO);
        return r;
    }
}
