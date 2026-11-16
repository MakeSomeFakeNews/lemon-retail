package com.lemon.erp.modules.dist.mapper;

import com.lemon.erp.modules.dist.dto.DistViews;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * 门店对比报表：只读汇总供应链单据、库存流水、库存与本模块的要货 / 配送（按仓库所属门店）
 */
public interface DistReportMapper {

    /**
     * @param storeIds 限定的门店（区域筛选或用户的门店范围），为空不限
     */
    List<DistViews.StoreCompareRow> selectStoreCompare(@Param("begin") LocalDate begin, @Param("end") LocalDate end,
                                                       @Param("storeIds") Collection<Long> storeIds);
}
