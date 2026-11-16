package com.lemon.erp.modules.pos.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.pos.entity.PosPromotionGoods;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface PosPromotionGoodsMapper extends BaseMapper<PosPromotionGoods> {

    void insertBatch(@Param("list") List<PosPromotionGoods> goods);

    /** 活动的参与商品 / 分类（带商品编码、名称） */
    List<PosPromotionGoods> selectByPromotionIds(@Param("ids") Collection<Long> promotionIds);
}
