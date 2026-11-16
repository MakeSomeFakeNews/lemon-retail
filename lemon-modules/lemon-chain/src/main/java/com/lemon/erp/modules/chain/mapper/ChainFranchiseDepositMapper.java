package com.lemon.erp.modules.chain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.chain.entity.ChainFranchiseDeposit;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;

public interface ChainFranchiseDepositMapper extends BaseMapper<ChainFranchiseDeposit> {

    IPage<ChainFranchiseDeposit> selectPageList(IPage<ChainFranchiseDeposit> page, @Param("storeId") Long storeId,
                                                @Param("kind") String kind, @Param("begin") LocalDate begin, @Param("end") LocalDate end,
                                                @Param("storeIds") Collection<Long> storeIds);

    /** 保证金余额 = 收取 − 退还 */
    BigDecimal selectBalance(@Param("storeId") Long storeId);
}
