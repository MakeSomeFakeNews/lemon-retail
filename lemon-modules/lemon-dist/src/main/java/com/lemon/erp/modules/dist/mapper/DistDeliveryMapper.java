package com.lemon.erp.modules.dist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.dist.dto.DistQuery;
import com.lemon.erp.modules.dist.dto.DistViews;
import com.lemon.erp.modules.dist.entity.DistDelivery;
import com.lemon.erp.modules.dist.entity.DistDeliveryItem;
import com.lemon.erp.modules.dist.model.DeliveryKind;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DistDeliveryMapper extends BaseMapper<DistDelivery> {

    /** 列表：含门店、仓库、调拨单号、待处理差异数（数据权限由 Service 启用） */
    IPage<DistDelivery> selectPageList(IPage<DistDelivery> page, @Param("kind") DeliveryKind kind, @Param("q") DistQuery q);

    DistDelivery selectDetail(@Param("id") Long id);

    List<DistDeliveryItem> selectItems(@Param("deliveryId") Long deliveryId);

    /** 行锁：发货、收货、差异处理串行 */
    Long lockById(@Param("id") Long id);

    /** 调拨单明细的调出成本价、调拨价、调拨金额（按明细 ID 顺序，与配送单明细一一对应） */
    List<DistViews.TransferLine> selectTransferLines(@Param("transferId") Long transferId);

    /** 在途查询：已调出未收货的调拨单（数据权限由 Service 启用，按调出、调入仓库） */
    IPage<DistViews.TransitRow> selectTransit(IPage<DistViews.TransitRow> page, @Param("q") DistQuery q);

    /** 在途合计（数量、调拨金额），条件同 selectTransit */
    DistViews.TransitTotal selectTransitTotal(@Param("q") DistQuery q);

    /** 调拨单状态（差异调拨是否仍在途） */
    String selectTransferStatus(@Param("transferId") Long transferId);
}
