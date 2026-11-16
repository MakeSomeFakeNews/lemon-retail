package com.lemon.erp.modules.dist.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.modules.dist.dto.DistQuery;
import com.lemon.erp.modules.dist.dto.HandleRequest;
import com.lemon.erp.modules.dist.entity.DistDelivery;
import com.lemon.erp.modules.dist.entity.DistDeliveryItem;
import com.lemon.erp.modules.dist.entity.DistDiscrepancy;
import com.lemon.erp.modules.dist.mapper.DistDeliveryItemMapper;
import com.lemon.erp.modules.dist.mapper.DistDeliveryMapper;
import com.lemon.erp.modules.dist.mapper.DistDiscrepancyMapper;
import com.lemon.erp.modules.dist.model.DeliveryKind;
import com.lemon.erp.modules.dist.model.DistDeliveryStatus;
import com.lemon.erp.modules.dist.model.DiscrepancyHandling;
import com.lemon.erp.modules.dist.model.DiscrepancyKind;
import com.lemon.erp.modules.dist.model.DiscrepancyStatus;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.model.DocStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.lemon.erp.modules.wms.model.WmsConstants.NO_LOCATION;

/**
 * 收货差异处理（总部判定责任方，权限 {@code dist:discrepancy:handle}）。发货方 / 收货方：配送为 配送中心 / 门店，退仓为 门店 / 配送中心。
 * <ul>
 *   <li>少收 / 破损（差异数量已在途：收货仓库 → 发货仓库）：
 *     <ul>
 *       <li>退回发货方：发货仓库收下差异调拨（货物实际未发出或已退回）；</li>
 *       <li>发货方承担：发货仓库收下后报损出库（其他出库，类别默认「配送差异报损」）；</li>
 *       <li>收货方承担：发货仓库收下后调给收货仓库（非在途），再在收货仓库报损出库——收货方库存不变、记一笔报损。</li>
 *     </ul></li>
 *   <li>多收（多出的货物未入账）：收货方留用（发货仓库调给收货仓库，补记库存）或退回发货方（不入账）。</li>
 * </ul>
 * 处理在当天的业务日期生成仓库单据（受结账与日期锁定限制）；全部差异处理完后配送单变为已完成
 */
@Service
@RequiredArgsConstructor
public class DistDiscrepancyService {

    /** 报损的默认出库类别（字典 wms_out_category 内置取值） */
    public static final String DEFAULT_LOSS_CATEGORY = "DIST_LOSS";

    private final DistDiscrepancyMapper discrepancyMapper;
    private final DistDeliveryMapper deliveryMapper;
    private final DistDeliveryItemMapper itemMapper;
    private final DistDeliveryService deliveryService;
    private final DistStockMoves moves;

    /** 列表：数据权限按配送单的发货、收货仓库（主表 dist_delivery） */
    @DataPermission(table = "dist_delivery", deptColumn = "", userColumn = "", warehouseColumn = "from_warehouse_id,to_warehouse_id")
    public PageResult<DistDiscrepancy> page(DeliveryKind kind, long current, long size, DistQuery query) {
        Page<DistDiscrepancy> page = Page.of(current, size);
        discrepancyMapper.selectPageList(page, kind, query);
        return PageResult.of(page);
    }

    @Transactional
    public void handle(Long id, HandleRequest req) {
        DistDiscrepancy found = discrepancyMapper.selectById(id);
        if (found == null) {
            throw new BizException("dist.error.discrepancyNotFound");
        }
        DistDelivery d = deliveryService.getAccessible(null, found.getDeliveryId());
        deliveryMapper.lockById(d.getId());
        DistDiscrepancy x = discrepancyMapper.selectById(id);
        if (x.getStatus() != DiscrepancyStatus.PENDING) {
            throw new BizException("dist.error.discrepancyHandled");
        }
        DistDeliveryItem item = itemMapper.selectById(x.getDeliveryItemId());
        DiscrepancyHandling handling = req.handling();
        boolean over = x.getKind() == DiscrepancyKind.OVER;
        if (over ? handling != DiscrepancyHandling.ACCEPT && handling != DiscrepancyHandling.RETURN_SOURCE
                : handling == DiscrepancyHandling.ACCEPT) {
            throw new BizException("dist.error.handlingNotAllowed");
        }
        String category = StringUtils.hasText(req.ioCategory()) ? req.ioCategory().trim() : DEFAULT_LOSS_CATEGORY;
        LocalDate today = LocalDate.now();
        boolean differential = moves.differential(d);
        List<String> docNos = new ArrayList<>();
        String sourceNo = d.getDeliveryNo();
        if (!over) {
            // 差异调拨仍在途时，发货仓库收下（回到发货时的库位）
            if (x.getTransferId() != null && DocStatus.TRANSIT.name().equals(deliveryMapper.selectTransferStatus(x.getTransferId()))) {
                moves.receive(x.getTransferId(), today);
            }
            if (handling == DiscrepancyHandling.SOURCE_WRITE_OFF) {
                docNos.add(moves.writeOff(d.getFromWarehouseId(), today, category,
                        List.of(line(item, x, item.getLocationId(), null)), sourceNo, req.remark()).getDocNo());
            } else if (handling == DiscrepancyHandling.TARGET_WRITE_OFF) {
                WmsStockDoc t = moves.transfer(d.getFromWarehouseId(), d.getToWarehouseId(), today, differential, false,
                        List.of(line(item, x, item.getLocationId(), NO_LOCATION)), sourceNo, req.remark());
                docNos.add(t.getDocNo());
                docNos.add(moves.writeOff(d.getToWarehouseId(), today, category,
                        List.of(line(item, x, NO_LOCATION, null)), sourceNo, req.remark()).getDocNo());
            }
        } else if (handling == DiscrepancyHandling.ACCEPT) {
            // 多收留用：发货仓库调给收货仓库，补记库存
            WmsStockDoc t = moves.transfer(d.getFromWarehouseId(), d.getToWarehouseId(), today, differential, false,
                    List.of(line(item, x, item.getLocationId(), NO_LOCATION)), sourceNo, req.remark());
            docNos.add(t.getDocNo());
        }
        boolean writeOff = handling == DiscrepancyHandling.SOURCE_WRITE_OFF || handling == DiscrepancyHandling.TARGET_WRITE_OFF;
        StatusTransitions.change(discrepancyMapper, DistDiscrepancy::getId, id, DistDiscrepancy::getStatus,
                DiscrepancyStatus.PENDING, DiscrepancyStatus.HANDLED, w -> w
                        .set(DistDiscrepancy::getHandling, handling)
                        .set(DistDiscrepancy::getIoCategory, writeOff ? category : null)
                        .set(DistDiscrepancy::getHandleDocNos, docNos.isEmpty() ? null : String.join(",", docNos))
                        .set(DistDiscrepancy::getHandleRemark, req.remark())
                        .set(DistDiscrepancy::getHandleBy, SecurityUtils.getUserId())
                        .set(DistDiscrepancy::getHandleTime, LocalDateTime.now())
                        .set(DistDiscrepancy::getUpdateTime, LocalDateTime.now()),
                "dist.error.discrepancyHandled");
        boolean pending = discrepancyMapper.exists(Wrappers.<DistDiscrepancy>lambdaQuery()
                .eq(DistDiscrepancy::getDeliveryId, d.getId()).eq(DistDiscrepancy::getStatus, DiscrepancyStatus.PENDING));
        if (!pending) {
            StatusTransitions.tryChange(deliveryMapper, DistDelivery::getId, d.getId(), DistDelivery::getStatus,
                    DistDeliveryStatus.DIFF, DistDeliveryStatus.CLOSED);
        }
    }

    private static DistStockMoves.Line line(DistDeliveryItem item, DistDiscrepancy x, Long locationId, Long toLocationId) {
        return new DistStockMoves.Line(x.getMaterialId(), locationId, toLocationId, x.getBatchNo(), null, x.getQty(), item.getPrice(), null);
    }
}
