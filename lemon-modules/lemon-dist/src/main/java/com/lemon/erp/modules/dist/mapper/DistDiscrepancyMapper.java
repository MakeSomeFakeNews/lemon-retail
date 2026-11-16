package com.lemon.erp.modules.dist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.dist.dto.DistQuery;
import com.lemon.erp.modules.dist.entity.DistDiscrepancy;
import com.lemon.erp.modules.dist.model.DeliveryKind;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DistDiscrepancyMapper extends BaseMapper<DistDiscrepancy> {

    /** 列表：含配送单号、门店、物料（数据权限按配送单的发货、收货仓库，由 Service 启用） */
    IPage<DistDiscrepancy> selectPageList(IPage<DistDiscrepancy> page, @Param("kind") DeliveryKind kind, @Param("q") DistQuery q);

    List<DistDiscrepancy> selectByDelivery(@Param("deliveryId") Long deliveryId);
}
