package com.lemon.erp.modules.pos.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.pos.dto.PosRequests;
import com.lemon.erp.modules.pos.entity.PosMemberRule;
import com.lemon.erp.modules.pos.entity.PosPointsExclude;
import com.lemon.erp.modules.pos.entity.PosTopupTier;
import com.lemon.erp.modules.pos.mapper.PosMemberRuleMapper;
import com.lemon.erp.modules.pos.mapper.PosPointsExcludeMapper;
import com.lemon.erp.modules.pos.mapper.PosTopupTierMapper;
import com.lemon.erp.modules.pos.model.GoodsTargetType;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.model.MaterialCategoryTree;
import com.lemon.erp.modules.wms.service.WmsMaterialCategoryService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 积分与储值规则（每个租户一条，没有时按默认值）、不积分的商品 / 分类（分类含下级分类）、充值赠送档位（充 X 送 Y）
 */
@Service
@RequiredArgsConstructor
public class PosMemberRuleService {

    private final PosMemberRuleMapper ruleMapper;
    private final PosPointsExcludeMapper excludeMapper;
    private final PosTopupTierMapper tierMapper;
    private final WmsMaterialService materialService;
    private final WmsMaterialCategoryService categoryService;

    /** 当前规则，没有保存过时为默认值 */
    public PosMemberRule get() {
        PosMemberRule r = ruleMapper.selectOne(Wrappers.<PosMemberRule>lambdaQuery());
        return r != null ? r : PosMemberRule.defaults();
    }

    public List<PosPointsExclude> excludes() {
        return excludeMapper.selectWithNames();
    }

    /** 充值赠送档位，按门槛从低到高 */
    public List<PosTopupTier> tiers() {
        return tierMapper.selectList(Wrappers.<PosTopupTier>lambdaQuery().orderByAsc(PosTopupTier::getMinAmount));
    }

    /** 保存规则、不积分的商品 / 分类、充值赠送档位 */
    @Transactional
    public void save(PosRequests.MemberRuleSave req) {
        PosMemberRule r = req.rule();
        PosMemberRule old = ruleMapper.selectOne(Wrappers.<PosMemberRule>lambdaQuery());
        r.setId(old == null ? null : old.getId());
        if (old == null) {
            ruleMapper.insert(r);
        } else {
            ruleMapper.updateById(r);
        }
        excludeMapper.delete(Wrappers.<PosPointsExclude>lambdaQuery().isNotNull(PosPointsExclude::getId));
        List<PosPointsExclude> excludes = goods(req.excludes()).stream().map(g -> {
            PosPointsExclude e = new PosPointsExclude();
            e.setTargetType(g.materialId() != null ? GoodsTargetType.MATERIAL : GoodsTargetType.CATEGORY);
            e.setMaterialId(g.materialId());
            e.setCategoryCode(g.materialId() != null ? null : g.categoryCode());
            return e;
        }).toList();
        if (!excludes.isEmpty()) {
            excludeMapper.insertBatch(excludes);
        }
        tierMapper.delete(Wrappers.<PosTopupTier>lambdaQuery().isNotNull(PosTopupTier::getId));
        List<PosRequests.TopupTier> tiers = req.tiers() == null ? List.of() : req.tiers();
        if (tiers.stream().map(t -> t.minAmount().stripTrailingZeros()).distinct().count() < tiers.size()) {
            throw new BizException("pos.error.topupTierDuplicate");
        }
        if (!tiers.isEmpty()) {
            tierMapper.insertBatch(tiers.stream().map(t -> {
                PosTopupTier x = new PosTopupTier();
                x.setMinAmount(Decimals.money(t.minAmount()));
                x.setBonusAmount(Decimals.money(t.bonusAmount()));
                return x;
            }).toList());
        }
    }

    /**
     * 校验商品范围（商品、分类二选一，不重复；商品须启用，分类须存在），供不积分设置、满减活动的参与商品使用
     */
    public List<PosRequests.GoodsItem> goods(List<PosRequests.GoodsItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        MaterialCategoryTree tree = null;
        Set<String> keys = new HashSet<>();
        Set<Long> materials = new HashSet<>();
        for (PosRequests.GoodsItem g : items) {
            boolean hasMaterial = g.materialId() != null;
            boolean hasCategory = StringUtils.hasText(g.categoryCode());
            if (hasMaterial == hasCategory) {
                throw new BizException("pos.error.goodsItemInvalid");
            }
            if (!keys.add(hasMaterial ? "M" + g.materialId() : "C" + g.categoryCode())) {
                throw new BizException("pos.error.goodsItemDuplicate");
            }
            if (hasMaterial) {
                materials.add(g.materialId());
            } else {
                tree = tree == null ? categoryService.tree() : tree;
                if (tree.byCode(g.categoryCode()) == null) {
                    throw new BizException("pos.error.categoryNotFound", g.categoryCode());
                }
            }
        }
        if (!materials.isEmpty()) {
            materialService.getEnabled(materials);
        }
        return items;
    }

    /** 不积分的物料（按商品，或物料分类及其上级分类命中设置的分类） */
    public Set<Long> excludedMaterials(Collection<WmsMaterial> materials) {
        List<PosPointsExclude> excludes = excludeMapper.selectList(Wrappers.<PosPointsExclude>lambdaQuery());
        if (excludes.isEmpty() || materials.isEmpty()) {
            return Set.of();
        }
        Set<Long> ids = excludes.stream().map(PosPointsExclude::getMaterialId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> codes = excludes.stream().map(PosPointsExclude::getCategoryCode).filter(Objects::nonNull).collect(Collectors.toSet());
        MaterialCategoryTree tree = codes.isEmpty() ? null : categoryService.tree();
        return materials.stream().filter(m -> ids.contains(m.getId()) || tree != null && inCategories(tree, m, codes))
                .map(WmsMaterial::getId).collect(Collectors.toSet());
    }

    /** 物料的分类或其上级分类是否在 codes 中 */
    static boolean inCategories(MaterialCategoryTree tree, WmsMaterial m, Set<String> codes) {
        return m.getCategory() != null && tree.lineage(m.getCategory()).stream().anyMatch(codes::contains);
    }

    /** 充值金额对应的赠送金额：达到的最高档位，没有档位时为 0 */
    public BigDecimal bonusFor(BigDecimal amount) {
        return tiers().stream().filter(t -> amount.compareTo(t.getMinAmount()) >= 0)
                .max(Comparator.comparing(PosTopupTier::getMinAmount)).map(PosTopupTier::getBonusAmount).orElse(BigDecimal.ZERO);
    }
}
