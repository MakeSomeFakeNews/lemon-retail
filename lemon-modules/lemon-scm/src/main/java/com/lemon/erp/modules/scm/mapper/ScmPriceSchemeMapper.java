package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.scm.entity.ScmPriceScheme;
import org.apache.ibatis.annotations.Param;

public interface ScmPriceSchemeMapper extends BaseMapper<ScmPriceScheme> {

    /** 列表：含方案中的商品数 */
    IPage<ScmPriceScheme> selectPageList(IPage<ScmPriceScheme> page, @Param("keyword") String keyword, @Param("status") EnableStatus status);
}
