package com.lemon.erp.modules.pos.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.pos.entity.PosSpecialItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PosSpecialItemMapper extends BaseMapper<PosSpecialItem> {

    void insertBatch(@Param("list") List<PosSpecialItem> items);

    /** 活动商品（含物料编码、名称、规格、单位、总部零售价） */
    List<PosSpecialItem> selectBySpecialId(@Param("specialId") Long specialId);
}
