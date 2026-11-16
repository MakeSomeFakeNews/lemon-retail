package com.lemon.erp.modules.dist.service;

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
import lombok.RequiredArgsConstructor;
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
