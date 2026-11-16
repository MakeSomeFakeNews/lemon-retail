package com.lemon.erp.modules.pos.dto;

import com.lemon.erp.modules.pos.entity.PosMember;
import com.lemon.erp.modules.pos.entity.PosShift;
import com.lemon.erp.modules.pos.entity.PosTerminal;
import com.lemon.erp.modules.pos.model.PayMethod;
import com.lemon.erp.modules.pos.model.PayProviderType;
import com.lemon.erp.modules.pos.model.PosPriceSource;
import com.lemon.erp.modules.pos.model.PosRounding;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 零售前台的返回结构
 */
public final class PosViews {

    private PosViews() {
    }

    /**
     * 收银台进入收银时的上下文：收银台、门店、当班班次、可用的支付方式与通道、收银相关的权限
     *
     * @param perms 当前用户在收银台可用的操作（改价、折扣、超限折扣、退货、无原单退货、作废）
     */
    public record Context(PosTerminal terminal, String storeName, PosShift shift, PayProviderType provider,
                          List<PayMethod> payMethods, BigDecimal maxDiscountRate, PosRounding rounding, List<String> perms) {
    }

    /**
     * 扫码 / 输入编码识别的商品（一次返回物料、单位、价格、本收银台仓库的库存）
     *
     * @param unit        识别出的单位（包装条码为辅助单位），价格均为该单位的价格
     * @param ratio       1 单位 = ratio × 基本单位
     * @param listPrice   原价（门店价 / 总部零售价）
     * @param price       取价结果（不含会员时的价格；带 memberId 时含会员价）
     * @param limitQty    特价每单限购（基本单位），为空不限
     * @param minPrice    最低售价（基本单位，价格控制），为空不控制
     * @param stockQty    本收银台仓库的可用库存（基本单位）
     */
    public record Lookup(Long materialId, String code, String name, String spec, String unit, String baseUnit, BigDecimal ratio,
                         String barcode, String imageUrl, boolean batchEnabled, boolean serialEnabled,
                         BigDecimal listPrice, BigDecimal price, PosPriceSource priceSource, Long specialId, BigDecimal limitQty,
                         BigDecimal minPrice, boolean minPriceBlock, BigDecimal stockQty) {
    }

    /** 试算 / 结算后的行（价格为录入单位的含税价） */
    @Data
    public static class PricedLine {
        private int lineNo;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private String spec;
        private String unit;
        private BigDecimal unitRatio;
        private BigDecimal qty;
        private BigDecimal baseQty;
        private BigDecimal listPrice;
        private BigDecimal price;
        private PosPriceSource priceSource;
        private Long specialId;
        private BigDecimal limitQty;
        private BigDecimal discountRate;
        private BigDecimal salePrice;
        private BigDecimal amount;
        private BigDecimal orderShare;
        private BigDecimal finalAmount;
        /** 低于最低售价（控制方式为提示时只提示） */
        private boolean belowMin;
        private String serialNos;
        private String remark;
    }

    /**
     * 试算结果
     *
     * @param manualDiscountRate 手工优惠（改价、单行折扣、整单优惠）占取价金额的百分比，与折扣上限比较
     */
    public record Preview(List<PricedLine> lines, BigDecimal totalQty, BigDecimal listAmount, BigDecimal amount, BigDecimal orderDiscount,
                          BigDecimal roundOff, BigDecimal payable, BigDecimal manualDiscountRate, PosMember member) {
    }

    /** 班次的收款按支付方式汇总 */
    @Data
    public static class PayRow {
        private PayMethod payMethod;
        private Long accountId;
        private String accountName;
        private BigDecimal receiptAmount;
        private BigDecimal refundAmount;
        private BigDecimal amount;
        private Integer payCount;
    }

    /** 班次 / 日结的单据汇总 */
    @Data
    public static class SaleSummary {
        private Integer saleCount;
        private BigDecimal saleAmount;
        private Integer returnCount;
        private BigDecimal returnAmount;
        private Integer voidCount;
    }

    /**
     * 交班单：系统按支付方式汇总的金额与应有现金
     *
     * @param expectedCash 应有现金 = 备用金 + 现金净收款
     * @param unsettled    已收款未结算的聚合支付笔数（需要先结算或退款才能交班）
     */
    public record ShiftReport(PosShift shift, List<PayRow> pays, SaleSummary sales, BigDecimal cashAmount, BigDecimal expectedCash,
                              int unsettled) {
    }

    /**
     * 门店日结试算：openShifts 为还没有交班的班次数（有时不能日结）
     */
    public record DailySummary(Long storeId, String storeName, LocalDate businessDate, SaleSummary sales, List<PayRow> pays,
                               BigDecimal netAmount, int shiftCount, int openShifts, BigDecimal expectedCash, BigDecimal countedCash,
                               BigDecimal cashDiff, boolean closed) {
    }

    /** 收银日报的一行：支付方式金额按 PayMethod 名称 */
    @Data
    public static class CashierRow {
        private String groupKey;
        private String groupName;
        private Integer saleCount;
        private BigDecimal saleAmount;
        private Integer returnCount;
        private BigDecimal returnAmount;
        private BigDecimal netAmount;
        private Map<String, BigDecimal> pays;
        private BigDecimal payTotal;
    }

    public record CashierReport(List<CashierRow> rows, CashierRow total) {
    }

    /** 收银日报的原始汇总行（mapper 结果） */
    @Data
    public static class ReportSaleRow {
        private String groupKey;
        private String groupName;
        private Integer saleCount;
        private BigDecimal saleAmount;
        private Integer returnCount;
        private BigDecimal returnAmount;
    }

    @Data
    public static class ReportPayRow {
        private String groupKey;
        private String groupName;
        private PayMethod payMethod;
        private BigDecimal amount;
    }

    /** POS 销售明细（按行） */
    @Data
    public static class DetailRow {
        private Long saleId;
        private String saleNo;
        private String kind;
        private String status;
        private LocalDate businessDate;
        private String saleTime;
        private String storeName;
        private String terminalName;
        private String cashierName;
        private String memberCode;
        private String memberName;
        private String materialCode;
        private String materialName;
        private String spec;
        private String unit;
        private BigDecimal qty;
        private BigDecimal listPrice;
        private BigDecimal salePrice;
        private BigDecimal amount;
        private BigDecimal finalAmount;
        private String priceSource;
    }

    /** 支付通道设置：密钥只返回是否已设置 */
    public record PayConfig(PayProviderType provider, String appId, String merchantNo, boolean hasApiKey, boolean hasApiSecret,
                            String notifyUrl, String remark, List<PayProviderType> providers) {
    }
}
