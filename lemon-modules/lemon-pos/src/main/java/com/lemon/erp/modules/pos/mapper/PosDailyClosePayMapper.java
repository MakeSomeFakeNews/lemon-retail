package com.lemon.erp.modules.pos.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.pos.entity.PosDailyClosePay;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PosDailyClosePayMapper extends BaseMapper<PosDailyClosePay> {

    void insertBatch(@Param("list") List<PosDailyClosePay> pays);

    /** 日结的收款明细（含结算账户名称） */
    List<PosDailyClosePay> selectByCloseId(@Param("closeId") Long closeId);
}
