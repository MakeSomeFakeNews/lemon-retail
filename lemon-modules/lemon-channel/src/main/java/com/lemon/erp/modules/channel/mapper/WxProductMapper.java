package com.lemon.erp.modules.channel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.channel.dto.WxProductQuery;
import com.lemon.erp.modules.channel.entity.WxProduct;
import org.apache.ibatis.annotations.Param;

public interface WxProductMapper extends BaseMapper<WxProduct> {

    /** 列表：含店铺名称、映射物料的编码、名称、规格、单位、零售价 */
    IPage<WxProduct> selectPageList(IPage<WxProduct> page, @Param("q") WxProductQuery query);
}
