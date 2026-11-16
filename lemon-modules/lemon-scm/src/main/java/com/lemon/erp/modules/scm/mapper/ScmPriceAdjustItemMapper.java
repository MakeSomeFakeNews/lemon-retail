package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.scm.entity.ScmPriceAdjustItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ScmPriceAdjustItemMapper extends BaseMapper<ScmPriceAdjustItem> {

    void insertBatch(@Param("list") List<ScmPriceAdjustItem> list);
}
