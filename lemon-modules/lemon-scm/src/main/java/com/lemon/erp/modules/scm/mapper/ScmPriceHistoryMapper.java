package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.scm.entity.ScmPriceHistory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ScmPriceHistoryMapper extends BaseMapper<ScmPriceHistory> {

    void insertBatch(@Param("list") List<ScmPriceHistory> list);
}
