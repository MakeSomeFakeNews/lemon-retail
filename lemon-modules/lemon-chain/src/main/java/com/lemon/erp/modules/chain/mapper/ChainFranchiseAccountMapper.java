package com.lemon.erp.modules.chain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.chain.entity.ChainFranchiseAccount;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;

public interface ChainFranchiseAccountMapper extends BaseMapper<ChainFranchiseAccount> {

    /**
     * 加盟店（含还没有账户的）及其账户、应收 / 预收余额、保证金余额
     *
     * @param storeIds 门店范围，为空不限
     */
    IPage<ChainFranchiseAccount> selectPageList(IPage<ChainFranchiseAccount> page, @Param("keyword") String keyword,
                                                @Param("storeId") Long storeId, @Param("settleMode") String settleMode,
                                                @Param("storeIds") Collection<Long> storeIds);

    /** 一个加盟店的账户与余额（没有账户时只有门店资料） */
    ChainFranchiseAccount selectByStore(@Param("storeId") Long storeId);
}
