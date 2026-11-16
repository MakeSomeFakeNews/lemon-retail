package com.lemon.erp.modules.pos.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.dto.PosViews;
import com.lemon.erp.modules.pos.entity.PosSale;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface PosSaleMapper extends BaseMapper<PosSale> {

    /** 列表：含门店、收银台、收银员、会员、支付方式 */
    IPage<PosSale> selectPageList(IPage<PosSale> page, @Param("q") PosQueries.Sale q);

    PosSale selectDetail(@Param("id") Long id);

    /** 按单号查找（原单退货扫小票） */
    Long selectIdByNo(@Param("saleNo") String saleNo);

    /** 生成的仓库单据明细（只读）：物料、库位、批次、数量，退货 / 作废时按原出库位置入库 */
    List<Map<String, Object>> selectStockLines(@Param("docId") Long docId);

    /** 门店某营业日期的单据汇总 */
    PosViews.SaleSummary selectDaySales(@Param("storeId") Long storeId, @Param("date") LocalDate date);

    /** 门店某营业日期的收款按支付方式 × 结算账户汇总 */
    List<PosViews.PayRow> selectDayPays(@Param("storeId") Long storeId, @Param("date") LocalDate date);

    /** 门店某营业日期开班的班次：[{shiftCount, expectedCash, countedCash, cashDiff}] */
    Map<String, Object> selectDayShifts(@Param("storeId") Long storeId, @Param("date") LocalDate date);

    /** 原销售单是否已有退货 */
    boolean existsReturn(@Param("origSaleId") Long origSaleId);
}
