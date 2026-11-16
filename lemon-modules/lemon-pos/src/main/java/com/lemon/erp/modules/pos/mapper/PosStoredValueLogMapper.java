package com.lemon.erp.modules.pos.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.dto.PosViews;
import com.lemon.erp.modules.pos.entity.PosStoredValueLog;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PosStoredValueLogMapper extends BaseMapper<PosStoredValueLog> {

    IPage<PosStoredValueLog> selectPageList(IPage<PosStoredValueLog> page, @Param("q") PosQueries.StoredValueLog q);

    /** 跨店结算：按门店汇总营业日期范围内的充值、赠送、消费、退回、调整 */
    List<PosViews.StoredValueSettleRow> selectSettle(@Param("q") PosQueries.StoredValueSettle q);

    /** 门店某营业日期的储值消费净额（消费 − 退回，正数为净消费） */
    BigDecimal selectDayConsume(@Param("storeId") Long storeId, @Param("date") LocalDate date);
}
