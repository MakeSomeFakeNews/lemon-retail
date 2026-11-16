package com.lemon.erp.modules.pos.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.pos.entity.PosSpecialTarget;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PosSpecialTargetMapper extends BaseMapper<PosSpecialTarget> {

    void insertBatch(@Param("list") List<PosSpecialTarget> targets);
}
