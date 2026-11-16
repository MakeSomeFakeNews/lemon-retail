package com.lemon.erp.modules.dist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.dist.dto.DistQuery;
import com.lemon.erp.modules.dist.dto.DistViews;
import com.lemon.erp.modules.dist.entity.DistRequisition;
import com.lemon.erp.modules.dist.entity.DistRequisitionItem;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface DistRequisitionMapper extends BaseMapper<DistRequisition> {

    /** 列表：含门店、仓库、制单人名称、明细行数（数据权限由 Service 启用） */
    IPage<DistRequisition> selectPageList(IPage<DistRequisition> page, @Param("q") DistQuery q);

    DistRequisition selectDetail(@Param("id") Long id);

    /** 明细：含物料、收货仓库现存与在途、供货仓库可用数量 */
    List<DistRequisitionItem> selectItems(@Param("requisitionId") Long requisitionId, @Param("warehouseId") Long warehouseId,
                                          @Param("supplyWarehouseId") Long supplyWarehouseId);

    /** 行锁：审核、配送回写、关闭串行 */
    Long lockById(@Param("id") Long id);

    /** 要货建议：收货仓库的下限、现存、在途和供货仓库的可用数量 */
    List<DistViews.SuggestRow> selectSuggest(@Param("warehouseId") Long warehouseId, @Param("supplyWarehouseId") Long supplyWarehouseId,
                                             @Param("materialIds") Collection<Long> materialIds);

    /** 要货汇总：已审核、部分配送的要货单中待配送的明细，按 供货仓库 + 物料（数据权限由 Service 启用） */
    List<DistViews.SummaryRow> selectSummary(@Param("supplyWarehouseId") Long supplyWarehouseId, @Param("keyword") String keyword);
}
