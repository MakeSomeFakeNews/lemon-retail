package com.lemon.erp.modules.bi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lemon.erp.modules.wms.model.StockBizType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据大屏的返回结构：每个大屏一个 record（都带 key、title、time），统计查询的结果用带 setter 的类
 * （MyBatis 按列名映射，为空的列不输出，与原先返回 Map 时一致）
 */
public final class BiScreen {

    private BiScreen() {
    }

    /** 大屏数据 */
    public sealed interface ScreenData permits Warehouse, Sales {
        String key();

        String title();

        /** 生成时间（ISO 格式，不含毫秒） */
        String time();
    }

    // ---------------------------------------------------------------- 仓储物流

    public record Warehouse(String key, String title, String time, WarehouseKpi kpi, List<StockDay> trend,
                            List<StockDocType> byType, List<WarehouseStock> warehouses, List<LowStock> lowStock,
                            List<ExpiringBatch> expiring, List<PendingDelivery> deliveries, List<RecentStockDoc> docs) implements ScreenData {
    }

    public record WarehouseKpi(Long todayInDocs, BigDecimal todayInQty, Long todayOutDocs, BigDecimal todayOutQty,
                               Long pendingPick, Long pendingShip, Long shippedToday, int transit, Long skus,
                               BigDecimal stockQty, int lowStock, int expiring) {
    }

    /** 已完成的出入库单合计 */
    @Data
    public static class StockDocSum {
        private StockBizType kind;
        private Long docs;
        private BigDecimal qty;
    }

    /** 每日出入库数量（数据库按出入库分行） */
    @Data
    public static class StockDocDay {
        private LocalDate day;
        private StockBizType kind;
        private BigDecimal qty;
    }

    /** 每日出入库数量（入库、出库合并为一行，没有的日期补 0） */
    @Data
    public static class StockDay {
        private LocalDate day;
        private BigDecimal inQty;
        private BigDecimal outQty;
    }

    /** 按出入库类型 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StockDocType {
        private StockBizType kind;
        private String label;
        private BigDecimal qty;
        private String value;
    }

    /** 各仓库库存 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WarehouseStock {
        private String name;
        private BigDecimal qty;
        private Long skus;
    }

    /** 库存概况 */
    @Data
    public static class StockOverview {
        private Long skus;
        private BigDecimal qty;
    }

    /** 低于安全库存 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LowStock {
        private String code;
        private String name;
        private String unit;
        private BigDecimal qty;
        private BigDecimal safety;
    }

    /** 临期 / 过期批次 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExpiringBatch {
        private String code;
        private String name;
        private String batchNo;
        private LocalDate expiryDate;
        private BigDecimal qty;
    }

    /** 待发货的发货通知 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PendingDelivery {
        private String deliveryNo;
        private String customer;
        private String warehouse;
        private LocalDate shipDate;
        private String status;
        private String statusLabel;
    }

    /** 发货通知数量：待拣货、待发货、当日已发货 */
    @Data
    public static class DeliveryCounts {
        private Long pending;
        private Long picked;
        private Long shipped;
    }

    /** 最近完成的出入库单 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RecentStockDoc {
        private String docNo;
        private StockBizType kind;
        private String typeLabel;
        private String warehouse;
        private BigDecimal qty;
        private LocalDateTime time;
        private String bizType;
    }

    // ---------------------------------------------------------------- 销售经营

    public record Sales(String key, String title, String time, SalesKpi kpi, List<SalesMonth> monthly,
                        List<NameAmount> topCustomers, List<NameAmount> topProducts, List<NameAmount> salesmen,
                        List<OrderStatus> orderStatus, List<RecentOrder> orders) implements ScreenData {
    }

    public record SalesKpi(BigDecimal todayOrderAmount, Long todayOrders, BigDecimal monthOrderAmount, BigDecimal yearOrderAmount,
                           BigDecimal todayShipped, BigDecimal monthShipped, BigDecimal yearShipped, BigDecimal lastYearShipped,
                           BigDecimal monthReceipt, BigDecimal yearReceipt, BigDecimal receivable, int overdueOrders,
                           int newCustomers) {
    }

    /** 本年各月：订单金额、出库金额（未到的月份为空）、上年同月出库金额 */
    public record SalesMonth(int month, BigDecimal orders, BigDecimal shipped, BigDecimal lastYear) {
    }

    /** 已审核的销售订单合计 */
    @Data
    public static class OrderSum {
        private BigDecimal amount;
        private Long orders;
    }

    /** 按月金额 */
    @Data
    public static class PeriodAmount {
        private Integer period;
        private BigDecimal amount;
    }

    /** 排行：客户（金额）、产品（金额、数量）、业务员（金额、订单数） */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NameAmount {
        private String name;
        private BigDecimal amount;
        private BigDecimal qty;
        private Long orders;
    }

    /** 执行中的销售订单按状态 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderStatus {
        private String status;
        private String label;
        private Long orders;
        private BigDecimal amount;
    }

    /** 最新销售订单 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RecentOrder {
        private String orderNo;
        private String customer;
        private BigDecimal amount;
        private String status;
        private String statusLabel;
        private LocalDate orderDate;
    }
}
