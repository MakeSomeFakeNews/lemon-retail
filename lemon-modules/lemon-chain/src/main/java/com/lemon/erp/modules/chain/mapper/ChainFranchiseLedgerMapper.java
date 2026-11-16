package com.lemon.erp.modules.chain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.chain.dto.ChainViews;
import com.lemon.erp.modules.chain.entity.ChainFranchiseLedger;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ChainFranchiseLedgerMapper extends BaseMapper<ChainFranchiseLedger> {

    IPage<ChainFranchiseLedger> selectPageList(IPage<ChainFranchiseLedger> page, @Param("storeId") Long storeId,
                                               @Param("begin") LocalDate begin, @Param("end") LocalDate end,
                                               @Param("keyword") String keyword, @Param("storeIds") Collection<Long> storeIds);

    /** 调拨单明细（只读关联仓库的出入库单明细）：加盟退回按配送价计算红字应收 */
    List<ChainViews.TransferLine> selectTransferLines(@Param("transferId") Long transferId);
}
