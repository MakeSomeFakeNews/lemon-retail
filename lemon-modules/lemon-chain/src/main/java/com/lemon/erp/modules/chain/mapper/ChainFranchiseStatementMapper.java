package com.lemon.erp.modules.chain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.chain.entity.ChainFranchiseStatement;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;

public interface ChainFranchiseStatementMapper extends BaseMapper<ChainFranchiseStatement> {

    IPage<ChainFranchiseStatement> selectPageList(IPage<ChainFranchiseStatement> page, @Param("storeId") Long storeId,
                                                  @Param("period") String period, @Param("status") String status,
                                                  @Param("storeIds") Collection<Long> storeIds);

    ChainFranchiseStatement selectDetail(@Param("id") Long id);
}
