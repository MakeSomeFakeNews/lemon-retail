package com.lemon.erp.modules.bi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 订单全链路追溯：出入库单、应收应付单、收付款。没有对应查看权限的部分不返回
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderTrace(List<TradeDoc> tradeDocs, List<Bill> bills, List<Settlement> settlements) {

    /** 以下为数据库查询结果，为空的列不输出 */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TradeDoc {
        private Long id;
        private String docNo;
        private String kind;
        private LocalDate bizDate;
        private BigDecimal totalQty;
        private BigDecimal totalAmount;
        private String wmsDocNo;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Bill {
        private Long id;
        private String billNo;
        private String kind;
        private String sourceNo;
        private LocalDate billDate;
        private BigDecimal amount;
        private BigDecimal settledAmount;
        private String status;
        private Long voucherId;
        private String voucherNo;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Settlement {
        private Long id;
        private String settleNo;
        private String kind;
        private LocalDate settleDate;
        private BigDecimal amount;
        private String status;
        private Long voucherId;
        private String voucherNo;
    }
}
