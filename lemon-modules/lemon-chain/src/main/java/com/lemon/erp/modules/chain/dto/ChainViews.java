package com.lemon.erp.modules.chain.dto;

import com.lemon.erp.framework.store.StoreType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 连锁经营的查询结果
 */
public final class ChainViews {

    private ChainViews() {
    }

    /** 按门店 + 月份汇总的金额（门店损益的各项取数） */
    @Data
    public static class StoreMonthAmount {
        private Long storeId;
        /** 年 × 100 + 月 */
        private Integer ym;
        /** 月份 yyyy-MM（分摊记录），按它换算 ym */
        private String ymText;
        private BigDecimal amount;

        /** 年 × 100 + 月 */
        public int month() {
            if (ym != null) {
                return ym;
            }
            return Integer.parseInt(ymText.substring(0, 4)) * 100 + Integer.parseInt(ymText.substring(5, 7));
        }
    }

    /** 调拨单明细（加盟退回按配送价计算红字应收） */
    @Data
    public static class TransferLine {
        private Long materialId;
        private BigDecimal qty;
        private BigDecimal transferAmount;
    }

    /**
     * 门店损益一行（一个门店一个月，或一个门店一段日期；合计行 storeId 为空）。金额为本位币、不含税：
     * 毛利 = 零售净额 + 批发净额 − 销售成本；经营利润 = 毛利 + 内部调拨收入 − 内部调拨成本 + 其他收入 − 门店费用 − 分摊总部费用
     */
    @Data
    public static class PnlRow {
        private Long storeId;
        private String storeCode;
        private String storeName;
        private StoreType storeType;
        private String regionName;
        private BigDecimal area;
        private Integer staffCount;
        /** 月份 yyyy-MM，按日期范围汇总时为空 */
        private String period;
        private BigDecimal retailSales = BigDecimal.ZERO;
        private BigDecimal retailCost = BigDecimal.ZERO;
        private BigDecimal wholesaleSales = BigDecimal.ZERO;
        private BigDecimal wholesaleCost = BigDecimal.ZERO;
        private BigDecimal sales = BigDecimal.ZERO;
        private BigDecimal cost = BigDecimal.ZERO;
        private BigDecimal grossProfit = BigDecimal.ZERO;
        private BigDecimal grossMargin;
        private BigDecimal transferIncome = BigDecimal.ZERO;
        private BigDecimal transferCost = BigDecimal.ZERO;
        private BigDecimal otherIncome = BigDecimal.ZERO;
        private BigDecimal expense = BigDecimal.ZERO;
        /** 分摊的总部费用；总部为负数（已分摊出去的部分） */
        private BigDecimal allocated = BigDecimal.ZERO;
        private BigDecimal operatingProfit = BigDecimal.ZERO;
        private BigDecimal operatingMargin;
        /** 坪效 = 销售额 ÷ 面积 */
        private BigDecimal salesPerArea;
        /** 人效 = 销售额 ÷ 员工数（用户的所属门店，启用） */
        private BigDecimal salesPerStaff;
    }

    /** 门店损益的费用明细：按收支类别 */
    @Data
    public static class ExpenseRow {
        private Long categoryId;
        private String categoryName;
        private Integer docCount;
        private BigDecimal amount;
    }

    /** 门店损益穿透：一个门店在日期范围内的各月、费用类别 */
    public record PnlDetail(List<PnlRow> months, List<ExpenseRow> expenses, List<ExpenseRow> incomes) {
    }

    /** 门店员工数 */
    @Data
    public static class StoreCount {
        private Long storeId;
        private Integer cnt;
    }

    /**
     * 门店补货建议一行（仓库 × 物料）：日均销量 = 近 N 天销售净数量（POS 零售 + 销售出库，扣退货）÷ N，
     * 建议量 = 日均 × (到货天数 + 安全天数) − 现存 − 在途 − 未完成的要货（向上取整，不足 0 不建议）
     */
    @Data
    public static class ReplenishRow {
        private Long storeId;
        private String storeName;
        private StoreType storeType;
        private Long warehouseId;
        private String warehouseName;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private String spec;
        private String unit;
        private BigDecimal soldQty;
        private BigDecimal avgDaily;
        private BigDecimal stockQty;
        private BigDecimal transitQty;
        private BigDecimal requisitionQty;
        private Integer leadDays;
        private Integer safetyDays;
        private BigDecimal targetQty;
        private BigDecimal suggestQty;
    }

    /** 补货参数（门店的设置与默认值） */
    public record ReplenishParams(Long storeId, int salesDays, int leadDays, int safetyDays, Integer storeLeadDays, Integer storeSafetyDays) {
    }

    /** 加盟结算汇总一行（一个加盟店） */
    @Data
    public static class FranchiseSummaryRow {
        private Long storeId;
        private String storeCode;
        private String storeName;
        private String franchisee;
        private String settleMode;
        private BigDecimal opening = BigDecimal.ZERO;
        private BigDecimal deliveryAmount = BigDecimal.ZERO;
        private BigDecimal returnAmount = BigDecimal.ZERO;
        private BigDecimal paymentAmount = BigDecimal.ZERO;
        private BigDecimal otherAmount = BigDecimal.ZERO;
        private BigDecimal closing = BigDecimal.ZERO;
        private BigDecimal depositBalance = BigDecimal.ZERO;
    }

    /** 店长看板（今天与昨天对比） */
    @Data
    public static class StoreDashboard {
        private Long storeId;
        private String storeName;
        private LocalDate date;
        private DayFigures today;
        private DayFigures yesterday;
        private List<TopItem> topItems = new ArrayList<>();
        /** 待收货的配送单数、草稿 / 被驳回的要货单数 */
        private Integer pendingReceive;
        private Integer draftRequisitions;
    }

    /** 一天的 POS 数据：净销售额（含税实收）、单数、客单价、毛利（不含税净额 − 成本，有成本权限时） */
    @Data
    public static class DayFigures {
        private BigDecimal sales = BigDecimal.ZERO;
        private Integer orders = 0;
        private Integer returns = 0;
        private BigDecimal avgTicket;
        private BigDecimal netSales = BigDecimal.ZERO;
        private BigDecimal cost;
        private BigDecimal grossProfit;
    }

    /** 畅销商品 */
    @Data
    public static class TopItem {
        private Long materialId;
        private String materialCode;
        private String materialName;
        private String unit;
        private BigDecimal qty;
        private BigDecimal amount;
    }
}
