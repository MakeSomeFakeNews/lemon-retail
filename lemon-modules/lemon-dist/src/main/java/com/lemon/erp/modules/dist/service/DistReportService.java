package com.lemon.erp.modules.dist.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataScope;
import com.lemon.erp.framework.store.StoreLookup;
import com.lemon.erp.modules.dist.dto.DistViews;
import com.lemon.erp.modules.dist.mapper.DistReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 门店对比报表（RP-06 / RP-07 部分）：按门店汇总日期范围内的销售、退货、毛利、采购入库、配送入库、要货满足率与配送及时率，
 * 以及当前库存金额和库存周转。门店范围：可按区域（含下级）或门店筛选，再与用户的门店范围取交集。
 * 销售数据来自供应链的销售出库 / 退货，加上 POS 零售（按 POS 生成的零售出库 / 零售退货仓库单据，dist 不依赖 pos）
 */
@Service
@RequiredArgsConstructor
public class DistReportService {

    /** 日期范围上限（天） */
    private static final long MAX_DAYS = 366;

    private final DistReportMapper reportMapper;
    private final StoreLookup storeLookup;

    public List<DistViews.StoreCompareRow> storeCompare(LocalDate begin, LocalDate end, Long regionId, Long storeId) {
        if (begin == null || end == null || end.isBefore(begin) || begin.plusDays(MAX_DAYS).isBefore(end)) {
            throw new BizException("dist.error.dateRangeInvalid", MAX_DAYS);
        }
        Set<Long> stores = null;
        if (regionId != null) {
            stores = new HashSet<>(storeLookup.storesInRegion(regionId));
        }
        if (storeId != null) {
            stores = stores == null ? new HashSet<>(Set.of(storeId)) : stores;
            stores.retainAll(Set.of(storeId));
        }
        DataScope scope = SecurityUtils.getDataScope();
        if (!scope.allStores()) {
            if (stores == null) {
                stores = new HashSet<>(scope.storeIds());
            } else {
                stores.retainAll(scope.storeIds());
            }
        }
        if (stores != null && stores.isEmpty()) {
            return List.of();
        }
        List<DistViews.StoreCompareRow> rows = reportMapper.selectStoreCompare(begin, end, stores);
        rows.forEach(DistReportService::derive);
        return rows;
    }

    private static void derive(DistViews.StoreCompareRow r) {
        BigDecimal net = Decimals.nz(r.getSalesAmount()).subtract(Decimals.nz(r.getReturnAmount()));
        r.setNetSales(Decimals.money(net));
        r.setCost(Decimals.money(Decimals.nz(r.getCost())));
        r.setGrossProfit(Decimals.money(net.subtract(r.getCost())));
        r.setGrossMargin(percent(r.getGrossProfit(), net));
        r.setStockAmount(Decimals.money(Decimals.nz(r.getStockAmount())));
        r.setTurnover(r.getStockAmount().signum() > 0 ? r.getCost().divide(r.getStockAmount(), 2, RoundingMode.HALF_UP) : null);
        r.setFillRate(percent(r.getDeliveredQty(), r.getApprovedQty()));
        r.setOnTimeRate(r.getDeliveryCount() == null || r.getDeliveryCount() == 0 ? null
                : percent(BigDecimal.valueOf(r.getOnTimeCount()), BigDecimal.valueOf(r.getDeliveryCount())));
    }

    /** 百分比（2 位小数），分母为 0 时为空 */
    private static BigDecimal percent(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() == 0) {
            return null;
        }
        return Decimals.nz(part).multiply(Decimals.HUNDRED).divide(whole, 2, RoundingMode.HALF_UP);
    }
}
