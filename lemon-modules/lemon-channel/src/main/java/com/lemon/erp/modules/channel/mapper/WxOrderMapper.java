package com.lemon.erp.modules.channel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.channel.dto.WxOrderQuery;
import com.lemon.erp.modules.channel.entity.WxOrder;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface WxOrderMapper extends BaseMapper<WxOrder> {

    /** 列表（不含收货信息）：含店铺、门店名称、商品摘要、商品行数 */
    IPage<WxOrder> selectPageList(IPage<WxOrder> page, @Param("q") WxOrderQuery query);

    /** 详情：含店铺、门店名称 */
    WxOrder selectDetail(@Param("id") Long id);

    /** 行锁：同一订单的同步、发货、售后处理串行，返回订单 ID（不存在时为 null） */
    Long lockById(@Param("id") Long id);

    /**
     * 店铺已付款、还没有出库（也没有关闭）的渠道订单占用的数量，按当前映射的物料汇总（基本单位，已扣除发货前退款的数量）：
     * 线上已扣减了这些库存，推送库存时要从可用库存中减去
     *
     * @return [{materialId, qty}]
     */
    List<Map<String, Object>> selectPendingQty(@Param("shopId") Long shopId);
}
