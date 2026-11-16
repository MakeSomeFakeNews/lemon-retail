package com.lemon.erp.modules.chain.mapper;

import com.lemon.erp.modules.chain.dto.ChainViews;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * 门店损益、补货建议、店长看板的统计（只读关联 POS、供应链、库存、门店往来、其他收支，见 ChainReportMapper.xml）
 */
public interface ChainReportMapper {

    /** POS 零售净额：不含税（实收 × 100 ÷ (100 + 税率)），退货为负，不含作废 */
    List<ChainViews.StoreMonthAmount> selectRetail(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** POS 零售成本：零售出库 / 退货的库存流水实际成本（不含作废） */
    List<ChainViews.StoreMonthAmount> selectRetailCost(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 批发（销售出库 − 销售退货，不含税），按单据仓库的门店 */
    List<ChainViews.StoreMonthAmount> selectWholesale(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 批发成本：销售出库 / 退货的库存流水实际成本 */
    List<ChainViews.StoreMonthAmount> selectWholesaleCost(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 内部调拨差价（门店往来：调拨金额 − 调出成本），记为调出门店的收入 */
    List<ChainViews.StoreMonthAmount> selectTransferIncome(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 内部调拨差价，记为调入门店的成本 */
    List<ChainViews.StoreMonthAmount> selectTransferCost(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /**
     * 已确认的其他收支（不含门店日结生成的零售收支），按结算账户的门店，各门店共用的账户计入总部
     *
     * @param direction IN 其他收入 / OUT 费用支出
     */
    List<ChainViews.StoreMonthAmount> selectIo(@Param("begin") LocalDate begin, @Param("end") LocalDate end,
                                               @Param("direction") String direction, @Param("hqId") Long hqId);

    /** 门店在日期范围内的其他收支，按收支类别 */
    List<ChainViews.ExpenseRow> selectIoByCategory(@Param("begin") LocalDate begin, @Param("end") LocalDate end,
                                                   @Param("direction") String direction, @Param("hqId") Long hqId,
                                                   @Param("storeId") Long storeId);

    /** 分摊的总部费用（月份 yyyy-MM 的范围） */
    List<ChainViews.StoreMonthAmount> selectAllocated(@Param("beginPeriod") String beginPeriod, @Param("endPeriod") String endPeriod);

    /** 各门店的启用员工数（用户的所属门店） */
    List<ChainViews.StoreCount> selectStaffCounts();

    /**
     * 补货建议的基础数据：门店仓库中近 N 天有销售（零售 / 销售出库，扣退货）的物料，及现存、在途、未完成的要货
     *
     * @param warehouseIds 门店的仓库
     */
    List<ChainViews.ReplenishRow> selectReplenish(@Param("warehouseIds") Collection<Long> warehouseIds, @Param("begin") LocalDate begin,
                                                  @Param("end") LocalDate end, @Param("keyword") String keyword);

    /** 店长看板：门店某天的 POS 单数、退货单数、含税实收净额、不含税净额（退货为负，不含作废） */
    ChainViews.DayFigures selectDayFigures(@Param("storeId") Long storeId, @Param("date") LocalDate date);

    /** 店长看板：门店某天的 POS 销售成本 */
    BigDecimal selectDayCost(@Param("storeId") Long storeId, @Param("date") LocalDate date);

    /** 店长看板：门店某天的畅销商品（按净数量） */
    List<ChainViews.TopItem> selectTopItems(@Param("storeId") Long storeId, @Param("date") LocalDate date);

    /** 店长看板：待收货的配送单数 */
    Integer countPendingReceive(@Param("storeId") Long storeId);

    /** 店长看板：草稿的要货单数 */
    Integer countDraftRequisitions(@Param("storeId") Long storeId);
}
