package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.scm.entity.ScmPriceAdjust;
import com.lemon.erp.modules.scm.entity.ScmPriceAdjustItem;
import com.lemon.erp.modules.scm.entity.ScmPriceHistory;
import com.lemon.erp.modules.scm.model.PriceAdjustStatus;
import com.lemon.erp.modules.scm.model.PriceAdjustTarget;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScmPriceAdjustMapper extends BaseMapper<ScmPriceAdjust> {

    /** 列表：含制单人、明细行数、价格方案名称 */
    IPage<ScmPriceAdjust> selectPageList(IPage<ScmPriceAdjust> page, @Param("keyword") String keyword,
                                         @Param("status") PriceAdjustStatus status, @Param("target") PriceAdjustTarget target,
                                         @Param("schemeId") Long schemeId);

    /** 详情（含制单人） */
    ScmPriceAdjust selectDetail(@Param("id") Long id);

    /** 明细（含物料编码、名称、当前总部零售价） */
    List<ScmPriceAdjustItem> selectItems(@Param("adjustId") Long adjustId);

    /** 到达生效时间的待生效调价单 ID，按生效时间、ID 顺序 */
    List<Long> selectDueIds(@Param("status") PriceAdjustStatus status, @Param("now") LocalDateTime now);

    /** 行锁：生效与作废串行 */
    PriceAdjustStatus selectStatusForUpdate(@Param("id") Long id);

    /** 调价历史：按物料 / 方案 / 对象筛选，按生效时间倒序 */
    IPage<ScmPriceHistory> selectHistory(IPage<ScmPriceHistory> page, @Param("materialId") Long materialId,
                                         @Param("schemeId") Long schemeId, @Param("target") PriceAdjustTarget target,
                                         @Param("adjustId") Long adjustId);
}
