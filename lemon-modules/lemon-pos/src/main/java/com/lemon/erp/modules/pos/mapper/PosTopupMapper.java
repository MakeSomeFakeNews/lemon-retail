package com.lemon.erp.modules.pos.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.entity.PosTopup;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Map;

public interface PosTopupMapper extends BaseMapper<PosTopup> {

    IPage<PosTopup> selectPageList(IPage<PosTopup> page, @Param("q") PosQueries.Topup q);

    PosTopup selectDetail(@Param("id") Long id);

    /** 门店某营业日期的充值：topupCount、topupAmount、bonusAmount */
    Map<String, Object> selectDaySummary(@Param("storeId") Long storeId, @Param("date") LocalDate date);
}
