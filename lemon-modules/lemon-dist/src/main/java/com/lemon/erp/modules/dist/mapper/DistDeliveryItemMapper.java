package com.lemon.erp.modules.dist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.dist.entity.DistDeliveryItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DistDeliveryItemMapper extends BaseMapper<DistDeliveryItem> {

    void insertBatch(@Param("list") List<DistDeliveryItem> list);
}
