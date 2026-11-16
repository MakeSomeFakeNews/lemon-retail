package com.lemon.erp.modules.pos.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 充值赠送档位（充 X 送 Y）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pos_topup_tier")
public class PosTopupTier extends BaseEntity {

    private BigDecimal minAmount;

    private BigDecimal bonusAmount;
}
