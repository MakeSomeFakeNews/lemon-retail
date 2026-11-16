package com.lemon.erp.modules.dist.service;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.store.FranchiseCreditChecker;
import com.lemon.erp.framework.store.StoreType;
import com.lemon.erp.modules.dist.entity.DistDelivery;
import com.lemon.erp.modules.dist.model.DeliveryKind;
import com.lemon.erp.modules.scm.entity.ScmPriceSchemeItem;
import com.lemon.erp.modules.scm.service.ScmPriceSchemeService;
import com.lemon.erp.modules.store.entity.StoreInfo;
import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import com.lemon.erp.modules.wms.dto.StockDocRequest;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.model.StockOutType;
import com.lemon.erp.modules.wms.model.TransferPricing;
import com.lemon.erp.modules.wms.service.WmsStockDocService;
import com.lemon.erp.modules.wms.service.WmsStockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配送模块的库存移动，全部经 wms 的单据服务（生成即确认，来源单号为配送单号），不在本模块重复库存逻辑：
 * <ul>
 *   <li>发货：在途调拨（发货仓库调出，收货仓库收货后调入）；</li>
 *   <li>收货差异：少收 / 破损的数量生成反向的在途调拨（收货仓库 → 发货仓库），处理时收回、报损或再调给收货方；</li>
 *   <li>报损：其他出库（出库类别默认「配送差异报损」）。</li>
 * </ul>
 * 配送价规则：配送给加盟店时按差价调拨，调拨价 = 配送单明细上指定的价格 → 加盟店配送价方案的方案价（零售价列）→ 调出成本价；
 * 直营店、配送中心之间和门店退仓按同价调拨（调拨价 = 调出成本价）。成本按全公司移动加权平均，调拨价只是内部结算价，不改变库存成本
 */
@Component
@RequiredArgsConstructor
public class DistStockMoves {

    private final WmsStockDocService stockDocService;
    private final ScmPriceSchemeService schemeService;
    private final DistSupport support;
    private final WmsStockQueryService stockQueryService;
    private final ObjectProvider<FranchiseCreditChecker> creditChecker;

    /** 配送给加盟店：按配送价差价调拨 */
    public boolean differential(DistDelivery d) {
        return d.getKind() == DeliveryKind.DELIVERY && support.store(d.getToStoreId()).getType() == StoreType.FRANCHISE;
    }

    /** 加盟店配送价方案中的价格（方案的零售价列作为配送价），物料 ID → 价格；不是加盟店或没有方案时为空 */
    public Map<Long, BigDecimal> schemePrices(StoreInfo toStore, Collection<Long> materialIds) {
        Map<Long, BigDecimal> prices = new HashMap<>();
        if (toStore.getType() != StoreType.FRANCHISE || toStore.getDeliverySchemeId() == null) {
            return prices;
        }
        Map<Long, ScmPriceSchemeItem> items = schemeService.enabledPrices(toStore.getDeliverySchemeId(), materialIds);
        items.forEach((id, i) -> {
            if (i.getRetailPrice() != null) {
                prices.put(id, i.getRetailPrice());
            }
        });
        return prices;
    }

    /**
     * 加盟店的货款额度检查（要货单提交、配送单发货前）：金额 = 数量 × 配送价（指定价格 → 配送价方案价 → 当前移动加权均价，
     * 与发货时的调拨价规则一致），由连锁经营模块按预付余额 / 信用额度检查（{@link FranchiseCreditChecker}）。不是加盟店时不检查
     *
     * @param lines   物料 ID、数量、指定的价格（可为空）
     * @param docName 单据名称（消息键）
     */
    public void checkFranchiseCredit(StoreInfo toStore, List<PricedQty> lines, String docName) {
        FranchiseCreditChecker checker = creditChecker.getIfAvailable();
        if (checker == null || toStore.getType() != StoreType.FRANCHISE || lines.isEmpty()) {
            return;
        }
        List<Long> ids = lines.stream().map(PricedQty::materialId).distinct().toList();
        Map<Long, BigDecimal> scheme = schemePrices(toStore, ids);
        Map<Long, BigDecimal> costs = stockQueryService.unitCosts(ids);
        BigDecimal amount = BigDecimal.ZERO;
        for (PricedQty l : lines) {
            BigDecimal price = l.price() != null ? l.price() : scheme.getOrDefault(l.materialId(), costs.get(l.materialId()));
            amount = amount.add(Decimals.money(Decimals.nz(l.qty()).multiply(Decimals.nz(price))));
        }
        checker.check(toStore.getId(), amount, docName);
    }

    /** 额度检查的一行：物料、数量、指定的配送价（可为空） */
    public record PricedQty(Long materialId, BigDecimal qty, BigDecimal price) {
    }

    /**
     * 一行调拨 / 出库
     *
     * @param price 差价调拨的调拨价，为空按调出成本
     */
    public record Line(Long materialId, Long locationId, Long toLocationId, String batchNo, String serialNos, BigDecimal qty,
                       BigDecimal price, String remark) {
    }

    /**
     * 生成并确认调拨单（在调用方的事务中）
     *
     * @param inTransit 在途：只调出，收货时调入
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public WmsStockDoc transfer(Long fromWarehouseId, Long toWarehouseId, LocalDate bizDate, boolean differential, boolean inTransit,
                                List<Line> lines, String sourceNo, String remark) {
        List<StockDocItemRequest> items = lines.stream().map(l -> new StockDocItemRequest(l.materialId(), l.locationId(), l.toLocationId(),
                l.batchNo(), null, null, l.qty(), l.serialNos(), l.remark(), null, null, null, differential ? l.price() : null)).toList();
        StockDocRequest req = new StockDocRequest(null, fromWarehouseId, toWarehouseId, bizDate, remark, items, inTransit, null, null, null,
                differential ? TransferPricing.AVG_COST : TransferPricing.SAME, null, differential ? BigDecimal.ONE : null,
                null, null, null, null);
        return stockDocService.createConfirmed(StockBizType.TRANSFER, req, sourceNo);
    }

    /** 在途调拨收货（收货仓库入库），在调用方的事务中 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void receive(Long transferId, LocalDate date) {
        stockDocService.receiveGenerated(transferId, date);
    }

    /** 报损：其他出库并确认 */
    @Transactional(propagation = Propagation.MANDATORY)
    public WmsStockDoc writeOff(Long warehouseId, LocalDate bizDate, String ioCategory, List<Line> lines, String sourceNo, String remark) {
        List<StockDocItemRequest> items = lines.stream().map(l -> new StockDocItemRequest(l.materialId(), l.locationId(), null,
                l.batchNo(), null, null, l.qty(), l.serialNos(), l.remark())).toList();
        StockDocRequest req = new StockDocRequest(StockOutType.OTHER.name(), warehouseId, null, bizDate, remark, items, null, ioCategory);
        return stockDocService.createConfirmed(StockBizType.OUT, req, sourceNo);
    }
}
