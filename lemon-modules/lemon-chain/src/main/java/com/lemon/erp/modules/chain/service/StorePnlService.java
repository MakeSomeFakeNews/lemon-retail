package com.lemon.erp.modules.chain.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.store.StoreLookup;
import com.lemon.erp.framework.store.StoreType;
import com.lemon.erp.modules.chain.dto.ChainViews;
import com.lemon.erp.modules.chain.dto.ChainViews.PnlRow;
import com.lemon.erp.modules.chain.mapper.ChainReportMapper;
import com.lemon.erp.modules.store.entity.StoreInfo;
import com.lemon.erp.modules.store.service.StoreInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;

/**
 * 门店损益（RP-10，需求 §14 规则 4）。金额为本位币、不含税，成本按全公司移动加权平均（库存流水的实际成本，不分门店计算成本）：
 * <pre>
 * 零售净额（POS 销售 − 退货，实收 ÷ (1 + 税率)，不含作废）+ 批发净额（单据仓库属于本门店的销售出库 − 销售退货）= 销售额
 * 销售额 − 销售成本（零售 / 批发出入库的库存流水实际成本）= 毛利
 * 毛利 + 内部调拨收入（门店往来：本店调出的差价调拨，调拨金额 − 调出成本）− 内部调拨成本（本店调入的差价调拨的差价）
 *      + 其他收入 − 门店费用（已确认的其他收支，按结算账户的门店，共用账户计入总部；门店日结生成的零售收支不计）
 *      − 分摊的总部费用（{@link AllocationService} 按月运行的分摊结果；总部为负数，即已分摊出去的部分）= 经营利润
 * </pre>
 * 坪效 = 销售额 ÷ 营业面积；人效 = 销售额 ÷ 员工数（所属门店为本店的启用用户）。列表与报表按用户的门店范围过滤
 */
@Service
@RequiredArgsConstructor
public class StorePnlService {

    /** 日期范围上限（天） */
    private static final long MAX_DAYS = 366;
    /** 月份网格的月数上限 */
    private static final long MAX_MONTHS = 24;

    private final ChainReportMapper reportMapper;
    private final StoreInfoService storeService;
    private final StoreLookup storeLookup;

    /** 按日期范围：每个门店一行（分摊按涉及的月份全额计入），最后一行为合计（storeId 为空） */
    public List<PnlRow> range(LocalDate begin, LocalDate end, Long regionId, Long storeId) {
        if (begin == null || end == null || end.isBefore(begin) || begin.plusDays(MAX_DAYS).isBefore(end)) {
            throw new BizException("chain.error.dateRangeTooLong", MAX_DAYS);
        }
        List<StoreInfo> stores = stores(regionId, storeId);
        Map<Long, Map<Integer, PnlRow>> data = compute(begin, end);
        Map<Long, Integer> staff = staffCounts();
        List<PnlRow> rows = new ArrayList<>();
        PnlRow total = new PnlRow();
        for (StoreInfo s : stores) {
            PnlRow r = newRow(s, null, staff);
            data.getOrDefault(s.getId(), Map.of()).values().forEach(m -> add(r, m));
            derive(r);
            rows.add(r);
            add(total, r);
        }
        derive(total);
        rows.add(total);
        return rows;
    }

    /**
     * 门店 × 月份：每个门店每个月一行（period 为 yyyy-MM），再加每个门店的合计行（period 为空）
     *
     * @param beginMonth yyyy-MM
     * @param endMonth   yyyy-MM
     */
    public List<PnlRow> grid(String beginMonth, String endMonth, Long regionId, Long storeId) {
        YearMonth from = parse(beginMonth);
        YearMonth to = parse(endMonth);
        if (to.isBefore(from) || ChronoUnit.MONTHS.between(from, to) >= MAX_MONTHS) {
            throw new BizException("chain.error.monthRangeTooLong", MAX_MONTHS);
        }
        List<StoreInfo> stores = stores(regionId, storeId);
        Map<Long, Map<Integer, PnlRow>> data = compute(from.atDay(1), to.atEndOfMonth());
        Map<Long, Integer> staff = staffCounts();
        List<PnlRow> rows = new ArrayList<>();
        for (StoreInfo s : stores) {
            PnlRow total = newRow(s, null, staff);
            for (YearMonth m = from; !m.isAfter(to); m = m.plusMonths(1)) {
                PnlRow r = newRow(s, m.toString(), staff);
                PnlRow v = data.getOrDefault(s.getId(), Map.of()).get(m.getYear() * 100 + m.getMonthValue());
                if (v != null) {
                    add(r, v);
                }
                derive(r);
                rows.add(r);
                add(total, r);
            }
            derive(total);
            rows.add(total);
        }
        return rows;
    }

    /** 穿透：一个门店在日期范围内的各月损益、费用与其他收入的类别明细 */
    public ChainViews.PnlDetail detail(Long storeId, LocalDate begin, LocalDate end) {
        ChainSupport.checkStore(storeId);
        if (begin == null || end == null || end.isBefore(begin) || begin.plusDays(MAX_DAYS).isBefore(end)) {
            throw new BizException("chain.error.dateRangeTooLong", MAX_DAYS);
        }
        StoreInfo s = storeService.get(storeId);
        Map<Integer, PnlRow> data = compute(begin, end).getOrDefault(storeId, Map.of());
        Map<Long, Integer> staff = staffCounts();
        List<PnlRow> months = new ArrayList<>();
        for (YearMonth m = YearMonth.from(begin); !m.isAfter(YearMonth.from(end)); m = m.plusMonths(1)) {
            PnlRow r = newRow(s, m.toString(), staff);
            PnlRow v = data.get(m.getYear() * 100 + m.getMonthValue());
            if (v != null) {
                add(r, v);
            }
            derive(r);
            months.add(r);
        }
        Long hq = storeLookup.headquarters().id();
        return new ChainViews.PnlDetail(months, reportMapper.selectIoByCategory(begin, end, "OUT", hq, storeId),
                reportMapper.selectIoByCategory(begin, end, "IN", hq, storeId));
    }

    /** 当月各门店的销售额（零售 + 批发净额），按销售额分摊总部费用时使用 */
    Map<Long, BigDecimal> monthSales(YearMonth month) {
        Map<Long, BigDecimal> sales = new HashMap<>();
        LocalDate begin = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        reportMapper.selectRetail(begin, end).forEach(a -> sales.merge(a.getStoreId(), Decimals.nz(a.getAmount()), BigDecimal::add));
        reportMapper.selectWholesale(begin, end).forEach(a -> sales.merge(a.getStoreId(), Decimals.nz(a.getAmount()), BigDecimal::add));
        sales.replaceAll((k, v) -> Decimals.money(v));
        return sales;
    }

    /** 当月总部的费用（待分摊的总部费用池）：结算账户属于总部或各门店共用的费用支出单 */
    BigDecimal hqExpense(YearMonth month) {
        Long hq = storeLookup.headquarters().id();
        return Decimals.money(reportMapper.selectIo(month.atDay(1), month.atEndOfMonth(), "OUT", hq).stream()
                .filter(a -> hq.equals(a.getStoreId())).map(a -> Decimals.nz(a.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    // ---------------------------------------------------------------- 内部

    private static YearMonth parse(String month) {
        try {
            return YearMonth.parse(month);
        } catch (RuntimeException e) {
            throw new BizException("chain.valid.periodFormat");
        }
    }

    /** 可管辖的门店（区域含下级、指定门店），按排序 */
    private List<StoreInfo> stores(Long regionId, Long storeId) {
        if (storeId != null) {
            return List.of(storeService.detail(storeId));
        }
        return storeService.page(1, 500, null, null, null, regionId).records();
    }

    private Map<Long, Integer> staffCounts() {
        Map<Long, Integer> map = new HashMap<>();
        reportMapper.selectStaffCounts().forEach(c -> map.put(c.getStoreId(), c.getCnt()));
        return map;
    }

    private static PnlRow newRow(StoreInfo s, String period, Map<Long, Integer> staff) {
        PnlRow r = new PnlRow();
        r.setStoreId(s.getId());
        r.setStoreCode(s.getCode());
        r.setStoreName(s.getName());
        r.setStoreType(s.getType());
        r.setRegionName(s.getRegionName());
        r.setArea(s.getArea());
        r.setStaffCount(staff.get(s.getId()));
        r.setPeriod(period);
        return r;
    }

    /** 门店 → 月份（年 × 100 + 月）→ 各项金额 */
    private Map<Long, Map<Integer, PnlRow>> compute(LocalDate begin, LocalDate end) {
        Map<Long, Map<Integer, PnlRow>> data = new HashMap<>();
        Long hq = storeLookup.headquarters().id();
        merge(data, reportMapper.selectRetail(begin, end), (r, v) -> r.setRetailSales(r.getRetailSales().add(v)));
        merge(data, reportMapper.selectRetailCost(begin, end), (r, v) -> r.setRetailCost(r.getRetailCost().add(v)));
        merge(data, reportMapper.selectWholesale(begin, end), (r, v) -> r.setWholesaleSales(r.getWholesaleSales().add(v)));
        merge(data, reportMapper.selectWholesaleCost(begin, end), (r, v) -> r.setWholesaleCost(r.getWholesaleCost().add(v)));
        merge(data, reportMapper.selectTransferIncome(begin, end), (r, v) -> r.setTransferIncome(r.getTransferIncome().add(v)));
        merge(data, reportMapper.selectTransferCost(begin, end), (r, v) -> r.setTransferCost(r.getTransferCost().add(v)));
        merge(data, reportMapper.selectIo(begin, end, "IN", hq), (r, v) -> r.setOtherIncome(r.getOtherIncome().add(v)));
        merge(data, reportMapper.selectIo(begin, end, "OUT", hq), (r, v) -> r.setExpense(r.getExpense().add(v)));
        // 分摊：门店为分摊到的金额，总部为分摊出去的合计（负数）
        List<ChainViews.StoreMonthAmount> allocated = reportMapper.selectAllocated(YearMonth.from(begin).toString(), YearMonth.from(end).toString());
        merge(data, allocated, (r, v) -> r.setAllocated(r.getAllocated().add(v)));
        Map<Integer, BigDecimal> out = new TreeMap<>();
        allocated.forEach(a -> out.merge(a.month(), Decimals.nz(a.getAmount()), BigDecimal::add));
        out.forEach((ym, v) -> {
            PnlRow r = data.computeIfAbsent(hq, k -> new HashMap<>()).computeIfAbsent(ym, k -> new PnlRow());
            r.setAllocated(r.getAllocated().subtract(v));
        });
        return data;
    }

    private static void merge(Map<Long, Map<Integer, PnlRow>> data, List<ChainViews.StoreMonthAmount> amounts, BiConsumer<PnlRow, BigDecimal> setter) {
        for (ChainViews.StoreMonthAmount a : amounts) {
            if (a.getStoreId() == null) {
                continue;
            }
            PnlRow r = data.computeIfAbsent(a.getStoreId(), k -> new HashMap<>()).computeIfAbsent(a.month(), k -> new PnlRow());
            setter.accept(r, Decimals.nz(a.getAmount()));
        }
    }

    private static void add(PnlRow to, PnlRow v) {
        to.setRetailSales(to.getRetailSales().add(v.getRetailSales()));
        to.setRetailCost(to.getRetailCost().add(v.getRetailCost()));
        to.setWholesaleSales(to.getWholesaleSales().add(v.getWholesaleSales()));
        to.setWholesaleCost(to.getWholesaleCost().add(v.getWholesaleCost()));
        to.setTransferIncome(to.getTransferIncome().add(v.getTransferIncome()));
        to.setTransferCost(to.getTransferCost().add(v.getTransferCost()));
        to.setOtherIncome(to.getOtherIncome().add(v.getOtherIncome()));
        to.setExpense(to.getExpense().add(v.getExpense()));
        to.setAllocated(to.getAllocated().add(v.getAllocated()));
        if (v.getArea() != null && to.getStoreId() == null) {
            to.setArea(Decimals.nz(to.getArea()).add(v.getArea()));
        }
        if (v.getStaffCount() != null && to.getStoreId() == null) {
            to.setStaffCount((to.getStaffCount() == null ? 0 : to.getStaffCount()) + v.getStaffCount());
        }
    }

    /** 舍入到分，计算毛利、经营利润、比率 */
    static void derive(PnlRow r) {
        r.setRetailSales(Decimals.money(r.getRetailSales()));
        r.setRetailCost(Decimals.money(r.getRetailCost()));
        r.setWholesaleSales(Decimals.money(r.getWholesaleSales()));
        r.setWholesaleCost(Decimals.money(r.getWholesaleCost()));
        r.setTransferIncome(Decimals.money(r.getTransferIncome()));
        r.setTransferCost(Decimals.money(r.getTransferCost()));
        r.setOtherIncome(Decimals.money(r.getOtherIncome()));
        r.setExpense(Decimals.money(r.getExpense()));
        r.setAllocated(Decimals.money(r.getAllocated()));
        r.setSales(r.getRetailSales().add(r.getWholesaleSales()));
        r.setCost(r.getRetailCost().add(r.getWholesaleCost()));
        r.setGrossProfit(r.getSales().subtract(r.getCost()));
        r.setGrossMargin(percent(r.getGrossProfit(), r.getSales()));
        r.setOperatingProfit(r.getGrossProfit().add(r.getTransferIncome()).subtract(r.getTransferCost()).add(r.getOtherIncome())
                .subtract(r.getExpense()).subtract(r.getAllocated()));
        r.setOperatingMargin(percent(r.getOperatingProfit(), r.getSales()));
        r.setSalesPerArea(r.getArea() == null || r.getArea().signum() <= 0 ? null : r.getSales().divide(r.getArea(), 2, RoundingMode.HALF_UP));
        r.setSalesPerStaff(r.getStaffCount() == null || r.getStaffCount() <= 0 ? null
                : r.getSales().divide(BigDecimal.valueOf(r.getStaffCount()), 2, RoundingMode.HALF_UP));
    }

    private static BigDecimal percent(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() == 0) {
            return null;
        }
        return part.multiply(Decimals.HUNDRED).divide(whole, 2, RoundingMode.HALF_UP);
    }

    /** 门店类型是否参与总部费用分摊 */
    static boolean allocatable(StoreType type, boolean includeDc, boolean includeFranchise) {
        return type == StoreType.DIRECT || includeDc && type == StoreType.DC || includeFranchise && type == StoreType.FRANCHISE;
    }
}
