package com.lemon.erp.modules.pos.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.pos.model.PayMethod;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 门店收款账户：支付方式 → 结算账户
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pos_pay_account")
public class PosPayAccount extends BaseEntity {

    private Long storeId;

    private PayMethod payMethod;

    private Long accountId;
}
