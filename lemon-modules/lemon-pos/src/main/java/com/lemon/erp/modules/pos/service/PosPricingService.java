package com.lemon.erp.modules.pos.service;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.store.StoreBrief;
import com.lemon.erp.framework.store.StoreLookup;
import com.lemon.erp.modules.pos.entity.PosMember;
import com.lemon.erp.modules.pos.entity.PosSpecialTarget;
import com.lemon.erp.modules.pos.mapper.PosSpecialMapper;
import com.lemon.erp.modules.pos.mapper.PosSpecialTargetMapper;
import com.lemon.erp.modules.pos.model.PosPriceSource;
import com.lemon.erp.modules.pos.model.SpecialScope;
import com.lemon.erp.modules.scm.entity.ScmPriceSchemeItem;
import com.lemon.erp.modules.scm.service.ScmPriceSchemeService;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * POS 取价（价格均为基本单位、含税）。取价顺序（需求 §14-5）：
 * <ol>
 *   <li>特价活动：进行中、渠道为门店收银或全部、适用本门店（全部门店 / 指定门店 / 指定区域含下级区域），仅限会员的活动需要识别会员；
 *       同一商品命中多个活动时取最低价（折扣型按门店价 × 折扣计算后比较）</li>
 *   <li>会员价：识别了启用的会员时，门店价格方案中的会员价与 门店价 × 会员等级折扣 取低者（都没有时跳过）</li>
 *   <li>门店价格方案的零售价（门店绑定的启用方案中有该商品时）</li>
 *   <li>总部零售价（物料资料的零售价）</li>
 * </ol>
 * 「门店价」= 3 或 4，即原价（list price）。特价有每单限购时，超出部分按会员价 / 门店价（fallback）计算
 */
@Service
@RequiredArgsConstructor
public class PosPricingService {

    private final ScmPriceSchemeService schemeService;
    private final PosSpecialMapper specialMapper;
    private final PosSpecialTargetMapper targetMapper;
    private final StoreLookup storeLookup;

    /**
     * 一个物料的取价结果（基本单位）
     *
     * @param listPrice 原价（门店价），没有价格时为空
     * @param price     取价结果，没有价格时为空（需要手工输入）
     * @param fallback  特价限购超出部分的单价（会员价 / 门店价）
     * @param limitQty  特价每单限购（基本单位），为空不限
     */
    public record Price(BigDecimal listPrice, BigDecimal price, PosPriceSource source, Long specialId, BigDecimal fallback,
                        BigDecimal limitQty) {
    }

    /**
     * @param member 识别的会员（停用的会员不享受会员价、会员特价），可为空
     * @return 物料 ID → 取价结果
     */
    public Map<Long, Price> prices(StoreBrief store, PosMember member, Map<Long, WmsMaterial> materials, LocalDateTime now) {
        Set<Long> ids = materials.keySet();
        if (ids.isEmpty()) {
            return Map.of();
        }
        boolean isMember = member != null && member.getStatus() == EnableStatus.ENABLED;
        Map<Long, ScmPriceSchemeItem> scheme = schemeService.enabledPrices(store.priceSchemeId(), ids);
        Map<Long, List<PosSpecialMapper.Candidate>> specials = applicableSpecials(store, ids, now, isMember);
        BigDecimal levelRate = isMember && member.getDiscountRate() != null ? member.getDiscountRate() : Decimals.HUNDRED;
        Map<Long, Price> result = new HashMap<>();
        for (WmsMaterial m : materials.values()) {
            ScmPriceSchemeItem s = scheme.get(m.getId());
            BigDecimal schemeRetail = s == null ? null : s.getRetailPrice();
            BigDecimal list = schemeRetail != null ? schemeRetail : m.getRetailPrice();
            PosPriceSource listSource = schemeRetail != null ? PosPriceSource.SCHEME : PosPriceSource.RETAIL;
            // 会员价：方案会员价与 门店价 × 等级折扣 取低者
            BigDecimal memberPrice = null;
            if (isMember) {
                BigDecimal byScheme = s == null ? null : s.getMemberPrice();
                BigDecimal byLevel = list != null && levelRate.compareTo(Decimals.HUNDRED) < 0 ? rate(list, levelRate) : null;
                memberPrice = min(byScheme, byLevel);
            }
            BigDecimal fallback = memberPrice != null ? memberPrice : list;
            PosPriceSource fallbackSource = memberPrice != null ? PosPriceSource.MEMBER : listSource;
            // 特价：命中的活动中取最低价
            PosSpecialMapper.Candidate best = null;
            BigDecimal bestPrice = null;
            for (PosSpecialMapper.Candidate c : specials.getOrDefault(m.getId(), List.of())) {
                BigDecimal p = c.getSpecialPrice() != null ? c.getSpecialPrice() : list == null ? null : rate(list, c.getDiscountRate());
                if (p != null && (bestPrice == null || p.compareTo(bestPrice) < 0)) {
                    best = c;
                    bestPrice = p;
                }
            }
            if (best != null) {
                result.put(m.getId(), new Price(list, bestPrice, PosPriceSource.SPECIAL, best.getSpecialId(), fallback, best.getLimitQty()));
            } else {
                result.put(m.getId(), new Price(list, fallback, fallback == null ? null : fallbackSource, null, fallback, null));
            }
        }
        return result;
    }

    /** 进行中、适用本门店的特价（物料 ID → 候选），仅限会员的活动只在识别了会员时参与 */
    private Map<Long, List<PosSpecialMapper.Candidate>> applicableSpecials(StoreBrief store, Collection<Long> materialIds, LocalDateTime now,
                                                                           boolean isMember) {
        List<PosSpecialMapper.Candidate> candidates = specialMapper.selectCandidates(materialIds, now).stream()
                .filter(c -> isMember || c.getMemberOnly() == null || c.getMemberOnly() == 0)
                .toList();
        if (candidates.isEmpty()) {
            return Map.of();
        }
        List<Long> scoped = candidates.stream().filter(c -> c.getStoreScope() != SpecialScope.ALL)
                .map(PosSpecialMapper.Candidate::getSpecialId).distinct().toList();
        Map<Long, List<Long>> targets = scoped.isEmpty() ? Map.of() : targetMapper.selectList(Wrappers.<PosSpecialTarget>lambdaQuery()
                        .in(PosSpecialTarget::getSpecialId, scoped)).stream()
                .collect(Collectors.groupingBy(PosSpecialTarget::getSpecialId, Collectors.mapping(PosSpecialTarget::getTargetId, Collectors.toList())));
        // 区域内的门店（含下级区域），同一次取价中每个区域只查一次
        Map<Long, Set<Long>> regionStores = new HashMap<>();
        return candidates.stream().filter(c -> switch (c.getStoreScope()) {
            case ALL -> true;
            case STORES -> targets.getOrDefault(c.getSpecialId(), List.of()).contains(store.id());
            case REGIONS -> targets.getOrDefault(c.getSpecialId(), List.of()).stream()
                    .anyMatch(r -> regionStores.computeIfAbsent(r, storeLookup::storesInRegion).contains(store.id()));
        }).collect(Collectors.groupingBy(PosSpecialMapper.Candidate::getMaterialId));
    }

    /** 按折扣（%）计算的价格，保留 2 位小数 */
    static BigDecimal rate(BigDecimal price, BigDecimal rate) {
        return price.multiply(rate).divide(Decimals.HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal min(BigDecimal a, BigDecimal b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.min(b);
    }
}
