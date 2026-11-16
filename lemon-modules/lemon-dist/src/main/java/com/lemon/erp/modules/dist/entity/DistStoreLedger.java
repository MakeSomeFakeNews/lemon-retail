package com.lemon.erp.modules.dist.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 门店往来：跨门店差价调拨的内部结算（调出门店应收、调入门店应付），只追加（冲销记一笔负数）
 */
@Data
@TableName("dist_store_ledger")
public class DistStoreLedger {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long transferId;
    private String transferNo;
    private String sourceNo;
    private LocalDate bizDate;
    private Long fromStoreId;
    private Long toStoreId;
    private Long fromWarehouseId;
    private Long toWarehouseId;
    private BigDecimal costAmount;
    private BigDecimal amount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String fromStoreName;
    @TableField(exist = false)
    private String toStoreName;
    @TableField(exist = false)
    private String fromWarehouseName;
    @TableField(exist = false)
    private String toWarehouseName;
}
