package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.scm.entity.ScmPriceSchemeItem;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface ScmPriceSchemeItemMapper extends BaseMapper<ScmPriceSchemeItem> {

    /** 方案明细（含物料编码、名称、总部零售价），按物料编码排序 */
    List<ScmPriceSchemeItem> selectBySchemeId(@Param("schemeId") Long schemeId);

    /** 方案中指定物料的明细 */
    List<ScmPriceSchemeItem> selectOfMaterials(@Param("schemeId") Long schemeId, @Param("materialIds") Collection<Long> materialIds);

    void insertBatch(@Param("list") List<ScmPriceSchemeItem> list);
}
