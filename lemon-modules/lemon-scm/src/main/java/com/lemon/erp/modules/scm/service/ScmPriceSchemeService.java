package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.DetailLines;
import com.lemon.erp.framework.ref.RefType;
import com.lemon.erp.framework.ref.ReferenceGuard;
import com.lemon.erp.modules.scm.entity.ScmPriceAdjustItem;
import com.lemon.erp.modules.scm.entity.ScmPriceHistory;
import com.lemon.erp.modules.scm.entity.ScmPriceScheme;
import com.lemon.erp.modules.scm.entity.ScmPriceSchemeItem;
import com.lemon.erp.modules.scm.mapper.ScmPriceSchemeItemMapper;
import com.lemon.erp.modules.scm.mapper.ScmPriceSchemeMapper;
import com.lemon.erp.modules.scm.model.PriceAdjustTarget;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 门店价格方案：一组商品的价格覆盖（零售价、会员价），门店绑定一个方案（门店模块保存方案 ID，绑定时调用 {@link #requireEnabled}）。
 * 开单取价时单据仓库所属门店的方案价优先于总部零售价（{@link ScmPricingService}）；方案价可直接维护，也可通过调价单定时调整
 */
@Service
@RequiredArgsConstructor
public class ScmPriceSchemeService {

    private final ScmPriceSchemeMapper schemeMapper;
    private final ScmPriceSchemeItemMapper itemMapper;
    private final WmsMaterialService materialService;
    private final DocNoService docNoService;
    private final ReferenceGuard referenceGuard;

    public PageResult<ScmPriceScheme> page(long current, long size, String keyword, EnableStatus status) {
        Page<ScmPriceScheme> page = Page.of(current, size);
        schemeMapper.selectPageList(page, StringUtils.hasText(keyword) ? keyword.trim() : null, status);
        return PageResult.of(page);
    }

    /** 启用的方案（下拉），按编码排序；ids 中的方案（编辑回显，含停用的）一并返回 */
    public List<ScmPriceScheme> options(Collection<Long> ids) {
        boolean hasIds = ids != null && !ids.isEmpty();
        return schemeMapper.selectList(Wrappers.<ScmPriceScheme>lambdaQuery()
                .select(ScmPriceScheme::getId, ScmPriceScheme::getCode, ScmPriceScheme::getName, ScmPriceScheme::getStatus)
                .and(w -> w.eq(ScmPriceScheme::getStatus, EnableStatus.ENABLED).or(hasIds).in(hasIds, ScmPriceScheme::getId, ids))
                .orderByAsc(ScmPriceScheme::getCode));
    }

    public ScmPriceScheme get(Long id) {
        ScmPriceScheme scheme = id == null ? null : schemeMapper.selectById(id);
        if (scheme == null) {
            throw new BizException("scm.error.priceSchemeNotFound");
        }
        return scheme;
    }

    /** 详情：含明细（物料编码、名称、总部零售价） */
    public ScmPriceScheme detail(Long id) {
        ScmPriceScheme scheme = get(id);
        scheme.setItems(itemMapper.selectBySchemeId(id));
        scheme.setItemCount(scheme.getItems().size());
        return scheme;
    }

    /**
     * 方案必须存在且启用：门店绑定方案时调用
     *
     * @throws BizException 方案不存在或已停用
     */
    public ScmPriceScheme requireEnabled(Long id) {
        ScmPriceScheme scheme = get(id);
        if (!EnableStatus.isEnabled(scheme.getStatus())) {
            throw new BizException("scm.error.priceSchemeDisabled", scheme.getName());
        }
        return scheme;
    }

    /** 按 ID 批量取方案 */
    public Map<Long, ScmPriceScheme> mapByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return schemeMapper.selectByIds(ids).stream().collect(Collectors.toMap(ScmPriceScheme::getId, Function.identity()));
    }

    /**
     * 方案中指定物料的方案价（启用的方案），物料 ID → 明细；方案停用或不存在时返回空
     */
    public Map<Long, ScmPriceSchemeItem> enabledPrices(Long schemeId, Collection<Long> materialIds) {
        if (schemeId == null || materialIds == null || materialIds.isEmpty()) {
            return Map.of();
        }
        ScmPriceScheme scheme = schemeMapper.selectById(schemeId);
        if (scheme == null || !EnableStatus.isEnabled(scheme.getStatus())) {
            return Map.of();
        }
        return pricesOf(schemeId, materialIds);
    }

    /** 方案中指定物料的明细（不论方案是否启用），物料 ID → 明细 */
    public Map<Long, ScmPriceSchemeItem> pricesOf(Long schemeId, Collection<Long> materialIds) {
        if (materialIds == null || materialIds.isEmpty()) {
            return Map.of();
        }
        return itemMapper.selectOfMaterials(schemeId, new HashSet<>(materialIds)).stream()
                .collect(Collectors.toMap(ScmPriceSchemeItem::getMaterialId, Function.identity(), (a, b) -> a));
    }

    @Transactional
    public Long create(ScmPriceScheme req) {
        ScmPriceScheme scheme = new ScmPriceScheme();
        scheme.setCode(StringUtils.hasText(req.getCode()) ? req.getCode().trim() : docNoService.next(ScmDocNos.PRICE_SCHEME));
        checkCodeUnique(scheme.getCode(), null);
        scheme.setName(req.getName().trim());
        scheme.setRemark(req.getRemark());
        scheme.setStatus(req.getStatus() == null ? EnableStatus.ENABLED : req.getStatus());
        List<ScmPriceSchemeItem> items = normalizeItems(req.getItems());
        schemeMapper.insert(scheme);
        DetailLines.replace(itemMapper, ScmPriceSchemeItem::getSchemeId, scheme.getId(), items, ScmPriceSchemeItem::setSchemeId,
                itemMapper::insertBatch);
        return scheme.getId();
    }

    /** 修改：表头 + 明细整体替换（items 为 null 时明细不变） */
    @Transactional
    public void update(Long id, ScmPriceScheme req) {
        ScmPriceScheme old = get(id);
        String code = StringUtils.hasText(req.getCode()) ? req.getCode().trim() : old.getCode();
        checkCodeUnique(code, id);
        schemeMapper.update(Wrappers.<ScmPriceScheme>lambdaUpdate()
                .set(ScmPriceScheme::getCode, code)
                .set(ScmPriceScheme::getName, req.getName().trim())
                .set(ScmPriceScheme::getRemark, req.getRemark())
                .set(req.getStatus() != null, ScmPriceScheme::getStatus, req.getStatus())
                .set(ScmPriceScheme::getUpdateTime, LocalDateTime.now())
                .eq(ScmPriceScheme::getId, id));
        if (req.getItems() != null) {
            DetailLines.replace(itemMapper, ScmPriceSchemeItem::getSchemeId, id, normalizeItems(req.getItems()),
                    ScmPriceSchemeItem::setSchemeId, itemMapper::insertBatch);
        }
    }

    public void changeStatus(Long id, EnableStatus status) {
        get(id);
        schemeMapper.update(Wrappers.<ScmPriceScheme>lambdaUpdate().set(ScmPriceScheme::getStatus, status)
                .set(ScmPriceScheme::getUpdateTime, LocalDateTime.now()).eq(ScmPriceScheme::getId, id));
    }

    /** 删除：门店正在使用、调价单引用的方案不能删除（只能停用） */
    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            ScmPriceScheme scheme = get(id);
            referenceGuard.checkDeletable(RefType.PRICE_SCHEME, id,
                    Messages.get("scm.text.priceSchemeLabel", scheme.getCode(), scheme.getName()));
            itemMapper.delete(Wrappers.<ScmPriceSchemeItem>lambdaQuery().eq(ScmPriceSchemeItem::getSchemeId, id));
            schemeMapper.deleteById(id);
        }
    }

    /**
     * 调价单生效时写入方案价：已有明细的更新出现的价格列，没有的新增；为空的价格列不调整。在调价单的事务中执行
     *
     * @return 调价历史（每个物料一行，调整前后的价格），未加调价单 ID 和生效时间
     */
    List<ScmPriceHistory> applyAdjust(Long schemeId, List<ScmPriceAdjustItem> lines) {
        Map<Long, ScmPriceSchemeItem> exists = pricesOf(schemeId, lines.stream().map(ScmPriceAdjustItem::getMaterialId).toList());
        List<ScmPriceSchemeItem> inserts = new ArrayList<>();
        List<ScmPriceHistory> history = new ArrayList<>(lines.size());
        for (ScmPriceAdjustItem line : lines) {
            ScmPriceSchemeItem old = exists.get(line.getMaterialId());
            BigDecimal oldRetail = old == null ? null : old.getRetailPrice();
            BigDecimal oldMember = old == null ? null : old.getMemberPrice();
            BigDecimal newRetail = line.getRetailPrice() != null ? line.getRetailPrice() : oldRetail;
            BigDecimal newMember = line.getMemberPrice() != null ? line.getMemberPrice() : oldMember;
            if (old == null) {
                ScmPriceSchemeItem item = new ScmPriceSchemeItem();
                item.setSchemeId(schemeId);
                item.setMaterialId(line.getMaterialId());
                item.setRetailPrice(newRetail);
                item.setMemberPrice(newMember);
                inserts.add(item);
            } else if (!Objects.equals(oldRetail, newRetail) || !Objects.equals(oldMember, newMember)) {
                itemMapper.update(Wrappers.<ScmPriceSchemeItem>lambdaUpdate()
                        .set(ScmPriceSchemeItem::getRetailPrice, newRetail)
                        .set(ScmPriceSchemeItem::getMemberPrice, newMember)
                        .set(ScmPriceSchemeItem::getUpdateTime, LocalDateTime.now())
                        .eq(ScmPriceSchemeItem::getId, old.getId()));
            }
            ScmPriceHistory h = new ScmPriceHistory();
            h.setMaterialId(line.getMaterialId());
            h.setTarget(PriceAdjustTarget.SCHEME);
            h.setSchemeId(schemeId);
            h.setOldRetailPrice(oldRetail);
            h.setNewRetailPrice(newRetail);
            h.setOldMemberPrice(oldMember);
            h.setNewMemberPrice(newMember);
            history.add(h);
        }
        if (!inserts.isEmpty()) {
            itemMapper.insertBatch(inserts);
        }
        return history;
    }

    private void checkCodeUnique(String code, Long excludeId) {
        if (schemeMapper.exists(Wrappers.<ScmPriceScheme>lambdaQuery().eq(ScmPriceScheme::getCode, code)
                .ne(excludeId != null, ScmPriceScheme::getId, excludeId))) {
            throw new BizException("scm.error.priceSchemeCodeExists", code);
        }
    }

    /** 明细校验：物料存在、不重复，至少有一列价格；价格保留 4 位小数 */
    private List<ScmPriceSchemeItem> normalizeItems(List<ScmPriceSchemeItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        Set<Long> seen = new HashSet<>();
        Map<Long, ?> materials = materialService.mapByIds(items.stream().map(ScmPriceSchemeItem::getMaterialId).toList());
        List<ScmPriceSchemeItem> result = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            ScmPriceSchemeItem in = items.get(i);
            if (!materials.containsKey(in.getMaterialId())) {
                throw new BizException("scm.error.priceLineMaterialNotFound", i + 1);
            }
            if (!seen.add(in.getMaterialId())) {
                throw new BizException("scm.error.priceLineMaterialDuplicate", i + 1);
            }
            if (in.getRetailPrice() == null && in.getMemberPrice() == null) {
                throw new BizException("scm.error.priceLinePriceRequired", i + 1);
            }
            ScmPriceSchemeItem item = new ScmPriceSchemeItem();
            item.setMaterialId(in.getMaterialId());
            item.setRetailPrice(price(in.getRetailPrice()));
            item.setMemberPrice(price(in.getMemberPrice()));
            result.add(item);
        }
        return result;
    }

    static BigDecimal price(BigDecimal v) {
        return v == null ? null : v.setScale(Decimals.PRICE_SCALE, Decimals.ROUNDING);
    }
}
