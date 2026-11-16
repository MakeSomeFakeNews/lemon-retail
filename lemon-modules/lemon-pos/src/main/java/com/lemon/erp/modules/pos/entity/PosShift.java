package com.lemon.erp.modules.pos.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.pos.model.ShiftStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 班次：开班（备用金）→ 收银 → 交班（系统金额与实点现金对比）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pos_shift")
public class PosShift extends BaseEntity {

    private String shiftNo;

    private Long storeId;

    private Long terminalId;

    private Long cashierId;

    private LocalDate businessDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime openTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime closeTime;

    private BigDecimal openingCash;

    private ShiftStatus status;

    private Integer saleCount;

    private BigDecimal saleAmount;

    private Integer returnCount;

    private BigDecimal returnAmount;

    private BigDecimal cashAmount;

    private BigDecimal expectedCash;

    private BigDecimal countedCash;

    private BigDecimal cashDiff;

    private Long closedBy;

    private String remark;

    @TableField(exist = false)
    private String storeName;

    @TableField(exist = false)
    private String terminalName;

    @TableField(exist = false)
    private String cashierName;

    @TableField(exist = false)
    private String closedByName;
}
