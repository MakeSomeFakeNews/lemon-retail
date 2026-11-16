package com.lemon.erp.modules.pos.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 储值充值单：收款记入 pos_payment（topup_id），余额变动记入储值流水（充值、赠送各一条）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pos_topup")
public class PosTopup extends BaseEntity {

    private String topupNo;

    private Long storeId;

    private Long terminalId;

    private Long shiftId;

    private Long memberId;

    private LocalDate businessDate;

    private BigDecimal amount;

    private BigDecimal bonusAmount;

    private BigDecimal balanceAfter;

    private String payMethods;

    private String remark;

    private Long deptId;

    private Long createBy;

    @TableField(exist = false)
    private String storeName;

    @TableField(exist = false)
    private String terminalName;

    @TableField(exist = false)
    private String memberCode;

    @TableField(exist = false)
    private String memberName;

    @TableField(exist = false)
    private String memberPhone;

    @TableField(exist = false)
    private String createByName;

    @TableField(exist = false)
    private List<PosPayment> payments;
}
