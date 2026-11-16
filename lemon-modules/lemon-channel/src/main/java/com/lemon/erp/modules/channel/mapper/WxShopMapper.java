package com.lemon.erp.modules.channel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.channel.entity.WxShop;
import org.apache.ibatis.annotations.Param;

public interface WxShopMapper extends BaseMapper<WxShop> {

    /** 列表：含门店、仓库、客户名称 */
    IPage<WxShop> selectPageList(IPage<WxShop> page, @Param("keyword") String keyword, @Param("status") EnableStatus status,
                                 @Param("storeId") Long storeId);
}
