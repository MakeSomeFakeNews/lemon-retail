package com.lemon.erp.modules.chain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.rule.BizRules;
import com.lemon.erp.framework.store.StoreType;
import com.lemon.erp.modules.chain.dto.ChainViews;
import com.lemon.erp.modules.chain.dto.FranchiseRequests;
import com.lemon.erp.modules.chain.entity.ChainReplenishParam;
import com.lemon.erp.modules.chain.mapper.ChainReplenishParamMapper;
import com.lemon.erp.modules.chain.mapper.ChainReportMapper;
import com.lemon.erp.modules.dist.dto.RequisitionRequest;
import com.lemon.erp.modules.dist.service.DistRequisitionService;
import com.lemon.erp.modules.store.entity.StoreInfo;
import com.lemon.erp.modules.store.service.StoreInfoService;
import com.lemon.erp.modules.wms.entity.WmsWarehouse;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * 门店补货建议（PU-05 规则版）：按门店的每个仓库 × 物料，
 * <pre>
 * 日均销量 = 近 N 天（业务参数 chain.replenish.salesDays，含今天）的销售净数量 ÷ N，销售 = POS 零售 + 销售出库，扣零售退货、销售退货（库存流水）
 * 目标库存 = 日均销量 × (到货天数 + 安全天数)（门店的补货参数，没有时取业务参数默认值）
 * 建议量 = 目标库存 − 现存 − 在途（调入该仓库的在途调拨）− 未完成的要货（草稿 / 审批中 / 待审核按要货数量，已审核 / 部分配送按 批准 − 已配送），向上取整，不足 0 不建议
 * </pre>
 * 直营店、加盟店可一键生成要货单（草稿，经配送模块，供货仓库为上级配送门店的默认仓库）；总部、配送中心向供应商采购，使用供应链的「补货建议」
 */
@Service
@RequiredArgsConstructor
public class ReplenishService {

    private final ChainReportMapper reportMapper;
    private final ChainReplenishParamMapper paramMapper;
    private final StoreInfoService storeService;
    private final WmsWarehouseService warehouseService;
    private final DistRequisitionService requisitionService;
    private final BizRules bizRules;

    public ChainViews.ReplenishParams params(Long storeId) {
        ChainSupport.checkStore(storeId);
        ChainReplenishParam p = paramMapper.selectOne(Wrappers.<ChainReplenishParam>lambdaQuery().eq(ChainReplenishParam::getStoreId, storeId));
        return new ChainViews.ReplenishParams(storeId, bizRules.number(ChainBizRules.SALES_DAYS), bizRules.number(ChainBizRules.LEAD_DAYS),
                bizRules.number(ChainBizRules.SAFETY_DAYS), p == null ? null : p.getLeadDays(), p == null ? null : p.getSafetyDays());
    }

    @Transactional
    public void saveParams(Long storeId, FranchiseRequests.ReplenishParam req) {
        ChainSupport.checkStore(storeId);
        storeService.get(storeId);
        ChainReplenishParam p = paramMapper.selectOne(Wrappers.<ChainReplenishParam>lambdaQuery().eq(ChainReplenishParam::getStoreId, storeId));
        if (p == null) {
            p = new ChainReplenishParam();
            p.setStoreId(storeId);
        }
        p.setLeadDays(req.leadDays());
        p.setSafetyDays(req.safetyDays());
        if (p.getId() == null) {
            paramMapper.insert(p);
        } else {
            paramMapper.updateById(p);
        }
    }

    /** 门店的补货建议（门店须可管辖），onlySuggest 为 true 时只返回建议量大于 0 的行 */
    public List<ChainViews.ReplenishRow> suggest(Long storeId, String keyword, boolean onlySuggest) {
        ChainViews.ReplenishParams params = params(storeId);
        StoreInfo store = storeService.get(storeId);
        List<Long> warehouses = warehouseService.idsOfStores(List.of(storeId));
        if (warehouses.isEmpty()) {
            return List.of();
        }
        int days = params.salesDays();
        int lead = params.storeLeadDays() != null ? params.storeLeadDays() : params.leadDays();
        int safety = params.storeSafetyDays() != null ? params.storeSafetyDays() : params.safetyDays();
        LocalDate end = LocalDate.now();
        LocalDate begin = end.minusDays(days - 1L);
        List<ChainViews.ReplenishRow> rows = reportMapper.selectReplenish(warehouses, begin, end, keyword);
        for (ChainViews.ReplenishRow r : rows) {
            r.setStoreName(store.getName());
            r.setStoreType(store.getType());
            r.setLeadDays(lead);
            r.setSafetyDays(safety);
            BigDecimal avg = Decimals.nz(r.getSoldQty()).divide(BigDecimal.valueOf(days), Decimals.QTY_SCALE, RoundingMode.HALF_UP);
            r.setAvgDaily(avg);
            BigDecimal target = avg.multiply(BigDecimal.valueOf((long) lead + safety));
            r.setTargetQty(Decimals.qty(target));
            BigDecimal s = target.subtract(Decimals.nz(r.getStockQty())).subtract(Decimals.nz(r.getTransitQty()))
                    .subtract(Decimals.nz(r.getRequisitionQty()));
            r.setSuggestQty(s.signum() > 0 ? s.setScale(0, RoundingMode.CEILING) : BigDecimal.ZERO);
        }
        return onlySuggest ? rows.stream().filter(r -> r.getSuggestQty().signum() > 0).toList() : rows;
    }

    /**
     * 按勾选的建议生成要货单（草稿）：仓库须属于直营店或加盟店，供货仓库取上级配送门店的默认仓库
     *
     * @return 要货单 ID
     */
    @Transactional
    public Long toRequisition(FranchiseRequests.ToRequisition req) {
        WmsWarehouse w = warehouseService.get(req.warehouseId());
        StoreInfo store = storeService.get(w.getStoreId());
        if (store.getType() == StoreType.HQ || store.getType() == StoreType.DC) {
            throw new BizException("chain.error.replenishUsePurchase", store.getName());
        }
        LocalDate today = LocalDate.now();
        List<RequisitionRequest.Item> items = req.items().stream()
                .map(l -> new RequisitionRequest.Item(l.materialId(), l.qty(), l.suggestQty(), null)).toList();
        return requisitionService.create(new RequisitionRequest(w.getId(), null, today,
                req.expectedDate() != null && !req.expectedDate().isBefore(today) ? req.expectedDate() : null,
                "补货建议生成", items)); // i18n-ignore 存入单据备注
    }
}
