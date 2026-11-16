package com.lemon.erp.modules.dist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.dist.dto.DistQuery;
import com.lemon.erp.modules.dist.dto.DistViews;
import com.lemon.erp.modules.dist.entity.DistStoreLedger;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DistStoreLedgerMapper extends BaseMapper<DistStoreLedger> {

    /** 明细（storeId：调出或调入为该门店） */
    IPage<DistStoreLedger> selectPageList(IPage<DistStoreLedger> page, @Param("q") DistQuery q);

    /** 按 调出门店 → 调入门店 汇总 */
    List<DistViews.LedgerSummaryRow> selectSummary(@Param("q") DistQuery q);
}
