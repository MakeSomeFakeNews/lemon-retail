package com.lemon.erp.modules.pos.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.pos.entity.PosPromotionTarget;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PosPromotionTargetMapper extends BaseMapper<PosPromotionTarget> {

    void insertBatch(@Param("list") List<PosPromotionTarget> targets);
}
