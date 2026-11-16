package com.lemon.erp.modules.dist.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.modules.dist.dto.DistQuery;
import com.lemon.erp.modules.dist.dto.DistViews;
import com.lemon.erp.modules.dist.entity.DistStoreLedger;
import com.lemon.erp.modules.dist.mapper.DistDeliveryMapper;
import com.lemon.erp.modules.dist.mapper.DistStoreLedgerMapper;
import com.lemon.erp.modules.wms.event.TransferSettledEvent;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 门店往来（TR-04）与在途查询。
 * <p>
 * 门店往来：跨门店的差价调拨（手工的差价调拨单、加盟店配送单生成的调拨）确认时，按调拨金额（内部结算价 × 数量）记一笔
 * 「调出门店应收、调入门店应付」，冲销时记一笔负数；平价调拨（调拨价 = 调出成本）不产生往来。只用于门店之间的内部对账与考核，
 * 不生成凭证、不影响存货成本（成本按全公司移动加权平均）。加盟店的货款结算在 M4 按配送单的配送金额处理。
 * <p>
 * 在途查询：已调出未收货的全部调拨单（配送、退仓、收货差异和手工的在途调拨），在途天数 = 当天 − 调出日期
 */
@Service
@RequiredArgsConstructor
public class DistLedgerService {

    private final DistStoreLedgerMapper ledgerMapper;
    private final DistDeliveryMapper deliveryMapper;
    private final WmsWarehouseService warehouseService;

    /** 调拨单确认 / 冲销后（同一事务）：跨门店的差价调拨记门店往来 */
    @EventListener
    public void onTransferSettled(TransferSettledEvent e) {
        if (!e.differential() || e.toWarehouseId() == null) {
            return;
        }
        Map<Long, Long> stores = warehouseService.storeIds(List.of(e.warehouseId(), e.toWarehouseId()));
        Long from = stores.get(e.warehouseId());
        Long to = stores.get(e.toWarehouseId());
        if (from == null || to == null || Objects.equals(from, to)) {
            return;
        }
        DistStoreLedger l = new DistStoreLedger();
        l.setTransferId(e.docId());
        l.setTransferNo(e.docNo());
        l.setSourceNo(e.sourceNo());
        l.setBizDate(e.bizDate());
        l.setFromStoreId(from);
        l.setToStoreId(to);
        l.setFromWarehouseId(e.warehouseId());
        l.setToWarehouseId(e.toWarehouseId());
        l.setCostAmount(e.costAmount());
        l.setAmount(e.transferAmount() == null ? BigDecimal.ZERO : e.transferAmount());
        ledgerMapper.insert(l);
    }

    /** 门店往来明细：用户限定门店时只看到调出或调入为可管辖门店的记录 */
    @DataPermission(table = "dist_store_ledger", deptColumn = "", userColumn = "", warehouseColumn = "from_warehouse_id,to_warehouse_id")
    public PageResult<DistStoreLedger> page(long current, long size, DistQuery query) {
        Page<DistStoreLedger> page = Page.of(current, size);
        ledgerMapper.selectPageList(page, query);
        return PageResult.of(page);
    }

    @DataPermission(table = "dist_store_ledger", deptColumn = "", userColumn = "", warehouseColumn = "from_warehouse_id,to_warehouse_id")
    public List<DistViews.LedgerSummaryRow> summary(DistQuery query) {
        return ledgerMapper.selectSummary(query);
    }

    /** 在途调拨：按调出、调入仓库所属门店过滤 */
    @DataPermission(table = "wms_stock_doc", deptColumn = "", userColumn = "", warehouseColumn = "warehouse_id,to_warehouse_id")
    public PageResult<DistViews.TransitRow> transit(long current, long size, DistQuery query) {
        Page<DistViews.TransitRow> page = Page.of(current, size);
        deliveryMapper.selectTransit(page, query);
        LocalDate today = LocalDate.now();
        page.getRecords().forEach(r -> r.setDays(r.getBizDate() == null ? null : (int) ChronoUnit.DAYS.between(r.getBizDate(), today)));
        return PageResult.of(page);
    }

    @DataPermission(table = "wms_stock_doc", deptColumn = "", userColumn = "", warehouseColumn = "warehouse_id,to_warehouse_id")
    public DistViews.TransitTotal transitTotal(DistQuery query) {
        return deliveryMapper.selectTransitTotal(query);
    }
}
