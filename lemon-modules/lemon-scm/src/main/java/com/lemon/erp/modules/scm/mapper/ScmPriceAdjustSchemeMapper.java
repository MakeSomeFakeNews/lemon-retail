package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.scm.entity.ScmPriceAdjustScheme;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ScmPriceAdjustSchemeMapper extends BaseMapper<ScmPriceAdjustScheme> {

    void insertBatch(@Param("list") List<ScmPriceAdjustScheme> list);
}
