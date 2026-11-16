package com.lemon.erp.modules.pos.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.pos.entity.PosPromotionGift;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface PosPromotionGiftMapper extends BaseMapper<PosPromotionGift> {

    void insertBatch(@Param("list") List<PosPromotionGift> gifts);

    /** 活动的赠品（带商品编码、名称、基本单位） */
    List<PosPromotionGift> selectByPromotionIds(@Param("ids") Collection<Long> promotionIds);
}
