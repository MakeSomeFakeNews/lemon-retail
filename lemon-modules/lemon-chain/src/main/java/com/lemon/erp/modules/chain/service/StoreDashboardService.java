package com.lemon.erp.modules.chain.service;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.modules.chain.dto.ChainViews;
import com.lemon.erp.modules.chain.mapper.ChainReportMapper;
import com.lemon.erp.modules.store.entity.StoreInfo;
import com.lemon.erp.modules.store.service.StoreInfoService;
import com.lemon.erp.modules.wms.service.WmsStockDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 店长看板（MP-01，小程序店长工作台）：门店今天与昨天的 POS 销售（含税实收净额、单数、客单价）、毛利（不含税净额 − 成本，
 * 需要「查看成本金额」权限）、今天的畅销商品，以及待收货的配送单、草稿要货单数。门店须可管辖
 */
@Service
@RequiredArgsConstructor
public class StoreDashboardService {

    private final ChainReportMapper reportMapper;
    private final StoreInfoService storeService;
    private final PermissionService permissionService;

    public ChainViews.StoreDashboard dashboard(Long storeId, LocalDate date) {
        ChainSupport.checkStore(storeId);
        StoreInfo store = storeService.get(storeId);
        LocalDate day = date == null ? LocalDate.now() : date;
        boolean cost = permissionService.hasPerm(WmsStockDocService.COST_PERM);
        ChainViews.StoreDashboard d = new ChainViews.StoreDashboard();
        d.setStoreId(storeId);
        d.setStoreName(store.getName());
        d.setDate(day);
        d.setToday(figures(storeId, day, cost));
        d.setYesterday(figures(storeId, day.minusDays(1), cost));
        d.setTopItems(reportMapper.selectTopItems(storeId, day));
        d.setPendingReceive(reportMapper.countPendingReceive(storeId));
        d.setDraftRequisitions(reportMapper.countDraftRequisitions(storeId));
        return d;
    }

    private ChainViews.DayFigures figures(Long storeId, LocalDate day, boolean withCost) {
        ChainViews.DayFigures f = reportMapper.selectDayFigures(storeId, day);
        if (f == null) {
            f = new ChainViews.DayFigures();
        }
        f.setSales(Decimals.money(Decimals.nz(f.getSales())));
        f.setNetSales(Decimals.money(Decimals.nz(f.getNetSales())));
        f.setAvgTicket(f.getOrders() == null || f.getOrders() == 0 ? null
                : f.getSales().divide(java.math.BigDecimal.valueOf(f.getOrders()), 2, RoundingMode.HALF_UP));
        if (withCost) {
            f.setCost(Decimals.money(Decimals.nz(reportMapper.selectDayCost(storeId, day))));
            f.setGrossProfit(f.getNetSales().subtract(f.getCost()));
        }
        return f;
    }
}
