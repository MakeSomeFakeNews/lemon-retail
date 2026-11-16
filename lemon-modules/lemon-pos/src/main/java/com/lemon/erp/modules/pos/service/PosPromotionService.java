package com.lemon.erp.modules.pos.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.DetailLines;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.store.StoreBrief;
import com.lemon.erp.framework.store.StoreLookup;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.dto.PosRequests;
import com.lemon.erp.modules.pos.entity.PosPromotion;
import com.lemon.erp.modules.pos.entity.PosPromotionGift;
import com.lemon.erp.modules.pos.entity.PosPromotionGoods;
import com.lemon.erp.modules.pos.entity.PosPromotionTarget;
import com.lemon.erp.modules.pos.mapper.PosPromotionGiftMapper;
import com.lemon.erp.modules.pos.mapper.PosPromotionGoodsMapper;
import com.lemon.erp.modules.pos.mapper.PosPromotionMapper;
import com.lemon.erp.modules.pos.mapper.PosPromotionTargetMapper;
import com.lemon.erp.modules.pos.model.PromotionKind;
import com.lemon.erp.modules.pos.model.PromotionRange;
import com.lemon.erp.modules.pos.model.PromotionThreshold;
import com.lemon.erp.modules.pos.model.SaleChannel;
import com.lemon.erp.modules.pos.model.SpecialScope;
import com.lemon.erp.modules.pos.model.SpecialStatus;
import com.lemon.erp.modules.store.service.StoreRegionService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 满减 / 满折 / 满赠活动：草稿可修改、删除；开始后（进行中）在活动时间内参与 POS 收银的整单计算（见 {@link PosPromotionEngine}），
 * 可提前结束；已结束的不能再开始。适用门店、渠道、仅限会员与特价活动相同
 */
@Service
@RequiredArgsConstructor
public class PosPromotionService {

    private final PosPromotionMapper promotionMapper;
    private final PosPromotionTargetMapper targetMapper;
    private final PosPromotionGoodsMapper goodsMapper;
    private final PosPromotionGiftMapper giftMapper;
    private final PosMemberRuleService ruleService;
    private final WmsMaterialService materialService;
    private final StoreLookup storeLookup;
    private final StoreRegionService regionService;

    public PageResult<PosPromotion> page(long current, long size, PosQueries.Promotion q) {
        Page<PosPromotion> page = Page.of(current, size);
        promotionMapper.selectPageList(page, q);
        page.getRecords().forEach(this::fillTargets);
        return PageResult.of(page);
    }

    public PosPromotion detail(Long id) {
        PosPromotion p = id == null ? null : promotionMapper.selectDetail(id);
        if (p == null) {
            throw new BizException("pos.error.promotionNotFound");
        }
        fillTargets(p);
        p.setGoods(goodsMapper.selectByPromotionIds(List.of(id)));
        p.setGifts(giftMapper.selectByPromotionIds(List.of(id)));
        return p;
    }

    @Transactional
    public Long create(PosRequests.Promotion req) {
        PosPromotion p = new PosPromotion();
        p.setStatus(SpecialStatus.DRAFT);
        apply(p, req);
        promotionMapper.insert(p);
        saveLines(p.getId(), req);
        return p.getId();
    }

    /** 修改（草稿） */
    @Transactional
    public void update(Long id, PosRequests.Promotion req) {
        PosPromotion old = detail(id);
        if (old.getStatus() != SpecialStatus.DRAFT) {
            throw new BizException("pos.error.promotionNotDraft", old.getName());
        }
        PosPromotion p = new PosPromotion();
        p.setId(id);
        apply(p, req);
        promotionMapper.updateById(p);
        saveLines(id, req);
    }

    /** 删除（草稿、已结束） */
    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            PosPromotion p = detail(id);
            if (p.getStatus() == SpecialStatus.ACTIVE) {
                throw new BizException("pos.error.promotionActive", p.getName());
            }
            promotionMapper.deleteById(id);
            targetMapper.delete(Wrappers.<PosPromotionTarget>lambdaQuery().eq(PosPromotionTarget::getPromotionId, id));
            goodsMapper.delete(Wrappers.<PosPromotionGoods>lambdaQuery().eq(PosPromotionGoods::getPromotionId, id));
            giftMapper.delete(Wrappers.<PosPromotionGift>lambdaQuery().eq(PosPromotionGift::getPromotionId, id));
        }
    }

    /** 开始：草稿 → 进行中（结束时间已过的不能开始） */
    public void activate(Long id) {
        PosPromotion p = detail(id);
        if (p.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BizException("pos.error.promotionExpired", p.getName());
        }
        StatusTransitions.change(promotionMapper, PosPromotion::getId, id, PosPromotion::getStatus, SpecialStatus.DRAFT, SpecialStatus.ACTIVE,
                "pos.error.promotionNotDraft", p.getName());
    }

    /** 结束：进行中 → 已结束 */
    public void end(Long id) {
        PosPromotion p = detail(id);
        StatusTransitions.change(promotionMapper, PosPromotion::getId, id, PosPromotion::getStatus, SpecialStatus.ACTIVE, SpecialStatus.ENDED,
                "pos.error.promotionNotActive", p.getName());
    }

    private void apply(PosPromotion p, PosRequests.Promotion req) {
        if (!req.endTime().isAfter(req.beginTime())) {
            throw new BizException("pos.error.specialTimeInvalid");
        }
        switch (req.kind()) {
            case REDUCE -> {
                if (req.reduceAmount() == null) {
                    throw new BizException("pos.error.promotionReduceRequired");
                }
            }
            case DISCOUNT -> {
                if (req.discountRate() == null) {
                    throw new BizException("pos.error.promotionRateRequired");
                }
            }
            case GIFT -> {
                if (req.gifts() == null || req.gifts().isEmpty()) {
                    throw new BizException("pos.error.promotionGiftsRequired");
                }
            }
            default -> {
            }
        }
        PromotionThreshold thresholdType = req.thresholdType() == null ? PromotionThreshold.AMOUNT : req.thresholdType();
        if (req.kind() == PromotionKind.REDUCE && thresholdType == PromotionThreshold.AMOUNT
                && req.reduceAmount().compareTo(req.threshold()) >= 0) {
            throw new BizException("pos.error.promotionReduceTooLarge");
        }
        p.setName(req.name().trim());
        p.setKind(req.kind());
        p.setThresholdType(thresholdType);
        p.setThreshold(req.threshold());
        p.setReduceAmount(req.kind() == PromotionKind.REDUCE ? Decimals.money(req.reduceAmount()) : null);
        p.setDiscountRate(req.kind() == PromotionKind.DISCOUNT ? req.discountRate() : null);
        p.setStackable(req.kind() != PromotionKind.DISCOUNT && Boolean.TRUE.equals(req.stackable()));
        p.setGoodsScope(req.goodsScope() == null ? PromotionRange.ALL : req.goodsScope());
        p.setExcludeSpecial(Boolean.TRUE.equals(req.excludeSpecial()));
        p.setPriority(req.priority() == null ? 0 : req.priority());
        p.setBeginTime(req.beginTime());
        p.setEndTime(req.endTime());
        p.setChannel(req.channel() == null ? SaleChannel.POS : req.channel());
        p.setStoreScope(req.storeScope());
        p.setMemberOnly(Boolean.TRUE.equals(req.memberOnly()));
        p.setRemark(req.remark());
    }

    private void saveLines(Long id, PosRequests.Promotion req) {
        List<Long> targets = req.storeScope() == SpecialScope.ALL || req.targetIds() == null ? List.of()
                : req.targetIds().stream().filter(Objects::nonNull).distinct().toList();
        if (req.storeScope() != SpecialScope.ALL && targets.isEmpty()) {
            throw new BizException(req.storeScope() == SpecialScope.STORES ? "pos.error.specialStoresRequired" : "pos.error.specialRegionsRequired");
        }
        if (req.storeScope() == SpecialScope.STORES) {
            targets.forEach(storeLookup::require);
        } else if (req.storeScope() == SpecialScope.REGIONS) {
            Map<Long, String> names = regionService.nameMap(targets);
            targets.stream().filter(t -> !names.containsKey(t)).findFirst().ifPresent(t -> {
                throw new BizException("pos.error.regionNotFound");
            });
        }
        PromotionRange range = req.goodsScope() == null ? PromotionRange.ALL : req.goodsScope();
        List<PosRequests.GoodsItem> goods = range == PromotionRange.ALL ? List.of() : ruleService.goods(req.goods());
        if (range != PromotionRange.ALL && goods.isEmpty()) {
            throw new BizException("pos.error.promotionGoodsRequired");
        }
        boolean byMaterial = range == PromotionRange.MATERIALS;
        if (goods.stream().anyMatch(g -> byMaterial != (g.materialId() != null))) {
            throw new BizException("pos.error.goodsItemInvalid");
        }
        List<PosPromotionGoods> goodsRows = goods.stream().map(g -> {
            PosPromotionGoods x = new PosPromotionGoods();
            x.setMaterialId(g.materialId());
            x.setCategoryCode(StringUtils.hasText(g.categoryCode()) ? g.categoryCode() : null);
            return x;
        }).toList();
        DetailLines.replace(goodsMapper, PosPromotionGoods::getPromotionId, id, goodsRows, PosPromotionGoods::setPromotionId, goodsMapper::insertBatch);

        Set<Long> giftIds = new HashSet<>();
        List<PosPromotionGift> gifts = req.kind() != PromotionKind.GIFT || req.gifts() == null ? List.of() : req.gifts().stream().map(g -> {
            if (!giftIds.add(g.materialId())) {
                throw new BizException("pos.error.promotionGiftDuplicate");
            }
            PosPromotionGift x = new PosPromotionGift();
            x.setMaterialId(g.materialId());
            x.setQty(g.qty());
            return x;
        }).toList();
        if (!giftIds.isEmpty()) {
            materialService.getEnabled(giftIds);
        }
        DetailLines.replace(giftMapper, PosPromotionGift::getPromotionId, id, gifts, PosPromotionGift::setPromotionId, giftMapper::insertBatch);

        List<PosPromotionTarget> rows = targets.stream().map(t -> {
            PosPromotionTarget x = new PosPromotionTarget();
            x.setTargetId(t);
            return x;
        }).toList();
        DetailLines.replace(targetMapper, PosPromotionTarget::getPromotionId, id, rows, PosPromotionTarget::setPromotionId, targetMapper::insertBatch);
    }

    private void fillTargets(PosPromotion p) {
        if (p.getStoreScope() == SpecialScope.ALL) {
            p.setTargetIds(List.of());
            p.setTargetNames(List.of());
            return;
        }
        List<Long> ids = targetMapper.selectList(Wrappers.<PosPromotionTarget>lambdaQuery().eq(PosPromotionTarget::getPromotionId, p.getId()))
                .stream().map(PosPromotionTarget::getTargetId).toList();
        p.setTargetIds(ids);
        if (p.getStoreScope() == SpecialScope.STORES) {
            Map<Long, StoreBrief> stores = storeLookup.findAll(ids);
            p.setTargetNames(ids.stream().map(x -> stores.containsKey(x) ? stores.get(x).name() : String.valueOf(x)).toList());
        } else {
            Map<Long, String> names = regionService.nameMap(ids);
            p.setTargetNames(ids.stream().map(x -> names.getOrDefault(x, String.valueOf(x))).toList());
        }
    }
}
