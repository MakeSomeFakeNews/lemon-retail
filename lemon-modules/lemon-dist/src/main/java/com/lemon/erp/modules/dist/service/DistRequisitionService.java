package com.lemon.erp.modules.dist.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.DetailLines;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.period.BizDateLocks;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataScopes;
import com.lemon.erp.framework.workflow.ApprovalFlow;
import com.lemon.erp.framework.workflow.ApprovalSupport;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.framework.workflow.FlowStatus;
import com.lemon.erp.modules.dist.dto.AuditRequest;
import com.lemon.erp.modules.dist.dto.DistQuery;
import com.lemon.erp.modules.dist.dto.DistViews;
import com.lemon.erp.modules.dist.dto.RequisitionRequest;
import com.lemon.erp.modules.dist.entity.DistRequisition;
import com.lemon.erp.modules.dist.entity.DistRequisitionItem;
import com.lemon.erp.modules.dist.mapper.DistRequisitionItemMapper;
import com.lemon.erp.modules.dist.mapper.DistRequisitionMapper;
import com.lemon.erp.modules.dist.model.RequisitionStatus;
import com.lemon.erp.modules.store.entity.StoreInfo;
import com.lemon.erp.modules.wms.entity.WmsWarehouse;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 门店要货单：草稿 → 提交（发布了「门店要货审批」流程时先审批）→ 待审核 → 总部 / 配送中心审核（可调整各行批准数量）→ 已审核
 * → 配送单发货回写已配送数量（部分配送 / 已配送）；已审核、部分配送的可以关闭。审批驳回、总部驳回回到草稿并记录原因。
 * <p>
 * 门店由收货仓库决定（只能为可管辖的门店要货），供货仓库默认取门店上级配送门店的默认仓库，必须属于总部或配送中心。
 * 数据权限按要货门店、供货门店的仓库过滤（{@link DistSupport#REQUISITION_RULE}）：门店只看到本店的要货，配送中心看到向它要货的单据
 */
@Service
@RequiredArgsConstructor
public class DistRequisitionService {

    public static final String PROCESS_KEY = "dist_requisition";
    private static final ApprovalFlow FLOW = new ApprovalFlow(PROCESS_KEY, "dist_requisition:");
    private static final String DOC_NAME = "dist.label.requisition";

    private final DistRequisitionMapper requisitionMapper;
    private final DistRequisitionItemMapper itemMapper;
    private final DistSupport support;
    private final DocNoService docNoService;
    private final ApprovalSupport approvalSupport;
    private final BizDateLocks dateLocks;

    @DataPermission(table = "dist_requisition", deptColumn = "", userColumn = "", warehouseColumn = "warehouse_id,supply_warehouse_id")
    public PageResult<DistRequisition> page(long current, long size, DistQuery query) {
        Page<DistRequisition> page = Page.of(current, size);
        requisitionMapper.selectPageList(page, query);
        return PageResult.of(page);
    }

    public DistRequisition detail(Long id) {
        DistRequisition r = getAccessible(id);
        DistRequisition d = requisitionMapper.selectDetail(id);
        d.setItems(requisitionMapper.selectItems(id, r.getWarehouseId(), r.getSupplyWarehouseId()));
        d.setItemCount(d.getItems().size());
        return d;
    }

    /**
     * 要货建议：各物料在收货仓库的下限（分仓库预警的设置，没有时取物料的安全库存）、现存、在途，建议量 = 下限 − 现存 − 在途（不小于 0），
     * 以及供货仓库的可用数量
     */
    public List<DistViews.SuggestRow> suggest(Long warehouseId, Long supplyWarehouseId, Collection<Long> materialIds) {
        if (materialIds == null || materialIds.isEmpty()) {
            return List.of();
        }
        WmsWarehouse w = support.warehouse(warehouseId);
        DistSupport.checkStoreAccess(w.getStoreId(), w.getName());
        Long supply = supplyWarehouseId;
        if (supply == null) {
            try {
                supply = support.defaultSupplyWarehouse(support.store(w.getStoreId()));
            } catch (BizException e) {
                supply = null;
            }
        }
        List<DistViews.SuggestRow> rows = requisitionMapper.selectSuggest(warehouseId, supply, materialIds);
        rows.forEach(r -> {
            BigDecimal s = Decimals.nz(r.getMinQty()).subtract(Decimals.nz(r.getStockQty())).subtract(Decimals.nz(r.getTransitQty()));
            r.setSuggestQty(s.signum() > 0 ? s : BigDecimal.ZERO);
        });
        return rows;
    }

    @Transactional
    public Long create(RequisitionRequest req) {
        Header h = header(req);
        DistRequisition r = new DistRequisition();
        r.setReqNo(docNoService.next(DistDocNos.REQUISITION));
        h.apply(r);
        r.setBizDate(req.bizDate());
        r.setExpectedDate(req.expectedDate());
        r.setRemark(req.remark());
        r.setStatus(RequisitionStatus.DRAFT);
        r.setTotalQty(Decimals.sum(req.items(), RequisitionRequest.Item::qty));
        r.setApprovedQty(BigDecimal.ZERO);
        r.setDeliveredQty(BigDecimal.ZERO);
        r.setDeptId(SecurityUtils.getLoginUser().deptId());
        requisitionMapper.insert(r);
        itemMapper.insertBatch(toItems(r.getId(), req));
        return r.getId();
    }

    @Transactional
    public void update(Long id, RequisitionRequest req) {
        DistRequisition old = getDraft(id);
        dateLocks.check(old.getBizDate(), old.getWarehouseId(), DOC_NAME);
        Header h = header(req);
        StatusTransitions.change(requisitionMapper, DistRequisition::getId, id, DistRequisition::getStatus,
                RequisitionStatus.DRAFT, RequisitionStatus.DRAFT, w -> w
                        .set(DistRequisition::getStoreId, h.storeId())
                        .set(DistRequisition::getWarehouseId, h.warehouseId())
                        .set(DistRequisition::getSupplyStoreId, h.supplyStoreId())
                        .set(DistRequisition::getSupplyWarehouseId, h.supplyWarehouseId())
                        .set(DistRequisition::getBizDate, req.bizDate())
                        .set(DistRequisition::getExpectedDate, req.expectedDate())
                        .set(DistRequisition::getRemark, req.remark())
                        .set(DistRequisition::getTotalQty, Decimals.sum(req.items(), RequisitionRequest.Item::qty))
                        .set(DistRequisition::getUpdateTime, LocalDateTime.now()),
                "dist.error.requisitionStatusChanged");
        DetailLines.replace(itemMapper, DistRequisitionItem::getRequisitionId, id, toItems(id, req),
                DistRequisitionItem::setRequisitionId, itemMapper::insertBatch);
    }

    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            DistRequisition r = getDraft(id);
            dateLocks.check(r.getBizDate(), r.getWarehouseId(), DOC_NAME);
            itemMapper.delete(Wrappers.<DistRequisitionItem>lambdaQuery().eq(DistRequisitionItem::getRequisitionId, id));
            requisitionMapper.deleteById(id);
        }
    }

    /**
     * 提交：发布了门店要货审批流程时发起审批（审批中），否则直接待审核
     *
     * @return 提交后的状态
     */
    @Transactional
    public RequisitionStatus submit(Long id) {
        DistRequisition r = getDraft(id);
        dateLocks.check(r.getBizDate(), r.getWarehouseId(), DOC_NAME);
        List<DistRequisitionItem> items = items(id);
        if (items.isEmpty()) {
            throw new BizException("dist.valid.itemsRequired");
        }
        approvalSupport.submit(FLOW, id, () -> Map.of("reqNo", r.getReqNo(), "storeName", support.store(r.getStoreId()).getName(),
                        "totalQty", r.getTotalQty(), "itemCount", items.size()),
                () -> toSubmitted(id, RequisitionStatus.DRAFT),
                () -> StatusTransitions.change(requisitionMapper, DistRequisition::getId, id, DistRequisition::getStatus,
                        RequisitionStatus.DRAFT, RequisitionStatus.APPROVING, w -> w
                                .set(DistRequisition::getRejectReason, null)
                                .set(DistRequisition::getSubmitTime, LocalDateTime.now()),
                        "dist.error.requisitionStatusChanged"),
                instanceId -> requisitionMapper.update(Wrappers.<DistRequisition>lambdaUpdate()
                        .set(DistRequisition::getFlowInstanceId, instanceId).eq(DistRequisition::getId, id)));
        return requisitionMapper.selectById(id).getStatus();
    }

    private void toSubmitted(Long id, RequisitionStatus from) {
        StatusTransitions.change(requisitionMapper, DistRequisition::getId, id, DistRequisition::getStatus,
                from, RequisitionStatus.SUBMITTED, w -> w
                        .set(DistRequisition::getRejectReason, null)
                        .set(DistRequisition::getSubmitTime, LocalDateTime.now()),
                "dist.error.requisitionStatusChanged");
    }

    /** 审批结束：通过 → 待审核；驳回、撤销 → 草稿 */
    @EventListener
    public void onFlowFinished(FlowFinishedEvent e) {
        FLOW.onFinishedWithStatus(e, (id, status) -> {
            DistRequisition r = requisitionMapper.selectById(id);
            if (r == null || r.getStatus() != RequisitionStatus.APPROVING) {
                return;
            }
            if (status == FlowStatus.APPROVED) {
                toSubmitted(id, RequisitionStatus.APPROVING);
            } else {
                StatusTransitions.tryChange(requisitionMapper, DistRequisition::getId, id, DistRequisition::getStatus,
                        RequisitionStatus.APPROVING, RequisitionStatus.DRAFT,
                        w -> w.set(DistRequisition::getRejectReason, status == FlowStatus.REJECTED ? "审批未通过" : null)); // i18n-ignore 存入单据
            }
        });
    }

    /**
     * 审核（总部 / 配送中心）：待审核 → 已审核，按请求调整各行批准数量（不传的行按要货数量），批准数量合计须大于 0。
     * 审核人须可管辖供货门店
     */
    @Transactional
    public void audit(Long id, AuditRequest req) {
        DistRequisition r = getAccessible(id);
        requisitionMapper.lockById(id);
        r = requisitionMapper.selectById(id);
        if (r.getStatus() != RequisitionStatus.SUBMITTED) {
            throw new BizException("dist.error.requisitionNotSubmitted", r.getReqNo());
        }
        DistSupport.checkStoreAccess(r.getSupplyStoreId(), null);
        Map<Long, BigDecimal> approved = new HashMap<>();
        if (req != null && req.items() != null) {
            req.items().forEach(l -> approved.put(l.itemId(), l.approvedQty()));
        }
        List<DistRequisitionItem> items = items(id);
        BigDecimal total = BigDecimal.ZERO;
        for (DistRequisitionItem i : items) {
            BigDecimal qty = approved.containsKey(i.getId()) ? approved.remove(i.getId()) : i.getQty();
            itemMapper.update(Wrappers.<DistRequisitionItem>lambdaUpdate()
                    .set(DistRequisitionItem::getApprovedQty, qty)
                    .set(DistRequisitionItem::getUpdateTime, LocalDateTime.now())
                    .eq(DistRequisitionItem::getId, i.getId()));
            total = total.add(qty);
        }
        if (!approved.isEmpty()) {
            throw new BizException("dist.error.itemNotInDocument");
        }
        if (total.signum() <= 0) {
            throw new BizException("dist.error.approvedQtyRequired");
        }
        BigDecimal approvedTotal = total;
        StatusTransitions.change(requisitionMapper, DistRequisition::getId, id, DistRequisition::getStatus,
                RequisitionStatus.SUBMITTED, RequisitionStatus.APPROVED, w -> w
                        .set(DistRequisition::getApprovedQty, approvedTotal)
                        .set(DistRequisition::getAuditBy, SecurityUtils.getUserId())
                        .set(DistRequisition::getAuditTime, LocalDateTime.now())
                        .set(DistRequisition::getUpdateTime, LocalDateTime.now()),
                "dist.error.requisitionStatusChanged");
    }

    /** 总部驳回：待审核 → 草稿，记录原因 */
    @Transactional
    public void reject(Long id, String reason) {
        DistRequisition r = getAccessible(id);
        DistSupport.checkStoreAccess(r.getSupplyStoreId(), null);
        if (r.getStatus() != RequisitionStatus.SUBMITTED) {
            throw new BizException("dist.error.requisitionNotSubmitted", r.getReqNo());
        }
        StatusTransitions.change(requisitionMapper, DistRequisition::getId, id, DistRequisition::getStatus,
                RequisitionStatus.SUBMITTED, RequisitionStatus.DRAFT, w -> w
                        .set(DistRequisition::getRejectReason, reason)
                        .set(DistRequisition::getUpdateTime, LocalDateTime.now()),
                "dist.error.requisitionStatusChanged");
    }

    /** 关闭：已审核、部分配送的不再配送剩余数量（草稿中的配送单发货时会被拒绝） */
    @Transactional
    public void close(Long id, String reason) {
        DistRequisition r = getAccessible(id);
        requisitionMapper.lockById(id);
        r = requisitionMapper.selectById(id);
        if (!r.getStatus().deliverable()) {
            throw new BizException("dist.error.requisitionNotDeliverable", r.getReqNo());
        }
        StatusTransitions.change(requisitionMapper, DistRequisition::getId, id, DistRequisition::getStatus,
                r.getStatus(), RequisitionStatus.CLOSED, w -> w
                        .set(DistRequisition::getCloseReason, reason)
                        .set(DistRequisition::getUpdateTime, LocalDateTime.now()),
                "dist.error.requisitionStatusChanged");
    }

    // ---------------------------------------------------------------- 配送单使用

    /** 在数据权限范围内的要货单 */
    public DistRequisition getAccessible(Long id) {
        return DataScopes.require(DistSupport.REQUISITION_RULE, () -> requisitionMapper.selectById(id), "dist.error.requisitionNotFound");
    }

    public List<DistRequisitionItem> items(Long requisitionId) {
        return itemMapper.selectList(Wrappers.<DistRequisitionItem>lambdaQuery()
                .eq(DistRequisitionItem::getRequisitionId, requisitionId).orderByAsc(DistRequisitionItem::getId));
    }

    public List<DistRequisitionItem> itemsByIds(Collection<Long> itemIds) {
        return itemIds.isEmpty() ? List.of() : itemMapper.selectByIds(itemIds);
    }

    public List<DistRequisition> byIds(Collection<Long> ids) {
        return ids.isEmpty() ? List.of() : requisitionMapper.selectByIds(ids);
    }

    /**
     * 配送单发货：回写已配送数量（行锁串行），更新要货单状态为部分配送 / 已配送。要货单须为可配送状态；
     * 超过批准的剩余数量时，没有「超量配送」权限报错
     *
     * @param deltas 要货单明细 ID → 本次发货数量
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void addDelivered(Map<Long, BigDecimal> deltas, boolean allowOver) {
        if (deltas.isEmpty()) {
            return;
        }
        Map<Long, DistRequisitionItem> items = new HashMap<>();
        itemMapper.selectByIds(deltas.keySet()).forEach(i -> items.put(i.getId(), i));
        items.values().stream().map(DistRequisitionItem::getRequisitionId).distinct().sorted().forEach(requisitionMapper::lockById);
        for (Map.Entry<Long, BigDecimal> e : deltas.entrySet()) {
            DistRequisitionItem i = itemMapper.selectById(e.getKey());
            if (i == null) {
                throw new BizException("dist.error.requisitionItemNotFound");
            }
            DistRequisition r = requisitionMapper.selectById(i.getRequisitionId());
            if (!r.getStatus().deliverable()) {
                throw new BizException("dist.error.requisitionNotDeliverable", r.getReqNo());
            }
            BigDecimal delivered = Decimals.nz(i.getDeliveredQty()).add(e.getValue());
            if (!allowOver && delivered.compareTo(Decimals.nz(i.getApprovedQty())) > 0) {
                throw new BizException("dist.error.overApproved", r.getReqNo(), Decimals.nz(i.getApprovedQty()).subtract(Decimals.nz(i.getDeliveredQty())));
            }
            itemMapper.update(Wrappers.<DistRequisitionItem>lambdaUpdate()
                    .set(DistRequisitionItem::getDeliveredQty, delivered)
                    .set(DistRequisitionItem::getUpdateTime, LocalDateTime.now())
                    .eq(DistRequisitionItem::getId, i.getId()));
        }
        items.values().stream().map(DistRequisitionItem::getRequisitionId).distinct().forEach(this::refreshDelivered);
    }

    /** 按明细重算已配送合计与状态（批准数量全部配送为已配送，否则部分配送） */
    private void refreshDelivered(Long requisitionId) {
        List<DistRequisitionItem> items = items(requisitionId);
        BigDecimal delivered = Decimals.sum(items, DistRequisitionItem::getDeliveredQty);
        boolean done = items.stream().allMatch(i -> Decimals.nz(i.getDeliveredQty()).compareTo(Decimals.nz(i.getApprovedQty())) >= 0);
        requisitionMapper.update(Wrappers.<DistRequisition>lambdaUpdate()
                .set(DistRequisition::getDeliveredQty, delivered)
                .set(DistRequisition::getStatus, done ? RequisitionStatus.DELIVERED : RequisitionStatus.PARTIAL)
                .set(DistRequisition::getUpdateTime, LocalDateTime.now())
                .eq(DistRequisition::getId, requisitionId)
                .in(DistRequisition::getStatus, RequisitionStatus.APPROVED, RequisitionStatus.PARTIAL));
    }

    // ---------------------------------------------------------------- 内部

    private DistRequisition getDraft(Long id) {
        DistRequisition r = getAccessible(id);
        if (r.getStatus() != RequisitionStatus.DRAFT) {
            throw new BizException("dist.error.requisitionNotDraft", r.getReqNo());
        }
        return r;
    }

    private record Header(Long storeId, Long warehouseId, Long supplyStoreId, Long supplyWarehouseId) {
        void apply(DistRequisition r) {
            r.setStoreId(storeId);
            r.setWarehouseId(warehouseId);
            r.setSupplyStoreId(supplyStoreId);
            r.setSupplyWarehouseId(supplyWarehouseId);
        }
    }

    /** 校验收货仓库（可管辖、可开单）、供货仓库（总部或配送中心，默认取上级配送门店的默认仓库）、日期锁定、物料 */
    private Header header(RequisitionRequest req) {
        WmsWarehouse w = support.usableWarehouse(req.warehouseId());
        StoreInfo store = support.store(w.getStoreId());
        Long supplyWarehouseId = req.supplyWarehouseId() != null ? req.supplyWarehouseId() : support.defaultSupplyWarehouse(store);
        WmsWarehouse sw = support.warehouse(supplyWarehouseId);
        StoreInfo supply = support.supplyStore(sw);
        if (Objects.equals(sw.getId(), w.getId())) {
            throw new BizException("dist.error.sameWarehouse");
        }
        if (req.expectedDate() != null && req.expectedDate().isBefore(req.bizDate())) {
            throw new BizException("dist.error.expectedBeforeBizDate");
        }
        dateLocks.check(req.bizDate(), w.getId(), DOC_NAME);
        support.materials(req.items().stream().map(RequisitionRequest.Item::materialId).toList(), true);
        return new Header(store.getId(), w.getId(), supply.getId(), sw.getId());
    }

    private static List<DistRequisitionItem> toItems(Long requisitionId, RequisitionRequest req) {
        return req.items().stream().map(r -> {
            DistRequisitionItem i = new DistRequisitionItem();
            i.setRequisitionId(requisitionId);
            i.setMaterialId(r.materialId());
            i.setQty(r.qty());
            i.setSuggestQty(r.suggestQty());
            i.setRemark(r.remark());
            return i;
        }).toList();
    }
}
