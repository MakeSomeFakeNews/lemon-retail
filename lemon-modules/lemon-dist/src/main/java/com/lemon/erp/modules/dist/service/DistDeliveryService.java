package com.lemon.erp.modules.dist.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.DetailLines;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.period.BizDateLocks;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataScopes;
import com.lemon.erp.framework.store.StoreType;
import com.lemon.erp.modules.dist.dto.DeliveryRequest;
import com.lemon.erp.modules.dist.dto.DistQuery;
import com.lemon.erp.modules.dist.dto.DistViews;
import com.lemon.erp.modules.dist.dto.ReceiveRequest;
import com.lemon.erp.modules.dist.entity.DistDelivery;
import com.lemon.erp.modules.dist.entity.DistDeliveryItem;
import com.lemon.erp.modules.dist.entity.DistDiscrepancy;
import com.lemon.erp.modules.dist.entity.DistRequisition;
import com.lemon.erp.modules.dist.entity.DistRequisitionItem;
import com.lemon.erp.modules.dist.mapper.DistDeliveryItemMapper;
import com.lemon.erp.modules.dist.mapper.DistDeliveryMapper;
import com.lemon.erp.modules.dist.mapper.DistDiscrepancyMapper;
import com.lemon.erp.modules.dist.model.DeliveryKind;
import com.lemon.erp.modules.dist.model.DistDeliveryStatus;
import com.lemon.erp.modules.dist.model.DiscrepancyKind;
import com.lemon.erp.modules.dist.model.DiscrepancyStatus;
import com.lemon.erp.modules.store.entity.StoreInfo;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.entity.WmsWarehouse;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsStockDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.wms.model.WmsConstants.NO_LOCATION;

/**
 * 配送单与门店退仓单（同一张表，按种类区分）：
 * <ul>
 *   <li>配送：草稿（由要货单生成或手工开单）→ 发货（生成在途调拨：配送中心调出，货物在途；回写要货单已配送数量）→ 门店收货；</li>
 *   <li>退仓：门店草稿 → 提交 → 总部审核（或驳回回到草稿）→ 门店发货（在途调拨：门店调出）→ 配送中心收货。</li>
 * </ul>
 * 收货：调拨单整单调入收货仓库，实收少于发货的数量（少收 / 破损）立即生成反向的在途调拨（收货仓库 → 发货仓库），
 * 多于发货的（多收）只登记不入账，都记为收货差异（{@link DistDiscrepancyService} 处理）。这样收货方的库存始终等于实收数量，
 * 在途 = 未收货的配送 + 待处理的少收差异，可按调拨单逐笔对账（在途查询）
 */
@Service
@RequiredArgsConstructor
public class DistDeliveryService {

    /** 超量配送：发货数量可以超过要货单批准的剩余数量 */
    public static final String OVER_QTY_PERM = "dist:delivery:overQty";
    /** 修改配送价 */
    public static final String PRICE_PERM = "dist:delivery:price";
    /** 查看成本金额（wms） */
    private static final String COST_PERM = WmsStockDocService.COST_PERM;

    private final DistDeliveryMapper deliveryMapper;
    private final DistDeliveryItemMapper itemMapper;
    private final DistDiscrepancyMapper discrepancyMapper;
    private final DistRequisitionService requisitionService;
    private final DistStockMoves moves;
    private final DistSupport support;
    private final DocNoService docNoService;
    private final PermissionService permissionService;
    private final BizDateLocks dateLocks;

    @DataPermission(table = "dist_delivery", deptColumn = "", userColumn = "", warehouseColumn = "from_warehouse_id,to_warehouse_id")
    public PageResult<DistDelivery> page(DeliveryKind kind, long current, long size, DistQuery query) {
        Page<DistDelivery> page = Page.of(current, size);
        deliveryMapper.selectPageList(page, kind, query);
        page.getRecords().forEach(this::hideAmounts);
        return PageResult.of(page);
    }

    public DistDelivery detail(DeliveryKind kind, Long id) {
        getAccessible(kind, id);
        DistDelivery d = deliveryMapper.selectDetail(id);
        d.setItems(deliveryMapper.selectItems(id));
        d.setDiscrepancies(discrepancyMapper.selectByDelivery(id));
        hideAmounts(d);
        return d;
    }

    /**
     * 配送价预览：加盟店取配送价方案的价格（没有方案价的物料不返回，发货时按调出成本），其他门店为空（按调出成本）
     */
    public Map<Long, BigDecimal> prices(Long toWarehouseId, Collection<Long> materialIds) {
        if (toWarehouseId == null || materialIds == null || materialIds.isEmpty()) {
            return Map.of();
        }
        return moves.schemePrices(support.store(support.warehouse(toWarehouseId).getStoreId()), materialIds);
    }

    @Transactional
    public Long create(DeliveryKind kind, DeliveryRequest req) {
        Header h = header(kind, req);
        DistDelivery d = new DistDelivery();
        d.setDeliveryNo(docNoService.next(kind == DeliveryKind.DELIVERY ? DistDocNos.DELIVERY : DistDocNos.RETURN));
        d.setKind(kind);
        h.apply(d);
        d.setBizDate(req.bizDate());
        d.setRemark(req.remark());
        d.setStatus(DistDeliveryStatus.DRAFT);
        d.setTotalQty(Decimals.sum(req.items(), DeliveryRequest.Item::qty));
        d.setDeptId(SecurityUtils.getLoginUser().deptId());
        deliveryMapper.insert(d);
        itemMapper.insertBatch(toItems(d.getId(), kind, h, req));
        return d.getId();
    }

    @Transactional
    public void update(DeliveryKind kind, Long id, DeliveryRequest req) {
        DistDelivery old = getDraft(kind, id);
        dateLocks.check(old.getBizDate(), old.getFromWarehouseId(), labelOf(kind));
        Header h = header(kind, req);
        StatusTransitions.change(deliveryMapper, DistDelivery::getId, id, DistDelivery::getStatus,
                DistDeliveryStatus.DRAFT, DistDeliveryStatus.DRAFT, w -> w
                        .set(DistDelivery::getFromStoreId, h.fromStoreId())
                        .set(DistDelivery::getFromWarehouseId, h.fromWarehouseId())
                        .set(DistDelivery::getToStoreId, h.toStoreId())
                        .set(DistDelivery::getToWarehouseId, h.toWarehouseId())
                        .set(DistDelivery::getBizDate, req.bizDate())
                        .set(DistDelivery::getRemark, req.remark())
                        .set(DistDelivery::getTotalQty, Decimals.sum(req.items(), DeliveryRequest.Item::qty))
                        .set(DistDelivery::getUpdateTime, LocalDateTime.now()),
                "dist.error.deliveryStatusChanged");
        DetailLines.replace(itemMapper, DistDeliveryItem::getDeliveryId, id, toItems(id, kind, h, req),
                DistDeliveryItem::setDeliveryId, itemMapper::insertBatch);
    }

    @Transactional
    public void delete(DeliveryKind kind, List<Long> ids) {
        for (Long id : ids) {
            DistDelivery d = getDraft(kind, id);
            dateLocks.check(d.getBizDate(), d.getFromWarehouseId(), labelOf(kind));
            itemMapper.delete(Wrappers.<DistDeliveryItem>lambdaQuery().eq(DistDeliveryItem::getDeliveryId, id));
            deliveryMapper.deleteById(id);
        }
    }

    /**
     * 由已审核的要货单生成配送单（草稿）：按 供货仓库 + 收货仓库 分组，每组一张，明细为各要货单待配送的数量（批准 − 已配送）
     *
     * @return 生成的配送单 ID
     */
    @Transactional
    public List<Long> fromRequisitions(List<Long> requisitionIds, LocalDate bizDate) {
        if (requisitionIds == null || requisitionIds.isEmpty()) {
            throw new BizException("dist.valid.selectRequisitions");
        }
        LocalDate date = bizDate == null ? LocalDate.now() : bizDate;
        Map<String, List<DeliveryRequest.Item>> groups = new LinkedHashMap<>();
        Map<String, DistRequisition> heads = new HashMap<>();
        for (Long id : requisitionIds.stream().distinct().toList()) {
            DistRequisition r = requisitionService.getAccessible(id);
            if (!r.getStatus().deliverable()) {
                throw new BizException("dist.error.requisitionNotDeliverable", r.getReqNo());
            }
            String key = r.getSupplyWarehouseId() + ":" + r.getWarehouseId();
            heads.putIfAbsent(key, r);
            for (DistRequisitionItem i : requisitionService.items(id)) {
                BigDecimal pending = Decimals.nz(i.getApprovedQty()).subtract(Decimals.nz(i.getDeliveredQty()));
                if (pending.signum() > 0) {
                    groups.computeIfAbsent(key, k -> new ArrayList<>())
                            .add(new DeliveryRequest.Item(i.getId(), i.getMaterialId(), null, null, null, pending, null, i.getRemark()));
                }
            }
        }
        if (groups.isEmpty()) {
            throw new BizException("dist.error.nothingToDeliver");
        }
        List<Long> ids = new ArrayList<>();
        groups.forEach((key, lines) -> {
            DistRequisition r = heads.get(key);
            ids.add(create(DeliveryKind.DELIVERY, new DeliveryRequest(r.getSupplyWarehouseId(), r.getWarehouseId(), date, null, lines)));
        });
        return ids;
    }

    // ---------------------------------------------------------------- 退仓审核

    /** 门店提交退仓申请：草稿 → 待审核 */
    @Transactional
    public void submitReturn(Long id) {
        DistDelivery d = getDraft(DeliveryKind.RETURN, id);
        dateLocks.check(d.getBizDate(), d.getFromWarehouseId(), labelOf(DeliveryKind.RETURN));
        StatusTransitions.change(deliveryMapper, DistDelivery::getId, id, DistDelivery::getStatus,
                DistDeliveryStatus.DRAFT, DistDeliveryStatus.SUBMITTED, w -> w.set(DistDelivery::getRejectReason, null),
                "dist.error.deliveryStatusChanged");
    }

    /** 总部审核退仓：待审核 → 已审核（门店可以发货），审核人须可管辖收货方（总部 / 配送中心） */
    @Transactional
    public void approveReturn(Long id) {
        DistDelivery d = getAccessible(DeliveryKind.RETURN, id);
        DistSupport.checkStoreAccess(d.getToStoreId(), null);
        StatusTransitions.change(deliveryMapper, DistDelivery::getId, id, DistDelivery::getStatus,
                DistDeliveryStatus.SUBMITTED, DistDeliveryStatus.APPROVED, w -> w
                        .set(DistDelivery::getApproveBy, SecurityUtils.getUserId())
                        .set(DistDelivery::getApproveTime, LocalDateTime.now()),
                "dist.error.returnNotSubmitted", d.getDeliveryNo());
    }

    /** 总部驳回退仓：待审核 → 草稿 */
    @Transactional
    public void rejectReturn(Long id, String reason) {
        DistDelivery d = getAccessible(DeliveryKind.RETURN, id);
        DistSupport.checkStoreAccess(d.getToStoreId(), null);
        StatusTransitions.change(deliveryMapper, DistDelivery::getId, id, DistDelivery::getStatus,
                DistDeliveryStatus.SUBMITTED, DistDeliveryStatus.DRAFT, w -> w.set(DistDelivery::getRejectReason, reason),
                "dist.error.returnNotSubmitted", d.getDeliveryNo());
    }

    // ---------------------------------------------------------------- 发货、收货

    /**
     * 发货：配送单从草稿、退仓单从已审核发货。生成在途调拨（发货仓库调出、收货仓库收货时调入），回写要货单已配送数量，
     * 明细的配送价、金额、调出成本价取调拨单确认后生成的调拨价（见 {@link DistStockMoves} 的配送价规则）
     */
    @Transactional
    public void ship(DeliveryKind kind, Long id) {
        getAccessible(kind, id);
        deliveryMapper.lockById(id);
        DistDelivery d = deliveryMapper.selectById(id);
        DistDeliveryStatus from = kind == DeliveryKind.DELIVERY ? DistDeliveryStatus.DRAFT : DistDeliveryStatus.APPROVED;
        if (d.getStatus() != from) {
            throw new BizException(kind == DeliveryKind.DELIVERY ? "dist.error.deliveryNotDraft" : "dist.error.returnNotApproved", d.getDeliveryNo());
        }
        List<DistDeliveryItem> items = items(id);
        if (items.isEmpty()) {
            throw new BizException("dist.valid.itemsRequired");
        }
        // 要货单已配送数量（行锁），超过批准剩余数量需要「超量配送」权限
        Map<Long, BigDecimal> deltas = new HashMap<>();
        items.stream().filter(i -> i.getRequisitionItemId() != null)
                .forEach(i -> deltas.merge(i.getRequisitionItemId(), i.getQty(), BigDecimal::add));
        requisitionService.addDelivered(deltas, permissionService.hasPerm(OVER_QTY_PERM));

        boolean differential = moves.differential(d);
        Map<Long, BigDecimal> schemePrices = differential
                ? moves.schemePrices(support.store(d.getToStoreId()), items.stream().map(DistDeliveryItem::getMaterialId).collect(Collectors.toSet()))
                : Map.of();
        List<DistStockMoves.Line> lines = items.stream().map(i -> new DistStockMoves.Line(i.getMaterialId(), i.getLocationId(), NO_LOCATION,
                i.getBatchNo(), i.getSerialNos(), i.getQty(), i.getPrice() != null ? i.getPrice() : schemePrices.get(i.getMaterialId()),
                i.getRemark())).toList();
        WmsStockDoc transfer = moves.transfer(d.getFromWarehouseId(), d.getToWarehouseId(), d.getBizDate(), differential, true,
                lines, d.getDeliveryNo(), d.getRemark());

        // 调拨单明细与配送单明细按顺序一一对应
        List<DistViews.TransferLine> settled = deliveryMapper.selectTransferLines(transfer.getId());
        BigDecimal total = null;
        for (int k = 0; k < items.size() && k < settled.size(); k++) {
            DistViews.TransferLine t = settled.get(k);
            itemMapper.update(Wrappers.<DistDeliveryItem>lambdaUpdate()
                    .set(DistDeliveryItem::getPrice, t.getTransferPrice())
                    .set(DistDeliveryItem::getAmount, t.getTransferAmount())
                    .set(DistDeliveryItem::getCostPrice, t.getCostPrice())
                    .set(DistDeliveryItem::getUpdateTime, LocalDateTime.now())
                    .eq(DistDeliveryItem::getId, items.get(k).getId()));
            if (t.getTransferAmount() != null) {
                total = Decimals.nz(total).add(t.getTransferAmount());
            }
        }
        BigDecimal totalAmount = total;
        StatusTransitions.change(deliveryMapper, DistDelivery::getId, id, DistDelivery::getStatus, from, DistDeliveryStatus.SHIPPED, w -> w
                        .set(DistDelivery::getTransferId, transfer.getId())
                        .set(DistDelivery::getTotalQty, Decimals.sum(items, DistDeliveryItem::getQty))
                        .set(DistDelivery::getTotalAmount, totalAmount)
                        .set(DistDelivery::getShipBy, SecurityUtils.getUserId())
                        .set(DistDelivery::getShipTime, LocalDateTime.now())
                        .set(DistDelivery::getUpdateTime, LocalDateTime.now()),
                "dist.error.deliveryStatusChanged");
    }

    /**
     * 收货：收货人须可管辖收货门店。调拨单整单调入收货仓库后，少收 / 破损的数量生成反向的在途调拨（收货仓库 → 发货仓库），
     * 多收的只登记；有差异时状态为「收货有差异」，否则「已收货」。序列号物料须按发货数量收货（差异走退仓）
     */
    @Transactional
    public void receive(DeliveryKind kind, Long id, ReceiveRequest req) {
        getAccessible(kind, id);
        deliveryMapper.lockById(id);
        DistDelivery d = deliveryMapper.selectById(id);
        if (d.getStatus() != DistDeliveryStatus.SHIPPED) {
            throw new BizException("dist.error.deliveryNotShipped", d.getDeliveryNo());
        }
        DistSupport.checkStoreAccess(d.getToStoreId(), null);
        LocalDate date = req == null || req.receiveDate() == null ? LocalDate.now() : req.receiveDate();
        if (date.isBefore(d.getBizDate())) {
            throw new BizException("dist.error.receiveBeforeShip", date, d.getBizDate());
        }
        Map<Long, ReceiveRequest.Line> lines = new HashMap<>();
        if (req != null && req.items() != null) {
            req.items().forEach(l -> lines.put(l.itemId(), l));
        }
        List<DistDeliveryItem> items = deliveryMapper.selectItems(id);
        if (!items.stream().map(DistDeliveryItem::getId).collect(Collectors.toSet()).containsAll(lines.keySet())) {
            throw new BizException("dist.error.itemNotInDocument");
        }
        moves.receive(d.getTransferId(), date);

        boolean differential = moves.differential(d);
        BigDecimal receivedTotal = BigDecimal.ZERO;
        boolean hasDiff = false;
        for (DistDeliveryItem i : items) {
            ReceiveRequest.Line l = lines.get(i.getId());
            BigDecimal received = l == null ? i.getQty() : l.receivedQty();
            int cmp = received.compareTo(i.getQty());
            if (cmp != 0 && Integer.valueOf(1).equals(i.getSerialEnabled())) {
                throw new BizException("dist.error.serialMustReceiveAll", i.getMaterialName());
            }
            itemMapper.update(Wrappers.<DistDeliveryItem>lambdaUpdate()
                    .set(DistDeliveryItem::getReceivedQty, received)
                    .eq(DistDeliveryItem::getId, i.getId()));
            receivedTotal = receivedTotal.add(received);
            if (cmp == 0) {
                continue;
            }
            hasDiff = true;
            DistDiscrepancy x = new DistDiscrepancy();
            x.setDeliveryId(id);
            x.setDeliveryItemId(i.getId());
            x.setMaterialId(i.getMaterialId());
            x.setBatchNo(i.getBatchNo());
            x.setQty(received.subtract(i.getQty()).abs());
            x.setPrice(i.getPrice());
            x.setAmount(i.getPrice() == null ? null : Decimals.money(x.getQty().multiply(i.getPrice())));
            x.setStatus(DiscrepancyStatus.PENDING);
            x.setReason(l == null ? null : l.reason());
            if (cmp < 0) {
                x.setKind(l != null && l.kind() == DiscrepancyKind.DAMAGED ? DiscrepancyKind.DAMAGED : DiscrepancyKind.SHORT);
                // 少收 / 破损：差异数量从收货仓库反向在途（调回发货仓库原库位），收货方库存 = 实收
                WmsStockDoc back = moves.transfer(d.getToWarehouseId(), d.getFromWarehouseId(), date, differential, true,
                        List.of(new DistStockMoves.Line(i.getMaterialId(), NO_LOCATION, i.getLocationId(), i.getBatchNo(), null,
                                x.getQty(), i.getPrice(), null)), d.getDeliveryNo(), null);
                x.setTransferId(back.getId());
            } else {
                x.setKind(DiscrepancyKind.OVER);
            }
            discrepancyMapper.insert(x);
        }
        BigDecimal receivedSum = receivedTotal;
        StatusTransitions.change(deliveryMapper, DistDelivery::getId, id, DistDelivery::getStatus,
                DistDeliveryStatus.SHIPPED, hasDiff ? DistDeliveryStatus.DIFF : DistDeliveryStatus.RECEIVED, w -> w
                        .set(DistDelivery::getReceivedQty, receivedSum)
                        .set(DistDelivery::getReceiveDate, date)
                        .set(DistDelivery::getReceiveBy, SecurityUtils.getUserId())
                        .set(DistDelivery::getReceiveTime, LocalDateTime.now())
                        .set(DistDelivery::getUpdateTime, LocalDateTime.now()),
                "dist.error.deliveryStatusChanged");
    }

    // ---------------------------------------------------------------- 差异处理使用

    public DistDelivery getAccessible(DeliveryKind kind, Long id) {
        return DataScopes.require(DistSupport.DELIVERY_RULE, () -> deliveryMapper.selectById(id),
                d -> kind == null || d.getKind() == kind, "dist.error.deliveryNotFound");
    }

    public List<DistDeliveryItem> items(Long deliveryId) {
        return itemMapper.selectList(Wrappers.<DistDeliveryItem>lambdaQuery()
                .eq(DistDeliveryItem::getDeliveryId, deliveryId).orderByAsc(DistDeliveryItem::getId));
    }

    // ---------------------------------------------------------------- 内部

    private DistDelivery getDraft(DeliveryKind kind, Long id) {
        DistDelivery d = getAccessible(kind, id);
        if (d.getStatus() != DistDeliveryStatus.DRAFT) {
            throw new BizException("dist.error.deliveryNotDraft", d.getDeliveryNo());
        }
        return d;
    }

    private static String labelOf(DeliveryKind kind) {
        return kind == DeliveryKind.DELIVERY ? "dist.label.delivery" : "dist.label.storeReturn";
    }

    /** 没有「查看成本金额」时不返回调出成本价；同价调拨（非加盟配送）的配送价就是成本，也不返回 */
    private void hideAmounts(DistDelivery d) {
        if (permissionService.hasPerm(COST_PERM)) {
            return;
        }
        boolean showPrice = d.getKind() == DeliveryKind.DELIVERY && d.getToStoreType() == StoreType.FRANCHISE;
        if (!showPrice) {
            d.setTotalAmount(null);
        }
        if (d.getItems() != null) {
            d.getItems().forEach(i -> {
                i.setCostPrice(null);
                if (!showPrice) {
                    i.setPrice(null);
                    i.setAmount(null);
                }
            });
        }
        if (d.getDiscrepancies() != null && !showPrice) {
            d.getDiscrepancies().forEach(x -> {
                x.setPrice(null);
                x.setAmount(null);
            });
        }
    }

    private record Header(Long fromStoreId, Long fromWarehouseId, Long toStoreId, Long toWarehouseId, StoreInfo toStore) {
        void apply(DistDelivery d) {
            d.setFromStoreId(fromStoreId);
            d.setFromWarehouseId(fromWarehouseId);
            d.setToStoreId(toStoreId);
            d.setToWarehouseId(toWarehouseId);
        }
    }

    /**
     * 配送：发货仓库须可使用且属于总部 / 配送中心，收货仓库为门店的仓库；
     * 退仓：发货仓库为可使用的门店仓库，收货仓库为总部 / 配送中心的仓库（为空取门店上级配送门店的默认仓库）
     */
    private Header header(DeliveryKind kind, DeliveryRequest req) {
        WmsWarehouse from = support.usableWarehouse(req.fromWarehouseId());
        WmsWarehouse to;
        if (kind == DeliveryKind.DELIVERY) {
            support.supplyStore(from);
            if (req.toWarehouseId() == null) {
                throw new BizException("dist.valid.toWarehouseRequired");
            }
            to = support.warehouse(req.toWarehouseId());
        } else {
            Long toId = req.toWarehouseId() != null ? req.toWarehouseId() : support.defaultSupplyWarehouse(support.store(from.getStoreId()));
            to = support.warehouse(toId);
            support.supplyStore(to);
        }
        if (Objects.equals(from.getId(), to.getId())) {
            throw new BizException("dist.error.sameWarehouse");
        }
        dateLocks.check(req.bizDate(), from.getId(), labelOf(kind));
        return new Header(from.getStoreId(), from.getId(), to.getStoreId(), to.getId(), support.store(to.getStoreId()));
    }

    /**
     * 明细：物料存在；来源要货单明细须属于可配送的要货单，且要货单的供货仓库、收货仓库与本单一致、物料相同，
     * 本单数量不超过批准的剩余数量（「超量配送」权限除外）；配送价需要「修改配送价」权限（没有时忽略）
     */
    private List<DistDeliveryItem> toItems(Long deliveryId, DeliveryKind kind, Header h, DeliveryRequest req) {
        Map<Long, WmsMaterial> materials = support.materials(req.items().stream().map(DeliveryRequest.Item::materialId).toList(), false);
        Map<Long, DistRequisitionItem> reqItems = new HashMap<>();
        Map<Long, DistRequisition> reqs = new HashMap<>();
        if (kind == DeliveryKind.DELIVERY) {
            List<Long> ids = req.items().stream().map(DeliveryRequest.Item::requisitionItemId).filter(Objects::nonNull).distinct().toList();
            requisitionService.itemsByIds(ids).forEach(i -> reqItems.put(i.getId(), i));
            requisitionService.byIds(reqItems.values().stream().map(DistRequisitionItem::getRequisitionId).distinct().toList())
                    .forEach(r -> reqs.put(r.getId(), r));
        }
        boolean allowOver = permissionService.hasPerm(OVER_QTY_PERM);
        boolean canPrice = kind == DeliveryKind.DELIVERY && permissionService.hasPerm(PRICE_PERM);
        Map<Long, BigDecimal> perReqItem = new HashMap<>();
        List<DistDeliveryItem> items = new ArrayList<>();
        for (DeliveryRequest.Item r : req.items()) {
            DistDeliveryItem i = new DistDeliveryItem();
            i.setDeliveryId(deliveryId);
            if (kind == DeliveryKind.DELIVERY && r.requisitionItemId() != null) {
                DistRequisitionItem ri = reqItems.get(r.requisitionItemId());
                DistRequisition rq = ri == null ? null : reqs.get(ri.getRequisitionId());
                if (ri == null || rq == null || !Objects.equals(ri.getMaterialId(), r.materialId())) {
                    throw new BizException("dist.error.requisitionItemNotFound");
                }
                if (!rq.getStatus().deliverable()) {
                    throw new BizException("dist.error.requisitionNotDeliverable", rq.getReqNo());
                }
                if (!Objects.equals(rq.getSupplyWarehouseId(), h.fromWarehouseId()) || !Objects.equals(rq.getWarehouseId(), h.toWarehouseId())) {
                    throw new BizException("dist.error.requisitionWarehouseMismatch", rq.getReqNo());
                }
                BigDecimal sum = perReqItem.merge(ri.getId(), r.qty(), BigDecimal::add);
                BigDecimal remaining = Decimals.nz(ri.getApprovedQty()).subtract(Decimals.nz(ri.getDeliveredQty()));
                if (!allowOver && sum.compareTo(remaining) > 0) {
                    throw new BizException("dist.error.overApproved", rq.getReqNo(), remaining);
                }
                i.setRequisitionId(rq.getId());
                i.setRequisitionItemId(ri.getId());
            }
            i.setMaterialId(materials.get(r.materialId()).getId());
            i.setLocationId(r.locationId() == null ? NO_LOCATION : r.locationId());
            i.setBatchNo(StringUtils.hasText(r.batchNo()) ? r.batchNo().trim() : "");
            i.setSerialNos(StringUtils.hasText(r.serialNos()) ? r.serialNos().trim() : null);
            i.setQty(r.qty());
            i.setPrice(canPrice ? r.price() : null);
            i.setRemark(r.remark());
            items.add(i);
        }
        return items;
    }
}
