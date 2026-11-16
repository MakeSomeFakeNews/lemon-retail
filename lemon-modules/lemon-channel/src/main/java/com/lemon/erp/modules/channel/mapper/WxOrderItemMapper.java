package com.lemon.erp.modules.channel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.channel.entity.WxOrderItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface WxOrderItemMapper extends BaseMapper<WxOrderItem> {

    /** 订单明细：含物料编码、名称（已生成销售订单的取生成时的物料，否则取当前映射），按 ID 顺序 */
    List<WxOrderItem> selectByOrder(@Param("wxOrderId") Long wxOrderId, @Param("shopId") Long shopId);

    /** 批量插入（单条多值 INSERT） */
    int insertBatch(@Param("list") List<WxOrderItem> items);
}
