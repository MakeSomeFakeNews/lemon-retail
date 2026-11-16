package com.lemon.erp.modules.store.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.ref.RefType;
import com.lemon.erp.framework.ref.ReferenceGuard;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.store.StoreStatus;
import com.lemon.erp.framework.store.StoreType;
import com.lemon.erp.framework.tenant.TenantQuotaLookup;
import com.lemon.erp.modules.fin.service.FinAccountService;
import com.lemon.erp.modules.scm.service.ScmPriceSchemeService;
import com.lemon.erp.modules.store.dto.StoreOption;
import com.lemon.erp.modules.store.entity.StoreInfo;
import com.lemon.erp.modules.store.mapper.StoreInfoMapper;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 门店资料。总部每个租户唯一（新租户初始化时创建，不能删除、不能改为其他类型）；有业务数据的门店不能删除，只能停业 / 闭店
 */
@Service
@RequiredArgsConstructor
public class StoreInfoService {

    private final StoreInfoMapper storeMapper;
    private final StoreRegionService regionService;
    private final DocNoService docNoService;
    private final WmsWarehouseService warehouseService;
    private final FinAccountService accountService;
    private final ScmPriceSchemeService priceSchemeService;
    private final ReferenceGuard referenceGuard;
    private final ObjectProvider<TenantQuotaLookup> quotaLookup;

    /**
     * 当前用户可管辖的门店
     *
     * @param keyword  编码、名称或简称，模糊匹配
     * @param regionId 区域（含下级区域）
     */
    @DataPermission(table = "store_info", deptColumn = "", userColumn = "", storeColumn = "id")
    public PageResult<StoreInfo> page(long current, long size, String keyword, StoreType type, StoreStatus status, Long regionId) {
        Set<Long> regions = regionId == null ? null : regionService.selfAndDescendantIds(regionId);
        Page<StoreInfo> page = storeMapper.selectPage(Page.of(current, size), Wrappers.<StoreInfo>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w.like(StoreInfo::getCode, keyword).or().like(StoreInfo::getName, keyword)
                        .or().like(StoreInfo::getShortName, keyword))
                .eq(type != null, StoreInfo::getType, type)
                .eq(status != null, StoreInfo::getStatus, status)
                .in(regions != null, StoreInfo::getRegionId, regions)
                .orderByAsc(StoreInfo::getSort)
                .orderByAsc(StoreInfo::getId));
        fillNames(page.getRecords());
        return PageResult.of(page);
    }

    public StoreInfo get(Long id) {
        StoreInfo store = id == null ? null : storeMapper.selectById(id);
        if (store == null) {
            throw new BizException("framework.error.storeNotFound");
        }
        return store;
    }

    /** 详情：没有该门店的数据权限时拒绝 */
    public StoreInfo detail(Long id) {
        StoreInfo store = get(id);
        checkAccess(id);
        fillNames(List.of(store));
        return store;
    }

    /**
     * 门店下拉选项
     *
     * @param all     true 时返回全部门店（选择上级配送门店、调入门店等），否则只返回当前用户可管辖的门店
     * @param trading 只返回可以开单的门店（筹备、营业）
     */
    public List<StoreOption> options(boolean all, boolean trading) {
        var scope = SecurityUtils.getDataScope();
        return storeMapper.selectList(Wrappers.<StoreInfo>lambdaQuery()
                        .select(StoreInfo::getId, StoreInfo::getCode, StoreInfo::getName, StoreInfo::getShortName, StoreInfo::getType,
                                StoreInfo::getStatus, StoreInfo::getRegionId, StoreInfo::getDefaultWarehouseId, StoreInfo::getDefaultAccountId)
                        .in(trading, StoreInfo::getStatus, StoreStatus.PREPARING, StoreStatus.OPEN)
                        .orderByAsc(StoreInfo::getSort)
                        .orderByAsc(StoreInfo::getId))
                .stream()
                .filter(s -> all || scope.canAccessStore(s.getId()))
                .map(StoreOption::of)
                .toList();
    }

    @Transactional
    public Long create(StoreInfo store) {
        store.setId(null);
        if (!StringUtils.hasText(store.getCode())) {
            store.setCode(docNoService.next(StoreDocNos.STORE));
        }
        checkCodeUnique(store.getCode(), null);
        if (store.getType() == StoreType.HQ) {
            checkSingleHq(null);
        } else {
            checkStoreLimit();
        }
        if (store.getStatus() == null) store.setStatus(StoreStatus.OPEN);
        if (store.getSort() == null) store.setSort(0);
        // 默认仓库、账户只能是本门店的，新增时还没有，由下面自动创建
        store.setDefaultWarehouseId(null);
        store.setDefaultAccountId(null);
        checkRefs(store);
        storeMapper.insert(store);
        Long id = store.getId();
        boolean changed = false;
        if (Boolean.TRUE.equals(store.getCreateWarehouse())) {
            store.setDefaultWarehouseId(warehouseService.createForStore(id, store.getCode(), store.getName()));
            changed = true;
        }
        if (Boolean.TRUE.equals(store.getCreateAccount())) {
            store.setDefaultAccountId(accountService.createStoreCashAccount(id, store.getName()));
            changed = true;
        }
        if (changed) {
            storeMapper.update(Wrappers.<StoreInfo>lambdaUpdate()
                    .set(StoreInfo::getDefaultWarehouseId, store.getDefaultWarehouseId())
                    .set(StoreInfo::getDefaultAccountId, store.getDefaultAccountId())
                    .eq(StoreInfo::getId, id));
        }
        return id;
    }

    /** 不修改状态（走修改状态接口）；总部不能改为其他类型 */
    public void update(Long id, StoreInfo store) {
        StoreInfo old = get(id);
        checkAccess(id);
        store.setId(id);
        if (!StringUtils.hasText(store.getCode())) {
            store.setCode(old.getCode());
        }
        checkCodeUnique(store.getCode(), id);
        if (old.getType() == StoreType.HQ && store.getType() != StoreType.HQ) {
            throw new BizException("store.error.hqTypeCannotChange");
        }
        if (store.getType() == StoreType.HQ) {
            checkSingleHq(id);
        }
        if (Objects.equals(store.getSupplyStoreId(), id)) {
            throw new BizException("store.error.supplyStoreSelf");
        }
        checkRefs(store);
        if (store.getDefaultWarehouseId() != null) {
            warehouseService.checkStoreWarehouse(id, store.getDefaultWarehouseId());
        }
        if (store.getDefaultAccountId() != null) {
            accountService.checkStoreAccount(id, store.getDefaultAccountId());
        }
        store.setStatus(null);
        storeMapper.updateById(store);
    }

    /** 总部不能停业、闭店 */
    public void changeStatus(Long id, StoreStatus status) {
        StoreInfo store = get(id);
        checkAccess(id);
        if (store.getType() == StoreType.HQ && !status.canTrade()) {
            throw new BizException("store.error.hqCannotClose");
        }
        storeMapper.update(Wrappers.<StoreInfo>lambdaUpdate().set(StoreInfo::getStatus, status).eq(StoreInfo::getId, id));
    }

    /** 总部、有仓库或单据等业务数据的门店不能删除 */
    public void delete(List<Long> ids) {
        for (StoreInfo store : storeMapper.selectByIds(ids)) {
            checkAccess(store.getId());
            if (store.getType() == StoreType.HQ) {
                throw new BizException("store.error.hqCannotDelete");
            }
            if (storeMapper.exists(Wrappers.<StoreInfo>lambdaQuery().eq(StoreInfo::getSupplyStoreId, store.getId()))) {
                throw new BizException("store.error.storeIsSupplyStore", store.getName());
            }
            referenceGuard.checkDeletable(RefType.STORE, store.getId(), Messages.get("store.text.store", store.getName()));
        }
        storeMapper.deleteByIds(ids);
    }

    private void checkAccess(Long storeId) {
        if (!SecurityUtils.getDataScope().canAccessStore(storeId)) {
            throw new BizException("framework.error.storeNotAccessible");
        }
    }

    private void checkRefs(StoreInfo store) {
        if (store.getRegionId() != null) {
            regionService.requireEnabled(store.getRegionId());
        }
        if (store.getSupplyStoreId() != null) {
            StoreInfo supply = get(store.getSupplyStoreId());
            if (supply.getType() != StoreType.HQ && supply.getType() != StoreType.DC) {
                throw new BizException("store.error.supplyStoreMustHqOrDc", supply.getName());
            }
        }
        if (store.getPriceSchemeId() != null) {
            priceSchemeService.requireEnabled(store.getPriceSchemeId());
        }
        if (store.getDeliverySchemeId() != null) {
            priceSchemeService.requireEnabled(store.getDeliverySchemeId());
        }
        if (store.getType() != StoreType.FRANCHISE) {
            store.setFranchisee(null);
            store.setContractStart(null);
            store.setContractEnd(null);
            store.setDeposit(null);
            store.setSettleMode(null);
            store.setDeliverySchemeId(null);
        } else if (store.getContractStart() != null && store.getContractEnd() != null
                && store.getContractEnd().isBefore(store.getContractStart())) {
            throw new BizException("store.error.contractDateInvalid");
        }
    }

    /** 租户套餐的门店数量上限（不含总部，§11.2）：已有门店（含停业、闭店）达到上限时不能再新增 */
    private void checkStoreLimit() {
        TenantQuotaLookup quotas = quotaLookup.getIfAvailable();
        Integer limit = quotas == null ? null : quotas.storeLimit();
        if (limit != null && storeMapper.selectCount(Wrappers.<StoreInfo>lambdaQuery().ne(StoreInfo::getType, StoreType.HQ)) >= limit) {
            throw new BizException("store.error.storeLimitReached", limit);
        }
    }

    private void checkSingleHq(Long selfId) {
        boolean exists = storeMapper.exists(Wrappers.<StoreInfo>lambdaQuery().eq(StoreInfo::getType, StoreType.HQ)
                .ne(selfId != null, StoreInfo::getId, selfId));
        if (exists) {
            throw new BizException("store.error.hqExists");
        }
    }

    private void checkCodeUnique(String code, Long id) {
        StoreInfo exist = storeMapper.selectOne(Wrappers.<StoreInfo>lambdaQuery().eq(StoreInfo::getCode, code));
        if (exist != null && !Objects.equals(exist.getId(), id)) {
            throw new BizException("store.error.storeCodeExists", code);
        }
    }

    private void fillNames(List<StoreInfo> stores) {
        if (stores.isEmpty()) {
            return;
        }
        Map<Long, String> regions = regionService.nameMap(stores.stream().map(StoreInfo::getRegionId).toList());
        List<Long> supplyIds = stores.stream().map(StoreInfo::getSupplyStoreId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> supplies = supplyIds.isEmpty() ? Map.of() : storeMapper.selectByIds(supplyIds).stream()
                .collect(Collectors.toMap(StoreInfo::getId, StoreInfo::getName));
        for (StoreInfo s : stores) {
            // Map.of() 不接受 null 键
            s.setRegionName(s.getRegionId() == null ? null : regions.get(s.getRegionId()));
            s.setSupplyStoreName(s.getSupplyStoreId() == null ? null : supplies.get(s.getSupplyStoreId()));
        }
    }
}
