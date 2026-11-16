package com.lemon.erp.modules.pos.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.pos.model.DailyCloseStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 门店日结
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pos_daily_close")
public class PosDailyClose extends BaseEntity {

    private Long storeId;

    private LocalDate businessDate;

    private DailyCloseStatus status;

    private Integer shiftCount;

    private Integer saleCount;

    private BigDecimal saleAmount;

    private Integer returnCount;

    private BigDecimal returnAmount;

    private BigDecimal netAmount;

    private Integer topupCount;

    private BigDecimal topupAmount;

    private BigDecimal bonusAmount;

    private BigDecimal storedValueAmount;

    private BigDecimal pointsAmount;

    private Long voucherId;

    private BigDecimal expectedCash;

    private BigDecimal countedCash;

    private BigDecimal cashDiff;

    private Long closedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime closedTime;

    private Long reopenedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reopenedTime;

    private String remark;

    @TableField(exist = false)
    private String storeName;

    @TableField(exist = false)
    private String closedByName;

    @TableField(exist = false)
    private String reopenedByName;

    @TableField(exist = false)
    private List<PosDailyClosePay> pays;
}
