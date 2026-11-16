package com.lemon.erp.modules.pos.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.pos.entity.PosPayment;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PosPaymentMapper extends BaseMapper<PosPayment> {

    /** 单据的收款 / 退款（含结算账户名称），按 ID */
    List<PosPayment> selectBySaleId(@Param("saleId") Long saleId);

    /**
     * 结算时关联已收款的聚合支付：只关联成功、未关联其他单据的，返回更新行数
     */
    int bindSale(@Param("id") Long id, @Param("saleId") Long saleId, @Param("accountId") Long accountId);

    /** 原收款累加已退金额，可退金额不足时不更新（返回 0） */
    int addRefunded(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /** 行锁（聚合支付状态变更与结算互斥） */
    PosPayment selectForUpdate(@Param("id") Long id);
}
