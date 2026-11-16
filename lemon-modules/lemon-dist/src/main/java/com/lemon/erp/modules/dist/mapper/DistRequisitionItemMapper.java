package com.lemon.erp.modules.dist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.dist.entity.DistRequisitionItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DistRequisitionItemMapper extends BaseMapper<DistRequisitionItem> {

    void insertBatch(@Param("list") List<DistRequisitionItem> list);
}
