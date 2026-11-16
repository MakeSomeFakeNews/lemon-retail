package com.lemon.erp.modules.store.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.base.TreeEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.util.TreeUtils;
import com.lemon.erp.framework.ref.RefType;
import com.lemon.erp.framework.ref.ReferenceGuard;
import com.lemon.erp.modules.store.entity.StoreInfo;
import com.lemon.erp.modules.store.entity.StoreRegion;
import com.lemon.erp.modules.store.mapper.StoreInfoMapper;
import com.lemon.erp.modules.store.mapper.StoreRegionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 区域（多级树）
 */
@Service
@RequiredArgsConstructor
public class StoreRegionService {

    private final StoreRegionMapper regionMapper;
    private final StoreInfoMapper storeMapper;
    private final ReferenceGuard referenceGuard;

    /** 区域树（含停用），区域数量很少，一次查出 */
    public List<StoreRegion> tree() {
        return TreeUtils.build(listAll());
    }

    public List<StoreRegion> listAll() {
        return regionMapper.selectList(Wrappers.<StoreRegion>lambdaQuery().orderByAsc(StoreRegion::getSort).orderByAsc(StoreRegion::getId));
    }

    public StoreRegion get(Long id) {
        StoreRegion region = id == null ? null : regionMapper.selectById(id);
        if (region == null) {
            throw new BizException("store.error.regionNotFound");
        }
        return region;
    }

    public void create(StoreRegion region) {
        region.setId(null);
        normalize(region);
        checkCodeUnique(region.getCode(), null);
        checkParent(region);
        regionMapper.insert(region);
    }

    public void update(Long id, StoreRegion region) {
        get(id);
        region.setId(id);
        normalize(region);
        checkCodeUnique(region.getCode(), id);
        if (!Objects.equals(region.getParentId(), TreeEntity.ROOT_PARENT_ID) && selfAndDescendantIds(id).contains(region.getParentId())) {
            throw new BizException("store.error.regionParentInvalid");
        }
        checkParent(region);
        regionMapper.updateById(region);
    }

    /** 有下级区域或门店的区域不能删除 */
    public void delete(Long id) {
        StoreRegion region = get(id);
        if (regionMapper.exists(Wrappers.<StoreRegion>lambdaQuery().eq(StoreRegion::getParentId, id))) {
            throw new BizException("store.error.regionHasChildren", region.getName());
        }
        if (storeMapper.exists(Wrappers.<StoreInfo>lambdaQuery().eq(StoreInfo::getRegionId, id))) {
            throw new BizException("store.error.regionHasStores", region.getName());
        }
        referenceGuard.checkDeletable(RefType.REGION, id, Messages.get("store.text.region", region.getName()));
        regionMapper.deleteById(id);
    }

    /** 区域自身及全部下级区域 */
    public Set<Long> selfAndDescendantIds(Long regionId) {
        return TreeUtils.selfAndDescendantIds(regionMapper.selectList(Wrappers.<StoreRegion>lambdaQuery()
                .select(StoreRegion::getId, StoreRegion::getParentId)), regionId);
    }

    public Map<Long, String> nameMap(Collection<Long> ids) {
        List<Long> list = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (list.isEmpty()) {
            return Collections.emptyMap();
        }
        return regionMapper.selectByIds(list).stream().collect(Collectors.toMap(StoreRegion::getId, StoreRegion::getName, (a, b) -> a));
    }

    /** 启用的区域必须存在 */
    public void requireEnabled(Long regionId) {
        StoreRegion region = get(regionId);
        if (region.getStatus() != EnableStatus.ENABLED) {
            throw new BizException("store.error.regionDisabled", region.getName());
        }
    }

    private static void normalize(StoreRegion region) {
        if (region.getParentId() == null) region.setParentId(TreeEntity.ROOT_PARENT_ID);
        if (region.getSort() == null) region.setSort(0);
        if (region.getStatus() == null) region.setStatus(EnableStatus.ENABLED);
    }

    private void checkParent(StoreRegion region) {
        if (Objects.equals(region.getParentId(), TreeEntity.ROOT_PARENT_ID)) {
            return;
        }
        if (Objects.equals(region.getParentId(), region.getId())) {
            throw new BizException("store.error.regionParentInvalid");
        }
        get(region.getParentId());
    }

    private void checkCodeUnique(String code, Long id) {
        StoreRegion exist = regionMapper.selectOne(Wrappers.<StoreRegion>lambdaQuery().eq(StoreRegion::getCode, code));
        if (exist != null && !Objects.equals(exist.getId(), id)) {
            throw new BizException("store.error.regionCodeExists", code);
        }
    }

    static <T> Map<Long, T> byId(Collection<T> items, Function<T, Long> id) {
        return items.stream().collect(Collectors.toMap(id, Function.identity(), (a, b) -> a));
    }
}
