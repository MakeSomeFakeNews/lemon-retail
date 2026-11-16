package com.lemon.erp.modules.pos.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.dto.PosViews;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 收银报表（只读）：收银日报、POS 销售明细
 */
@Mapper
public interface PosReportMapper {

    /** 收银日报：单据数、金额按维度汇总（销售不含已作废） */
    List<PosViews.ReportSaleRow> selectCashierSales(@Param("q") PosQueries.CashierReport q);

    /** 收银日报：收款 - 退款按维度 × 支付方式汇总 */
    List<PosViews.ReportPayRow> selectCashierPays(@Param("q") PosQueries.CashierReport q);

    /** POS 销售明细（按行） */
    IPage<PosViews.DetailRow> selectDetail(IPage<PosViews.DetailRow> page, @Param("q") PosQueries.Sale q);
}
