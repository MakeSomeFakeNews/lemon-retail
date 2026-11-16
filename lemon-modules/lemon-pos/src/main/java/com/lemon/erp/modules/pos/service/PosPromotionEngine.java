package com.lemon.erp.modules.pos.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.store.StoreBrief;
import com.lemon.erp.framework.store.StoreLookup;
import com.lemon.erp.modules.pos.dto.PosViews;
import com.lemon.erp.modules.pos.entity.PosMember;
import com.lemon.erp.modules.pos.entity.PosPromotion;
import com.lemon.erp.modules.pos.entity.PosPromotionGift;
import com.lemon.erp.modules.pos.entity.PosPromotionGoods;
import com.lemon.erp.modules.pos.entity.PosPromotionTarget;
import com.lemon.erp.modules.pos.mapper.PosPromotionGiftMapper;
import com.lemon.erp.modules.pos.mapper.PosPromotionGoodsMapper;
import com.lemon.erp.modules.pos.mapper.PosPromotionMapper;
import com.lemon.erp.modules.pos.mapper.PosPromotionTargetMapper;
import com.lemon.erp.modules.pos.model.PosPriceSource;
import com.lemon.erp.modules.pos.model.PromotionKind;
import com.lemon.erp.modules.pos.model.PromotionRange;
import com.lemon.erp.modules.pos.model.PromotionThreshold;
import com.lemon.erp.modules.pos.model.SpecialScope;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.model.MaterialCategoryTree;
import com.lemon.erp.modules.wms.service.WmsMaterialCategoryService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 满减 / 满折 / 满赠的计算（{@link PosCartCalculator} 在取价、单行折扣之后调用，试算与结算相同）：
 * <ol>
 *   <li>候选：进行中、渠道为门店收银或全部、适用本门店（全部 / 指定门店 / 指定区域含下级），仅限会员的需要识别会员</li>
 *   <li>参与行：非赠品行中属于参与商品（全部 / 指定商品 / 指定分类含下级）的行；「与特价互斥」的活动不含特价行（特价是行级价格，先于整单活动）</li>
 *   <li>门槛：参与行的金额（取价、单行折扣后）或件数（录入数量）达到门槛；满减、满赠可「每满」叠加（倍数 = 取整(合计 ÷ 门槛)）</li>
 *   <li>优惠：满减 = 减额 × 倍数（不超过参与金额），满折 = 参与金额 × (100 − 折扣)%，满赠 = 赠品（按零售价估值，仅用于比较）</li>
 *   <li>每单只应用一个：先比优先级（大的优先），再比优惠金额，再取先建的</li>
 * </ol>
 * 满减 / 满折的优惠按参与行的金额比例分摊（最后一行取余数）；满赠生成单价 0 的赠品行（基本单位），随单出库扣库存
 */
@Component
@RequiredArgsConstructor
public class PosPromotionEngine {

    private final PosPromotionMapper promotionMapper;
    private final PosPromotionTargetMapper targetMapper;
    private final PosPromotionGoodsMapper goodsMapper;
    private final PosPromotionGiftMapper giftMapper;
    private final StoreLookup storeLookup;
    private final WmsMaterialCategoryService categoryService;
    private final WmsMaterialService materialService;

    /**
     * 应用结果
     *
     * @param shares   参与行的下标 → 分摊的优惠
     * @param gifts    赠品：物料 ID → 赠送数量（基本单位）
     */
    public record Applied(PosPromotion promotion, BigDecimal discount, Map<Integer, BigDecimal> shares, Map<Long, BigDecimal> gifts,
                          Map<Long, WmsMaterial> giftMaterials) {
    }

    /**
     * @param lines     已取价的行（不含赠品）
     * @param materials 行的物料
     * @return 应用的活动，没有满足的活动时为空
     */
    public Applied apply(StoreBrief store, PosMember member, List<PosViews.PricedLine> lines, Map<Long, WmsMaterial> materials,
                         LocalDateTime now) {
        boolean isMember = member != null && member.getStatus() == EnableStatus.ENABLED;
        List<PosPromotion> candidates = promotionMapper.selectActive(now).stream()
                .filter(p -> isMember || !Boolean.TRUE.equals(p.getMemberOnly())).toList();
        if (candidates.isEmpty() || lines.isEmpty()) {
            return null;
        }
        candidates = applicable(store, candidates);
        if (candidates.isEmpty()) {
            return null;
        }
        List<Long> ids = candidates.stream().map(PosPromotion::getId).toList();
        Map<Long, List<PosPromotionGoods>> goods = goodsMapper.selectByPromotionIds(ids).stream()
                .collect(Collectors.groupingBy(PosPromotionGoods::getPromotionId));
        Map<Long, List<PosPromotionGift>> gifts = giftMapper.selectByPromotionIds(ids).stream()
                .collect(Collectors.groupingBy(PosPromotionGift::getPromotionId));
        MaterialCategoryTree tree = candidates.stream().anyMatch(p -> p.getGoodsScope() == PromotionRange.CATEGORIES) ? categoryService.tree() : null;
        // 赠品（停用的赠品不赠送）：估值按零售价，只用于比较活动的优惠
        Map<Long, WmsMaterial> giftMaterials = materialService.mapByIds(gifts.values().stream().flatMap(List::stream)
                        .map(PosPromotionGift::getMaterialId).distinct().toList()).values().stream()
                .filter(m -> m.getStatus() == EnableStatus.ENABLED).collect(Collectors.toMap(WmsMaterial::getId, m -> m));

        Applied best = null;
        BigDecimal bestBenefit = null;
        for (PosPromotion p : candidates) {
            List<Integer> eligible = eligibleLines(p, lines, materials, goods.getOrDefault(p.getId(), List.of()), tree);
            if (eligible.isEmpty()) {
                continue;
            }
            BigDecimal baseAmount = eligible.stream().map(i -> lines.get(i).getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal measure = p.getThresholdType() == PromotionThreshold.QTY
                    ? eligible.stream().map(i -> lines.get(i).getQty()).reduce(BigDecimal.ZERO, BigDecimal::add) : baseAmount;
            if (measure.compareTo(p.getThreshold()) < 0) {
                continue;
            }
            BigDecimal times = Boolean.TRUE.equals(p.getStackable()) && p.getKind() != PromotionKind.DISCOUNT
                    ? measure.divide(p.getThreshold(), 0, RoundingMode.DOWN) : BigDecimal.ONE;
            BigDecimal discount = BigDecimal.ZERO;
            Map<Long, BigDecimal> giftQty = new HashMap<>();
            BigDecimal benefit;
            switch (p.getKind()) {
                case REDUCE -> discount = Decimals.money(p.getReduceAmount().multiply(times)).min(baseAmount);
                case DISCOUNT -> discount = Decimals.money(baseAmount.multiply(Decimals.HUNDRED.subtract(p.getDiscountRate()))
                        .divide(Decimals.HUNDRED, 6, RoundingMode.HALF_UP));
                case GIFT -> {
                    for (PosPromotionGift g : gifts.getOrDefault(p.getId(), List.of())) {
                        if (!giftMaterials.containsKey(g.getMaterialId())) {
                            continue;
                        }
                        giftQty.merge(g.getMaterialId(), g.getQty().multiply(times), BigDecimal::add);
                    }
                }
                default -> {
                }
            }
            if (p.getKind() == PromotionKind.GIFT) {
                if (giftQty.isEmpty()) {
                    continue;
                }
                benefit = giftQty.entrySet().stream()
                        .map(e -> e.getValue().multiply(Decimals.nz(giftMaterials.get(e.getKey()).getRetailPrice())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            } else {
                if (discount.signum() <= 0) {
                    continue;
                }
                benefit = discount;
            }
            if (best == null || priority(p) > priority(best.promotion())
                    || priority(p) == priority(best.promotion()) && benefit.compareTo(bestBenefit) > 0) {
                best = new Applied(p, discount, allocate(lines, eligible, discount, baseAmount), giftQty, giftMaterials);
                bestBenefit = benefit;
            }
        }
        return best;
    }

    private static int priority(PosPromotion p) {
        return p.getPriority() == null ? 0 : p.getPriority();
    }

    /** 参与的行（下标）：非赠品、属于参与商品，「与特价互斥」时不含特价行 */
    private static List<Integer> eligibleLines(PosPromotion p, List<PosViews.PricedLine> lines, Map<Long, WmsMaterial> materials,
                                               List<PosPromotionGoods> goods, MaterialCategoryTree tree) {
        Set<Long> ids = goods.stream().map(PosPromotionGoods::getMaterialId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> codes = goods.stream().map(PosPromotionGoods::getCategoryCode).filter(Objects::nonNull).collect(Collectors.toSet());
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            PosViews.PricedLine l = lines.get(i);
            if (l.isGift() || l.getAmount().signum() <= 0) {
                continue;
            }
            if (Boolean.TRUE.equals(p.getExcludeSpecial()) && l.getPriceSource() == PosPriceSource.SPECIAL) {
                continue;
            }
            boolean in = switch (p.getGoodsScope() == null ? PromotionRange.ALL : p.getGoodsScope()) {
                case ALL -> true;
                case MATERIALS -> ids.contains(l.getMaterialId());
                case CATEGORIES -> tree != null && PosMemberRuleService.inCategories(tree, materials.get(l.getMaterialId()), codes);
            };
            if (in) {
                result.add(i);
            }
        }
        return result;
    }

    /** 优惠按参与行的金额比例分摊，最后一行取余数 */
    private static Map<Integer, BigDecimal> allocate(List<PosViews.PricedLine> lines, List<Integer> eligible, BigDecimal discount, BigDecimal base) {
        Map<Integer, BigDecimal> shares = new HashMap<>();
        if (discount.signum() == 0) {
            return shares;
        }
        BigDecimal left = discount;
        for (int n = 0; n < eligible.size(); n++) {
            int i = eligible.get(n);
            BigDecimal s = n == eligible.size() - 1 ? left
                    : Decimals.money(discount.multiply(lines.get(i).getAmount()).divide(base, 6, RoundingMode.HALF_UP));
            left = left.subtract(s);
            shares.put(i, s);
        }
        return shares;
    }

    /** 适用本门店的活动（全部门店 / 指定门店 / 指定区域含下级区域） */
    private List<PosPromotion> applicable(StoreBrief store, List<PosPromotion> candidates) {
        List<Long> scoped = candidates.stream().filter(p -> p.getStoreScope() != SpecialScope.ALL).map(PosPromotion::getId).toList();
        Map<Long, List<Long>> targets = scoped.isEmpty() ? Map.of() : targetMapper.selectList(Wrappers.<PosPromotionTarget>lambdaQuery()
                        .in(PosPromotionTarget::getPromotionId, scoped)).stream()
                .collect(Collectors.groupingBy(PosPromotionTarget::getPromotionId, Collectors.mapping(PosPromotionTarget::getTargetId, Collectors.toList())));
        Map<Long, Set<Long>> regionStores = new HashMap<>();
        return candidates.stream().filter(p -> switch (p.getStoreScope()) {
            case ALL -> true;
            case STORES -> targets.getOrDefault(p.getId(), List.of()).contains(store.id());
            case REGIONS -> targets.getOrDefault(p.getId(), List.of()).stream()
                    .anyMatch(r -> regionStores.computeIfAbsent(r, storeLookup::storesInRegion).contains(store.id()));
        }).sorted(Comparator.comparing(PosPromotion::getId)).toList();
    }
}
