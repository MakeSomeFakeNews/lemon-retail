package com.lemon.erp.modules.pos.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.pos.entity.PosTopupTier;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PosTopupTierMapper extends BaseMapper<PosTopupTier> {

    void insertBatch(@Param("list") List<PosTopupTier> tiers);
}
