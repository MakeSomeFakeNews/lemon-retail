package com.lemon.erp.modules.pos.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.dto.PosViews;
import com.lemon.erp.modules.pos.entity.PosShift;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface PosShiftMapper extends BaseMapper<PosShift> {

    IPage<PosShift> selectPageList(IPage<PosShift> page, @Param("q") PosQueries.Shift q);

    PosShift selectDetail(@Param("id") Long id);

    /** 班次的收款按支付方式 × 结算账户汇总（成功的、已关联单据的） */
    List<PosViews.PayRow> selectPays(@Param("shiftId") Long shiftId);

    /** 班次的单据汇总（销售不含已作废） */
    PosViews.SaleSummary selectSales(@Param("shiftId") Long shiftId);

    /** 已收款未结算的聚合支付、未退成功的退款笔数 */
    int countUnsettled(@Param("shiftId") Long shiftId);

    /** 门店营业日期及之前还没有交班的班次数 */
    int countOpenUntil(@Param("storeId") Long storeId, @Param("date") LocalDate date);
}
