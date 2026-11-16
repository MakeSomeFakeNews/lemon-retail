package com.lemon.erp.modules.pos.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.data.DetailLines;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.store.StoreBrief;
import com.lemon.erp.framework.store.StoreLookup;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.dto.PosRequests;
import com.lemon.erp.modules.pos.entity.PosSpecial;
import com.lemon.erp.modules.pos.entity.PosSpecialItem;
import com.lemon.erp.modules.pos.entity.PosSpecialTarget;
import com.lemon.erp.modules.pos.mapper.PosSpecialItemMapper;
import com.lemon.erp.modules.pos.mapper.PosSpecialMapper;
import com.lemon.erp.modules.pos.mapper.PosSpecialTargetMapper;
import com.lemon.erp.modules.pos.model.SaleChannel;
import com.lemon.erp.modules.pos.model.SpecialScope;
import com.lemon.erp.modules.pos.model.SpecialStatus;
import com.lemon.erp.modules.store.service.StoreRegionService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 特价活动：草稿可修改、删除；开始后（进行中）在活动时间内参与 POS 取价（最优先），可提前结束；已结束的不能再开始。
 * 适用门店：全部 / 指定门店 / 指定区域（含下级区域，按取价时门店所在的区域判断）。同一商品命中多个活动时取最低价
 */
@Service
@RequiredArgsConstructor
public class PosSpecialService {

    private final PosSpecialMapper specialMapper;
    private final PosSpecialItemMapper itemMapper;
    private final PosSpecialTargetMapper targetMapper;
    private final WmsMaterialService materialService;
    private final StoreLookup storeLookup;
    private final StoreRegionService regionService;

    public PageResult<PosSpecial> page(long current, long size, PosQueries.Special q) {
        Page<PosSpecial> page = Page.of(current, size);
        specialMapper.selectPageList(page, q);
        page.getRecords().forEach(this::fillTargets);
        return PageResult.of(page);
    }

    public PosSpecial detail(Long id) {
        PosSpecial s = id == null ? null : specialMapper.selectDetail(id);
        if (s == null) {
            throw new BizException("pos.error.specialNotFound");
        }
        fillTargets(s);
        s.setItems(itemMapper.selectBySpecialId(id));
        return s;
    }

    @Transactional
    public Long create(PosRequests.Special req) {
        PosSpecial s = new PosSpecial();
        s.setStatus(SpecialStatus.DRAFT);
        apply(s, req);
        specialMapper.insert(s);
        saveLines(s.getId(), req);
        return s.getId();
    }

    /** 修改（草稿） */
    @Transactional
    public void update(Long id, PosRequests.Special req) {
        PosSpecial old = detail(id);
        if (old.getStatus() != SpecialStatus.DRAFT) {
            throw new BizException("pos.error.specialNotDraft", old.getName());
        }
        PosSpecial s = new PosSpecial();
        s.setId(id);
        apply(s, req);
        specialMapper.updateById(s);
        saveLines(id, req);
    }

    /** 删除（草稿、已结束） */
    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            PosSpecial s = detail(id);
            if (s.getStatus() == SpecialStatus.ACTIVE) {
                throw new BizException("pos.error.specialActive", s.getName());
            }
            specialMapper.deleteById(id);
            itemMapper.delete(Wrappers.<PosSpecialItem>lambdaQuery().eq(PosSpecialItem::getSpecialId, id));
            targetMapper.delete(Wrappers.<PosSpecialTarget>lambdaQuery().eq(PosSpecialTarget::getSpecialId, id));
        }
    }

    /** 开始：草稿 → 进行中（结束时间已过的不能开始） */
    public void activate(Long id) {
        PosSpecial s = detail(id);
        if (s.getEndTime().isBefore(java.time.LocalDateTime.now())) {
            throw new BizException("pos.error.specialExpired", s.getName());
        }
        StatusTransitions.change(specialMapper, PosSpecial::getId, id, PosSpecial::getStatus, SpecialStatus.DRAFT, SpecialStatus.ACTIVE,
                "pos.error.specialNotDraft", s.getName());
    }

    /** 结束：进行中 → 已结束 */
    public void end(Long id) {
        PosSpecial s = detail(id);
        StatusTransitions.change(specialMapper, PosSpecial::getId, id, PosSpecial::getStatus, SpecialStatus.ACTIVE, SpecialStatus.ENDED,
                "pos.error.specialNotActive", s.getName());
    }

    private void apply(PosSpecial s, PosRequests.Special req) {
        if (!req.endTime().isAfter(req.beginTime())) {
            throw new BizException("pos.error.specialTimeInvalid");
        }
        s.setName(req.name().trim());
        s.setBeginTime(req.beginTime());
        s.setEndTime(req.endTime());
        s.setChannel(req.channel() == null ? SaleChannel.POS : req.channel());
        s.setStoreScope(req.storeScope());
        s.setMemberOnly(Boolean.TRUE.equals(req.memberOnly()) ? 1 : 0);
        s.setRemark(req.remark());
    }

    private void saveLines(Long id, PosRequests.Special req) {
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
        Set<Long> materials = new HashSet<>();
        List<PosSpecialItem> items = req.items().stream().map(r -> {
            if (!materials.add(r.materialId())) {
                throw new BizException("pos.error.specialDuplicateMaterial");
            }
            if ((r.specialPrice() == null) == (r.discountRate() == null)) {
                throw new BizException("pos.error.specialPriceOrRate");
            }
            PosSpecialItem i = new PosSpecialItem();
            i.setMaterialId(r.materialId());
            i.setSpecialPrice(r.specialPrice());
            i.setDiscountRate(r.discountRate());
            i.setLimitQty(r.limitQty());
            return i;
        }).toList();
        materialService.getEnabled(materials);
        DetailLines.replace(itemMapper, PosSpecialItem::getSpecialId, id, items, PosSpecialItem::setSpecialId, itemMapper::insertBatch);
        List<PosSpecialTarget> rows = targets.stream().map(t -> {
            PosSpecialTarget x = new PosSpecialTarget();
            x.setTargetId(t);
            return x;
        }).toList();
        DetailLines.replace(targetMapper, PosSpecialTarget::getSpecialId, id, rows, PosSpecialTarget::setSpecialId, targetMapper::insertBatch);
    }

    private void fillTargets(PosSpecial s) {
        if (s.getStoreScope() == SpecialScope.ALL) {
            s.setTargetIds(List.of());
            s.setTargetNames(List.of());
            return;
        }
        List<Long> ids = targetMapper.selectList(Wrappers.<PosSpecialTarget>lambdaQuery().eq(PosSpecialTarget::getSpecialId, s.getId()))
                .stream().map(PosSpecialTarget::getTargetId).toList();
        s.setTargetIds(ids);
        if (s.getStoreScope() == SpecialScope.STORES) {
            Map<Long, StoreBrief> stores = storeLookup.findAll(ids);
            s.setTargetNames(ids.stream().map(id -> stores.containsKey(id) ? stores.get(id).name() : String.valueOf(id)).toList());
        } else {
            Map<Long, String> names = regionService.nameMap(ids);
            s.setTargetNames(ids.stream().map(id -> names.getOrDefault(id, String.valueOf(id))).toList());
        }
    }
}
