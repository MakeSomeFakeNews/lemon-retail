package com.lemon.erp.modules.scm.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.scm.dto.ReorderSuggestion;
import com.lemon.erp.modules.scm.dto.OrderItemRequest;
import com.lemon.erp.modules.scm.dto.OrderRequest;
import com.lemon.erp.modules.scm.entity.ScmSupplierPrice;
import com.lemon.erp.modules.scm.mapper.ScmPlanningMapper;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.OrderSource;
import com.lemon.erp.modules.wms.model.StockAlertMode;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 补货建议（再订货点法）：设置了再订货点的物料，预计库存不高于再订货点时建议补货，可一键按供应商生成采购订单（草稿）。
 * 按全公司计算：分仓库预警的物料安全库存取各仓库下限之和，并返回低于下限的仓库数（全公司够用时提示在仓库之间调拨）
 */
@Service
@RequiredArgsConstructor
public class ScmReorderService {

    private final ScmPlanningMapper planningMapper;
    private final ScmQueryService queryService;
    private final ScmOrderService orderService;
    private final WmsMaterialService materialService;

    /**
     * @param onlyNeeded 只返回需要补货的物料
     */
    public List<ReorderSuggestion> suggestions(String keyword, boolean onlyNeeded) {
        List<Map<String, Object>> rows = planningMapper.selectReorderMaterials(keyword);
        List<Long> ids = rows.stream().map(r -> ((Number) r.get("materialId")).longValue()).toList();
        Map<Long, ScmSupplierPrice> defaults = queryService.defaultSuppliers(ids);
        return rows.stream().map(r -> {
            Long id = ((Number) r.get("materialId")).longValue();
            BigDecimal available = dec(r.get("available"));
            BigDecimal poOpen = dec(r.get("poOpen"));
            BigDecimal soOpen = dec(r.get("soOpen"));
            BigDecimal projected = available.add(poOpen).subtract(soOpen);
            BigDecimal reorderPoint = dec(r.get("reorderPoint"));
            BigDecimal reorderQty = r.get("reorderQty") == null ? null : dec(r.get("reorderQty"));
            BigDecimal safety = dec(r.get("safetyStock"));
            ScmSupplierPrice price = defaults.get(id);
            BigDecimal suggest = BigDecimal.ZERO;
            if (projected.compareTo(reorderPoint) <= 0) {
                suggest = reorderPoint.add(safety).subtract(projected);
                if (reorderQty != null && reorderQty.compareTo(suggest) > 0) {
                    suggest = reorderQty;
                }
                if (price != null && price.getMinQty() != null && price.getMinQty().compareTo(suggest) > 0) {
                    suggest = price.getMinQty();
                }
                suggest = suggest.setScale(0, RoundingMode.CEILING);
            }
            return new ReorderSuggestion(id, (String) r.get("code"), (String) r.get("name"), (String) r.get("spec"), (String) r.get("unit"),
                    r.get("safetyStock") == null ? null : safety,
                    r.get("alertMode") == null ? StockAlertMode.TOTAL : StockAlertMode.valueOf(r.get("alertMode").toString()),
                    ((Number) r.get("lowWarehouses")).intValue(), reorderPoint, reorderQty,
                    available, poOpen, soOpen, projected, suggest,
                    price == null ? null : price.getSupplierId(), price == null ? null : price.getSupplierName(),
                    price == null ? null : queryService.effectivePrice(price, suggest));
        }).filter(s -> !onlyNeeded || s.suggestQty().signum() > 0).toList();
    }

    /**
     * 选中的补货建议生成采购订单（草稿）：按供应商（选择的供应商，为空取默认供应商）合并，单价取供货价格（阶梯价）
     *
     * @return 生成的采购订单 ID
     */
    @Transactional
    public List<Long> toOrders(List<Item> items, Long warehouseId) {
        if (items == null || items.isEmpty()) {
            throw new BizException("scm.valid.selectMaterialsReplenish");
        }
        Map<Long, ScmSupplierPrice> defaults = queryService.defaultSuppliers(items.stream().map(Item::materialId).toList());
        Map<Long, List<Item>> bySupplier = new LinkedHashMap<>();
        for (Item i : items) {
            Long supplier = i.supplierId() != null ? i.supplierId()
                    : defaults.containsKey(i.materialId()) ? defaults.get(i.materialId()).getSupplierId() : null;
            if (supplier == null) {
                throw new BizException("scm.error.materialHasNoSuggestedDefaultSupplier", materialService.get(i.materialId()).getName());
            }
            bySupplier.computeIfAbsent(supplier, k -> new ArrayList<>()).add(i);
        }
        List<Long> orderIds = new ArrayList<>();
        bySupplier.forEach((supplierId, list) -> {
            List<OrderItemRequest> lines = list.stream().map(i -> {
                ScmSupplierPrice p = queryService.supplierPrice(supplierId, i.materialId());
                BigDecimal price = p == null ? BigDecimal.ZERO : queryService.effectivePrice(p, i.qty());
                return new OrderItemRequest(i.materialId(), i.qty(), price, p == null ? null : p.getTaxRate(), null, i.remark());
            }).toList();
            orderIds.add(orderService.createDraft(OrderKind.PURCHASE, new OrderRequest(supplierId, LocalDate.now(), null, warehouseId,
                    "补货建议生成", lines), OrderSource.REORDER)); // i18n-ignore
        });
        return orderIds;
    }

    /**
     * 生成采购订单的一行
     *
     * @param supplierId 供应商，为空取默认供应商
     */
    public record Item(Long materialId, BigDecimal qty, Long supplierId, String remark) {
    }

    private static BigDecimal dec(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }
}
