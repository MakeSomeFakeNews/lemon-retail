package com.lemon.erp.modules.pos.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.pos.model.SaleKind;
import com.lemon.erp.modules.pos.model.SaleStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * POS 销售单 / 退货单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pos_sale")
public class PosSale extends BaseEntity {

    private String saleNo;

    private SaleKind kind;

    private SaleStatus status;

    private Long storeId;

    private Long terminalId;

    private Long shiftId;

    private Long warehouseId;

    private Long memberId;

    private Long origSaleId;

    private LocalDate businessDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime saleTime;

    private BigDecimal totalQty;

    private BigDecimal listAmount;

    private BigDecimal amount;

    private BigDecimal orderDiscount;

    private BigDecimal roundOff;

    private BigDecimal payableAmount;

    private BigDecimal paidAmount;

    private Long promotionId;

    private BigDecimal promoAmount;

    private BigDecimal pointsEarned;

    private BigDecimal changeAmount;

    private Long wmsDocId;

    private String wmsDocNo;

    private String remark;

    private Long voidBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime voidTime;

    private String voidReason;

    private Long voidDocId;

    private Long deptId;

    private Long createBy;

    // ---------------- 显示

    @TableField(exist = false)
    private String storeName;

    @TableField(exist = false)
    private String terminalName;

    @TableField(exist = false)
    private String shiftNo;

    @TableField(exist = false)
    private String cashierName;

    @TableField(exist = false)
    private String memberCode;

    @TableField(exist = false)
    private String memberName;

    @TableField(exist = false)
    private String memberPhone;

    @TableField(exist = false)
    private String origSaleNo;

    @TableField(exist = false)
    private String voidByName;

    @TableField(exist = false)
    private String promotionName;

    /** 支付方式摘要（列表），如 现金、微信 */
    @TableField(exist = false)
    private String payMethods;

    @TableField(exist = false)
    private List<PosSaleItem> items;

    @TableField(exist = false)
    private List<PosPayment> payments;
}
