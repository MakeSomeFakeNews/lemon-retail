package com.lemon.erp.modules.pos.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.pos.entity.PosPointsExclude;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PosPointsExcludeMapper extends BaseMapper<PosPointsExclude> {

    /** 全部（带商品编码、名称） */
    List<PosPointsExclude> selectWithNames();

    void insertBatch(@Param("list") List<PosPointsExclude> list);
}
