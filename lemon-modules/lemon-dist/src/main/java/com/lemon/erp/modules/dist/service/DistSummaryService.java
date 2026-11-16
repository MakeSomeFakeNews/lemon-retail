package com.lemon.erp.modules.dist.service;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.modules.dist.dto.DistViews;
import com.lemon.erp.modules.dist.dto.PurchaseRequest;
import com.lemon.erp.modules.dist.mapper.DistRequisitionMapper;
import com.lemon.erp.modules.scm.entity.ScmSupplierPrice;
import com.lemon.erp.modules.scm.model.OrderSource;
import com.lemon.erp.modules.scm.service.ScmQueryService;
import com.lemon.erp.modules.scm.service.ScmReorderService;
import com.lemon.erp.modules.wms.entity.WmsWarehouse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 要货汇总（总部 / 配送中心）：已审核、部分配送的要货单中待配送的数量按 供货仓库 + 物料 汇总，对照供货仓库的可用库存得出缺货；
 * 缺货可按物料的默认供应商（或选择的供应商）拆分生成采购订单草稿（PU-04，复用补货建议的 {@link ScmReorderService#toOrders}，来源「门店要货」）
 */
@Service
@RequiredArgsConstructor
public class DistSummaryService {

    private final DistRequisitionMapper requisitionMapper;
    private final ScmQueryService scmQueryService;
    private final ScmReorderService reorderService;
    private final DistSupport support;

    @DataPermission(table = "dist_requisition", deptColumn = "", userColumn = "", warehouseColumn = "warehouse_id,supply_warehouse_id")
    public List<DistViews.SummaryRow> summary(Long supplyWarehouseId, String keyword) {
        List<DistViews.SummaryRow> rows = requisitionMapper.selectSummary(supplyWarehouseId, keyword == null ? null : keyword.trim());
        Map<Long, ScmSupplierPrice> suppliers = rows.isEmpty() ? Map.of()
                : scmQueryService.defaultSuppliers(rows.stream().map(DistViews.SummaryRow::getMaterialId).distinct().toList());
        rows.forEach(r -> {
            BigDecimal shortage = Decimals.nz(r.getPendingQty()).subtract(Decimals.nz(r.getAvailable()));
            r.setShortage(shortage.signum() > 0 ? shortage : BigDecimal.ZERO);
            ScmSupplierPrice p = suppliers.get(r.getMaterialId());
            if (p != null) {
                r.setSupplierId(p.getSupplierId());
                r.setSupplierName(p.getSupplierName());
            }
        });
        return rows;
    }

    /**
     * 缺货生成采购订单（草稿）：收货仓库为供货仓库（须可使用、属于总部 / 配送中心），按供应商合并
     *
     * @return 采购订单 ID
     */
    @Transactional
    public List<Long> toPurchaseOrders(PurchaseRequest req) {
        WmsWarehouse w = support.usableWarehouse(req.warehouseId());
        support.supplyStore(w);
        List<ScmReorderService.Item> items = req.items().stream()
                .map(l -> new ScmReorderService.Item(l.materialId(), l.qty(), l.supplierId(), null)).toList();
        return reorderService.toOrders(items, w.getId(), OrderSource.REQUISITION, "门店要货汇总生成"); // i18n-ignore 存入订单备注
    }
}
