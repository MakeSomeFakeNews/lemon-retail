package com.lemon.erp.modules.pos.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.pos.entity.PosTerminal;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PosTerminalMapper extends BaseMapper<PosTerminal> {

    /** 列表：含门店、仓库、现金账户名称和当班的班次 */
    IPage<PosTerminal> selectPageList(IPage<PosTerminal> page, @Param("keyword") String keyword, @Param("storeId") Long storeId,
                                      @Param("status") EnableStatus status);

    /** 详情（含名称） */
    PosTerminal selectDetail(@Param("id") Long id);

    /** 行锁：同一收银台的开班互斥 */
    Long lockById(@Param("id") Long id);

    /** 本收银台仓库中物料的可用库存（库存 - 冻结，基本单位；只读关联仓库表） */
    BigDecimal selectAvailableQty(@Param("warehouseId") Long warehouseId, @Param("materialId") Long materialId);

    /** 各物料在仓库中的可用库存：[{materialId, qty}] */
    List<Map<String, Object>> selectAvailableQtys(@Param("warehouseId") Long warehouseId, @Param("materialIds") List<Long> materialIds);
}
