package com.lemon.erp.modules.pos.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.pos.entity.PosHeldSale;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PosHeldSaleMapper extends BaseMapper<PosHeldSale> {

    /** 门店的挂单（不含购物车内容），按时间倒序 */
    List<PosHeldSale> selectByStore(@Param("storeId") Long storeId);
}
