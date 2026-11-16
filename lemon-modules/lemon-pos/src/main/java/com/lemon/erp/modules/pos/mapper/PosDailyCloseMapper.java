package com.lemon.erp.modules.pos.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.entity.PosDailyClose;
import org.apache.ibatis.annotations.Param;

public interface PosDailyCloseMapper extends BaseMapper<PosDailyClose> {

    IPage<PosDailyClose> selectPageList(IPage<PosDailyClose> page, @Param("q") PosQueries.DailyClose q);

    PosDailyClose selectDetail(@Param("id") Long id);
}
