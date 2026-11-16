package com.lemon.erp.modules.store.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.TreeUtils;
import com.lemon.erp.framework.store.StoreBrief;
import com.lemon.erp.framework.store.StoreLookup;
import com.lemon.erp.framework.store.StoreType;
import com.lemon.erp.modules.store.entity.StoreInfo;
import com.lemon.erp.modules.store.entity.StoreRegion;
import com.lemon.erp.modules.store.mapper.StoreInfoMapper;
import com.lemon.erp.modules.store.mapper.StoreRegionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 只依赖本模块的 Mapper：下层模块的 Service 注入 StoreLookup，门店的 Service 又注入了它们，避免循环依赖
 */
@Component
@RequiredArgsConstructor
public class StoreLookupImpl implements StoreLookup {

    private final StoreInfoMapper storeMapper;
    private final StoreRegionMapper regionMapper;

    @Override
    public Optional<StoreBrief> find(Long storeId) {
        return storeId == null ? Optional.empty() : Optional.ofNullable(storeMapper.selectById(storeId)).map(StoreLookupImpl::brief);
    }

    @Override
    public Map<Long, StoreBrief> findAll(Collection<Long> storeIds) {
        List<Long> ids = storeIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return storeMapper.selectByIds(ids).stream().map(StoreLookupImpl::brief)
                .collect(Collectors.toMap(StoreBrief::id, b -> b, (a, b) -> a, LinkedHashMap::new));
    }

    @Override
    public StoreBrief headquarters() {
        StoreInfo hq = storeMapper.selectOne(Wrappers.<StoreInfo>lambdaQuery().eq(StoreInfo::getType, StoreType.HQ));
        if (hq == null) {
            throw new BizException("store.error.hqMissing");
        }
        return brief(hq);
    }

    @Override
    public Set<Long> storesInRegion(Long regionId) {
        Set<Long> regions = TreeUtils.selfAndDescendantIds(regionMapper.selectList(Wrappers.<StoreRegion>lambdaQuery()
                .select(StoreRegion::getId, StoreRegion::getParentId)), regionId);
        return storeMapper.selectList(Wrappers.<StoreInfo>lambdaQuery().select(StoreInfo::getId).in(StoreInfo::getRegionId, regions))
                .stream().map(StoreInfo::getId).collect(Collectors.toSet());
    }

    @Override
    public List<StoreBrief> listAll() {
        return storeMapper.selectList(Wrappers.<StoreInfo>lambdaQuery().orderByAsc(StoreInfo::getSort).orderByAsc(StoreInfo::getId))
                .stream().map(StoreLookupImpl::brief).toList();
    }

    static StoreBrief brief(StoreInfo s) {
        return new StoreBrief(s.getId(), s.getCode(), s.getName(), s.getType(), s.getStatus(), s.getRegionId(),
                s.getSupplyStoreId(), s.getPriceSchemeId(), s.getDefaultWarehouseId(), s.getDefaultAccountId());
    }
}
