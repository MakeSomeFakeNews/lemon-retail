package com.lemon.erp.modules.channel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.channel.dto.WxAftersaleQuery;
import com.lemon.erp.modules.channel.entity.WxAftersale;
import org.apache.ibatis.annotations.Param;

public interface WxAftersaleMapper extends BaseMapper<WxAftersale> {

    /** 列表：含店铺、门店名称、渠道订单的 ERP 处理状态 */
    IPage<WxAftersale> selectPageList(IPage<WxAftersale> page, @Param("q") WxAftersaleQuery query);

    /** 详情 */
    WxAftersale selectDetail(@Param("id") Long id);
}
