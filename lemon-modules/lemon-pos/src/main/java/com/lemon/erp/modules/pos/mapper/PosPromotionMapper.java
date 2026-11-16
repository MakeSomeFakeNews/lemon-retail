package com.lemon.erp.modules.pos.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.entity.PosPromotion;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PosPromotionMapper extends BaseMapper<PosPromotion> {

    IPage<PosPromotion> selectPageList(IPage<PosPromotion> page, @Param("q") PosQueries.Promotion q);

    PosPromotion selectDetail(@Param("id") Long id);

    /** 在 now 时进行中、门店收银可用的活动（门店适用范围、仅限会员由调用方判断） */
    List<PosPromotion> selectActive(@Param("now") LocalDateTime now);
}
