package com.lemon.erp.modules.pos.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.rule.BizRules;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.store.StoreBrief;
import com.lemon.erp.modules.pos.dto.PosRequests;
import com.lemon.erp.modules.pos.dto.PosViews;
import com.lemon.erp.modules.pos.entity.PosMember;
import com.lemon.erp.modules.pos.model.PosPriceSource;
import com.lemon.erp.modules.pos.model.PosRounding;
import com.lemon.erp.modules.scm.entity.ScmPriceControl;
import com.lemon.erp.modules.scm.model.PriceControlMode;
import com.lemon.erp.modules.scm.service.ScmPriceControlService;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsMaterialUnit;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 购物车计算（试算与结算共用，结算时以服务端计算为准，不信任前端金额）：
 * <ol>
 *   <li>每行按 {@link PosPricingService} 取价（录入单位的价格 = 基本单位价 × 换算率）；手工改价需要 {@code pos:sale:price}；
 *       特价超出每单限购的部分按会员价 / 门店价计算</li>
 *   <li>单行折扣、整单优惠需要 {@code pos:sale:discount}；手工优惠（改价、单行折扣、整单优惠）占取价金额的比例不能超过业务参数
 *       {@code pos.discount.maxRate}，有 {@code pos:sale:discountAny} 时不受限</li>
 *   <li>满减 / 满折 / 满赠（{@link PosPromotionEngine}）：在取价（含特价）、单行折扣之后按整单计算，每单只应用一个，优惠按参与行金额分摊
 *       （{@code promoShare}），赠品行单价 0 追加在末尾</li>
 *   <li>应收 = 商品金额 - 满减优惠 - 整单优惠，再按抹零规则（{@code pos.rounding}）处理；整单优惠与抹零按扣除满减后的行金额比例分摊到非赠品行（最后一行取余数）</li>
 *   <li>最低售价：按分摊后的实收单价与供应链「价格控制」的最低销售价比较，控制方式为「禁止」时需要 {@code pos:sale:belowMin}，「提示」时只标记</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class PosCartCalculator {

    static final String PRICE_PERM = "pos:sale:price";
    static final String DISCOUNT_PERM = "pos:sale:discount";
    static final String DISCOUNT_ANY_PERM = "pos:sale:discountAny";
    static final String BELOW_MIN_PERM = "pos:sale:belowMin";

    private final PosPricingService pricingService;
    private final PosPromotionEngine promotionEngine;
    private final WmsMaterialService materialService;
    private final ScmPriceControlService priceControlService;
    private final PermissionService permissionService;
    private final BizRules bizRules;

    /** 计算结果：试算视图 + 物料（结算生成出库单使用） */
    public record Cart(PosViews.Preview preview, Map<Long, WmsMaterial> materials) {
    }

    public Cart compute(StoreBrief store, PosMember member, List<PosRequests.SaleLine> lines, BigDecimal orderDiscountReq) {
        List<Long> ids = lines.stream().map(PosRequests.SaleLine::materialId).distinct().toList();
        Map<Long, WmsMaterial> materials = materialService.getEnabled(ids);
        materials.values().forEach(PosCartCalculator::checkSellable);
        LocalDateTime now = LocalDateTime.now();
        Map<Long, PosPricingService.Price> prices = pricingService.prices(store, member, materials, now);
        Map<Long, List<WmsMaterialUnit>> unitCache = new HashMap<>();

        List<PosViews.PricedLine> priced = new ArrayList<>(lines.size());
        BigDecimal autoTotal = BigDecimal.ZERO;
        BigDecimal listTotal = BigDecimal.ZERO;
        for (int i = 0; i < lines.size(); i++) {
            PosRequests.SaleLine l = lines.get(i);
            WmsMaterial m = materials.get(l.materialId());
            String unit = StringUtils.hasText(l.unit()) ? l.unit() : m.getUnit();
            BigDecimal ratio = ratio(m, unit, unitCache);
            BigDecimal qty = l.qty().setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
            BigDecimal baseQty = qty.multiply(ratio).setScale(4, RoundingMode.HALF_UP);
            PosPricingService.Price p = prices.get(m.getId());
            BigDecimal autoPrice = unitPrice(p.price(), ratio);
            BigDecimal listPrice = unitPrice(p.listPrice(), ratio);

            PosViews.PricedLine line = new PosViews.PricedLine();
            line.setLineNo(i + 1);
            line.setMaterialId(m.getId());
            line.setMaterialCode(m.getCode());
            line.setMaterialName(m.getName());
            line.setSpec(m.getSpec());
            line.setUnit(unit);
            line.setUnitRatio(ratio);
            line.setQty(qty);
            line.setBaseQty(baseQty);
            line.setSerialNos(l.serialNos());
            line.setRemark(l.remark());

            BigDecimal gross;
            BigDecimal autoGross;
            boolean manual = l.price() != null && (autoPrice == null || l.price().compareTo(autoPrice) != 0);
            if (manual) {
                requirePerm(PRICE_PERM, "pos.error.noPricePerm");
                line.setPrice(l.price());
                line.setPriceSource(PosPriceSource.MANUAL);
                gross = money(qty.multiply(l.price()));
                autoGross = autoPrice == null ? gross : lineAmount(p, qty, baseQty, ratio, autoPrice);
            } else {
                if (autoPrice == null) {
                    throw new BizException("pos.error.priceMissing", m.getCode(), m.getName());
                }
                line.setPrice(autoPrice);
                line.setPriceSource(p.source());
                line.setSpecialId(p.specialId());
                line.setLimitQty(p.limitQty());
                gross = lineAmount(p, qty, baseQty, ratio, autoPrice);
                autoGross = gross;
            }
            line.setListPrice(listPrice != null ? listPrice : line.getPrice());
            BigDecimal rate = l.discountRate() == null ? Decimals.HUNDRED : l.discountRate();
            if (rate.compareTo(Decimals.HUNDRED) < 0) {
                requirePerm(DISCOUNT_PERM, "pos.error.noDiscountPerm");
            }
            BigDecimal amount = rate.compareTo(Decimals.HUNDRED) < 0 ? money(gross.multiply(rate).divide(Decimals.HUNDRED, 6, RoundingMode.HALF_UP)) : gross;
            line.setDiscountRate(rate);
            line.setAmount(amount);
            line.setSalePrice(qty.signum() == 0 ? amount : amount.divide(qty, 4, RoundingMode.HALF_UP));
            priced.add(line);
            autoTotal = autoTotal.add(autoGross);
            listTotal = listTotal.add(money(qty.multiply(line.getListPrice())));
        }

        // 满减 / 满折 / 满赠：在特价（行级取价）、单行折扣之后按整单计算，每单只应用一个
        PosPromotionEngine.Applied promo = promotionEngine.apply(store, member, priced, materials, now);
        BigDecimal promoAmount = BigDecimal.ZERO;
        Map<Long, WmsMaterial> allMaterials = materials;
        if (promo != null) {
            promoAmount = promo.discount();
            promo.shares().forEach((i, share) -> priced.get(i).setPromoShare(share));
            if (!promo.gifts().isEmpty()) {
                allMaterials = new HashMap<>(materials);
                for (Map.Entry<Long, BigDecimal> g : promo.gifts().entrySet()) {
                    WmsMaterial gm = promo.giftMaterials().get(g.getKey());
                    allMaterials.putIfAbsent(gm.getId(), gm);
                    priced.add(giftLine(priced.size() + 1, gm, g.getValue()));
                }
            }
        }

        BigDecimal amount = Decimals.sum(priced, PosViews.PricedLine::getAmount);
        BigDecimal orderDiscount = orderDiscountReq == null ? BigDecimal.ZERO : Decimals.money(orderDiscountReq);
        if (orderDiscount.signum() > 0) {
            requirePerm(DISCOUNT_PERM, "pos.error.noDiscountPerm");
        }
        BigDecimal afterPromo = amount.subtract(promoAmount);
        if (orderDiscount.compareTo(afterPromo) > 0) {
            throw new BizException("pos.error.orderDiscountTooLarge");
        }
        // 手工优惠占取价金额的比例
        BigDecimal manualDiscount = autoTotal.subtract(amount.subtract(orderDiscount));
        BigDecimal manualRate = autoTotal.signum() == 0 || manualDiscount.signum() <= 0 ? BigDecimal.ZERO
                : manualDiscount.multiply(Decimals.HUNDRED).divide(autoTotal, 2, RoundingMode.HALF_UP);
        BigDecimal maxRate = bizRules.decimal(PosBizRules.MAX_DISCOUNT_RATE);
        if (manualRate.compareTo(maxRate) > 0 && !permissionService.hasPerm(DISCOUNT_ANY_PERM)) {
            throw new BizException("pos.error.discountExceeded", manualRate.toPlainString(), maxRate.stripTrailingZeros().toPlainString());
        }
        PosRounding rounding = bizRules.option(PosBizRules.ROUNDING, PosRounding.class);
        BigDecimal payable = rounding.apply(afterPromo.subtract(orderDiscount));
        BigDecimal roundOff = afterPromo.subtract(orderDiscount).subtract(payable);
        allocate(priced, orderDiscount.add(roundOff), afterPromo);
        checkMinPrice(priced);
        BigDecimal totalQty = Decimals.sum(priced, PosViews.PricedLine::getQty);
        return new Cart(new PosViews.Preview(priced, totalQty, listTotal, amount, orderDiscount, roundOff, payable, manualRate, member,
                promo == null ? null : promo.promotion().getId(), promo == null ? null : promo.promotion().getName(),
                promo == null ? null : promo.promotion().getKind(), promoAmount), allMaterials);
    }

    /** 行金额：特价超出每单限购的部分按 fallback（会员价 / 门店价） */
    private static BigDecimal lineAmount(PosPricingService.Price p, BigDecimal qty, BigDecimal baseQty, BigDecimal ratio, BigDecimal unitPrice) {
        if (p.source() == PosPriceSource.SPECIAL && p.limitQty() != null && baseQty.compareTo(p.limitQty()) > 0 && p.fallback() != null) {
            BigDecimal limitInput = p.limitQty().divide(ratio, 4, RoundingMode.DOWN);
            BigDecimal fallback = unitPrice(p.fallback(), ratio);
            return money(limitInput.multiply(unitPrice).add(qty.subtract(limitInput).multiply(fallback)));
        }
        return money(qty.multiply(unitPrice));
    }

    /**
     * 整单优惠与抹零按行金额（扣除满减分摊后）比例分摊到非赠品行，最后一个非赠品行取余数；赠品行实收为 0
     */
    private static void allocate(List<PosViews.PricedLine> lines, BigDecimal share, BigDecimal total) {
        int last = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).isGift()) {
                last = i;
            }
        }
        BigDecimal left = share;
        for (int i = 0; i < lines.size(); i++) {
            PosViews.PricedLine l = lines.get(i);
            BigDecimal base = l.getAmount().subtract(l.getPromoShare());
            BigDecimal s;
            if (l.isGift()) {
                s = BigDecimal.ZERO;
            } else if (i == last) {
                s = left;
            } else {
                s = total.signum() == 0 ? BigDecimal.ZERO : money(share.multiply(base).divide(total, 6, RoundingMode.HALF_UP));
                left = left.subtract(s);
            }
            l.setOrderShare(s);
            l.setFinalAmount(base.subtract(s));
        }
    }

    /** 满赠的赠品行：基本单位、单价 0 */
    private static PosViews.PricedLine giftLine(int lineNo, WmsMaterial m, BigDecimal qty) {
        PosViews.PricedLine line = new PosViews.PricedLine();
        BigDecimal q = qty.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
        line.setLineNo(lineNo);
        line.setMaterialId(m.getId());
        line.setMaterialCode(m.getCode());
        line.setMaterialName(m.getName());
        line.setSpec(m.getSpec());
        line.setUnit(m.getUnit());
        line.setUnitRatio(BigDecimal.ONE);
        line.setQty(q);
        line.setBaseQty(q.setScale(4, RoundingMode.HALF_UP));
        line.setListPrice(BigDecimal.ZERO);
        line.setPrice(BigDecimal.ZERO);
        line.setPriceSource(PosPriceSource.GIFT);
        line.setDiscountRate(Decimals.HUNDRED);
        line.setAmount(BigDecimal.ZERO.setScale(2));
        line.setSalePrice(BigDecimal.ZERO);
        line.setGift(true);
        return line;
    }

    private void checkMinPrice(List<PosViews.PricedLine> lines) {
        Map<Long, ScmPriceControl> controls = priceControlService.ofMaterials(lines.stream().map(PosViews.PricedLine::getMaterialId).distinct().toList())
                .stream().filter(c -> c.getMinSalePrice() != null)
                .collect(Collectors.toMap(ScmPriceControl::getMaterialId, Function.identity(), (a, b) -> a));
        if (controls.isEmpty()) {
            return;
        }
        boolean allowed = permissionService.hasPerm(BELOW_MIN_PERM);
        for (PosViews.PricedLine l : lines) {
            ScmPriceControl c = controls.get(l.getMaterialId());
            if (c == null || l.isGift() || l.getBaseQty().signum() == 0) {
                continue;
            }
            // 满减分摊不参与最低售价校验（活动由总部设置）
            BigDecimal unit = l.getFinalAmount().add(l.getPromoShare()).divide(l.getBaseQty(), 4, RoundingMode.HALF_UP);
            if (unit.compareTo(c.getMinSalePrice()) < 0) {
                l.setBelowMin(true);
                if (c.getControlMode() == PriceControlMode.BLOCK && !allowed) {
                    throw new BizException("pos.error.belowMinPrice", l.getMaterialCode(), l.getMaterialName(),
                            c.getMinSalePrice().stripTrailingZeros().toPlainString());
                }
            }
        }
    }

    /** 录入单位的换算率：基本单位为 1，辅助单位按物料的单位设置 */
    BigDecimal ratio(WmsMaterial m, String unit, Map<Long, List<WmsMaterialUnit>> cache) {
        if (unit == null || Objects.equals(unit, m.getUnit())) {
            return BigDecimal.ONE;
        }
        return cache.computeIfAbsent(m.getId(), materialService::units).stream().filter(u -> unit.equals(u.getUnit()))
                .map(WmsMaterialUnit::getRatio).findFirst()
                .orElseThrow(() -> new BizException("pos.error.unitNotFound", m.getName(), unit));
    }

    /** 录入单位的价格 = 基本单位价 × 换算率（辅助单位按 2 位小数） */
    static BigDecimal unitPrice(BigDecimal basePrice, BigDecimal ratio) {
        if (basePrice == null) {
            return null;
        }
        return ratio.compareTo(BigDecimal.ONE) == 0 ? basePrice : basePrice.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
    }

    static void checkSellable(WmsMaterial m) {
        if (m.getIsTemplate() != null && m.getIsTemplate() == 1) {
            throw new BizException("pos.error.templateNotSellable", m.getCode(), m.getName());
        }
    }

    private void requirePerm(String perm, String errorKey) {
        if (!permissionService.hasPerm(perm)) {
            throw new BizException(errorKey);
        }
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
