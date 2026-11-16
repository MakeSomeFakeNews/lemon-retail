package com.lemon.erp.modules.dist.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 查询结果（mapper 直接映射）
 */
public final class DistViews {

    private DistViews() {
    }

    /** 要货建议：收货仓库的下限、现存、在途，供货仓库的可用数量，建议量 = 下限 − 现存 − 在途（不小于 0） */
    @Data
    public static class SuggestRow {
        private Long materialId;
        /** 仓库下限（分仓库预警时的设置），没有时取物料的安全库存 */
        private BigDecimal minQty;
        private BigDecimal stockQty;
        private BigDecimal transitQty;
        private BigDecimal supplyAvailable;
        private BigDecimal suggestQty;
    }

    /** 要货汇总：已审核未配送的要货按 供货仓库 + 物料 汇总 */
    @Data
    public static class SummaryRow {
        private Long supplyWarehouseId;
        private String supplyWarehouseName;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private String spec;
        private String unit;
        /** 涉及的要货单数、门店数 */
        private Integer requisitionCount;
        private Integer storeCount;
        private BigDecimal approvedQty;
        private BigDecimal deliveredQty;
        /** 待配送 = 批准 − 已配送 */
        private BigDecimal pendingQty;
        /** 供货仓库可用数量（现存 − 冻结） */
        private BigDecimal available;
        /** 缺货 = 待配送 − 可用（不小于 0） */
        private BigDecimal shortage;
        private Long supplierId;
        private String supplierName;
    }

    /** 在途：已调出未收货的调拨单（含配送、退仓、差异调拨和手工的在途调拨） */
    @Data
    public static class TransitRow {
        private Long transferId;
        private String transferNo;
        private String sourceNo;
        private LocalDate bizDate;
        private Long fromWarehouseId;
        private String fromWarehouseName;
        private Long fromStoreId;
        private String fromStoreName;
        private Long toWarehouseId;
        private String toWarehouseName;
        private Long toStoreId;
        private String toStoreName;
        private BigDecimal totalQty;
        private BigDecimal transferAmount;
        /** 在途天数（当天 − 调出日期） */
        private Integer days;
    }

    /** 门店往来汇总：调出门店 → 调入门店 */
    @Data
    public static class LedgerSummaryRow {
        private Long fromStoreId;
        private String fromStoreName;
        private Long toStoreId;
        private String toStoreName;
        private Integer entryCount;
        private BigDecimal costAmount;
        private BigDecimal amount;
    }

    /** 门店对比 */
    @Data
    public static class StoreCompareRow {
        private Long storeId;
        private String storeCode;
        private String storeName;
        private String storeType;
        private String regionName;
        /** 销售额（不含税）、销售退货额 */
        private BigDecimal salesAmount;
        private BigDecimal returnAmount;
        /** 其中 POS 零售：零售出库、零售退货（含作废退回）的不含税金额 */
        private BigDecimal retailAmount;
        private BigDecimal retailReturnAmount;
        /** 销售净额 = 销售额 − 退货额 */
        private BigDecimal netSales;
        /** 销售成本（库存流水的实际出库成本，退货冲减） */
        private BigDecimal cost;
        private BigDecimal grossProfit;
        /** 毛利率 % */
        private BigDecimal grossMargin;
        /** 采购入库净额（不含税，扣采购退货） */
        private BigDecimal purchaseAmount;
        /** 配送入库金额（收货日期在范围内的配送单，按配送价） */
        private BigDecimal deliveryAmount;
        /** 当前库存金额（数量 × 实时移动加权均价） */
        private BigDecimal stockAmount;
        /** 库存周转（次）= 销售成本 ÷ 库存金额 */
        private BigDecimal turnover;
        /** 要货批准、已配送数量（要货日期在范围内） */
        private BigDecimal approvedQty;
        private BigDecimal deliveredQty;
        /** 要货满足率 % = 已配送 ÷ 批准 */
        private BigDecimal fillRate;
        /** 配送单数（关联有期望到货日期的要货）、按时到货的单数 */
        private Integer deliveryCount;
        private Integer onTimeCount;
        /** 配送及时率 % */
        private BigDecimal onTimeRate;
    }

    /** 调拨单明细的调出成本价、调拨价、调拨金额 */
    @Data
    public static class TransferLine {
        private Long id;
        private Long materialId;
        private BigDecimal qty;
        private BigDecimal costPrice;
        private BigDecimal transferPrice;
        private BigDecimal transferAmount;
    }

    /** 在途合计 */
    @Data
    public static class TransitTotal {
        private Integer docCount;
        private BigDecimal totalQty;
        private BigDecimal transferAmount;
    }
}
