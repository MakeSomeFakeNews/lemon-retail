package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.ref.RefType;
import com.lemon.erp.framework.ref.ReferenceGuard;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataScope;
import com.lemon.erp.framework.store.StoreBrief;
import com.lemon.erp.framework.store.StoreLookup;
import com.lemon.erp.modules.wms.dto.OptionItem;
import com.lemon.erp.modules.wms.entity.WmsCheck;
import com.lemon.erp.modules.wms.entity.WmsLocation;
import com.lemon.erp.modules.wms.entity.WmsStock;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.entity.WmsWarehouse;
import com.lemon.erp.modules.wms.mapper.WmsCheckMapper;
import com.lemon.erp.modules.wms.mapper.WmsLocationMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockDocMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockMapper;
import com.lemon.erp.modules.wms.mapper.WmsWarehouseMapper;
import com.lemon.erp.modules.wms.model.WarehouseType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WmsWarehouseService {

    private final WmsWarehouseMapper warehouseMapper;
    private final WmsLocationMapper locationMapper;
    private final WmsStockMapper stockMapper;
    private final WmsStockDocMapper docMapper;
    private final WmsCheckMapper checkMapper;
    private final ReferenceGuard referenceGuard;
    private final WmsStockLimitService limitService;
    private final StoreLookup storeLookup;

    /**
     * 可管辖门店的仓库
     *
     * @param keyword 编码或名称，模糊匹配
     */
    @DataPermission(table = "wms_warehouse", deptColumn = "", userColumn = "", storeColumn = "store_id")
    public PageResult<WmsWarehouse> page(long current, long size, String keyword, EnableStatus status, Long storeId) {
        Page<WmsWarehouse> page = warehouseMapper.selectPage(Page.of(current, size), Wrappers.<WmsWarehouse>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w.like(WmsWarehouse::getCode, keyword).or().like(WmsWarehouse::getName, keyword))
                .eq(status != null, WmsWarehouse::getStatus, status)
                .eq(storeId != null, WmsWarehouse::getStoreId, storeId)
                .orderByAsc(WmsWarehouse::getSort)
                .orderByAsc(WmsWarehouse::getId));
        Map<Long, StoreBrief> stores = storeLookup.findAll(page.getRecords().stream().map(WmsWarehouse::getStoreId).toList());
        page.getRecords().forEach(w -> w.setStoreName(Optional.ofNullable(stores.get(w.getStoreId())).map(StoreBrief::name).orElse(null)));
        return PageResult.of(page);
    }

    public WmsWarehouse get(Long id) {
        WmsWarehouse warehouse = id == null ? null : warehouseMapper.selectById(id);
        if (warehouse == null) {
            throw new BizException("wms.error.warehouseDoesNotExist");
        }
        return warehouse;
    }

    /** 可管辖门店的启用仓库，供单据等页面下拉选择 */
    public List<OptionItem> options() {
        return options(null, false);
    }

    /**
     * 启用的仓库
     *
     * @param storeId 只返回该门店的仓库，为空不限
     * @param all     true 时返回全部门店的仓库（如调拨的调入仓库），否则只返回可管辖门店的仓库
     */
    public List<OptionItem> options(Long storeId, boolean all) {
        DataScope scope = SecurityUtils.getDataScopeOrAll();
        return warehouseMapper.selectList(Wrappers.<WmsWarehouse>lambdaQuery()
                        .select(WmsWarehouse::getId, WmsWarehouse::getCode, WmsWarehouse::getName, WmsWarehouse::getType, WmsWarehouse::getStoreId)
                        .eq(WmsWarehouse::getStatus, EnableStatus.ENABLED)
                        .eq(storeId != null, WmsWarehouse::getStoreId, storeId)
                        .orderByAsc(WmsWarehouse::getSort)
                        .orderByAsc(WmsWarehouse::getId))
                .stream()
                .filter(w -> all || scope.canAccessStore(w.getStoreId()))
                .map(w -> new OptionItem(w.getId(), w.getCode(), w.getName(), w.getType() == null ? null : w.getType().name(), w.getStoreId()))
                .toList();
    }

    /**
     * 单据使用仓库前校验：当前用户可管辖仓库所属的门店，且门店可以开单（停业、闭店的门店不能开单）。
     * 未登录的系统操作不校验权限
     */
    public void checkUsable(WmsWarehouse warehouse) {
        if (warehouse.getStoreId() == null) {
            return;
        }
        if (!SecurityUtils.getDataScopeOrAll().canAccessStore(warehouse.getStoreId())) {
            throw new BizException("wms.error.warehouseStoreNotAccessible", warehouse.getName());
        }
        storeLookup.requireTrading(warehouse.getStoreId());
    }

    /** 仓库所属门店（单据的门店），仓库不存在时为空 */
    public Map<Long, Long> storeIds(Collection<Long> warehouseIds) {
        List<Long> ids = warehouseIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> map = new HashMap<>();
        warehouseMapper.selectList(Wrappers.<WmsWarehouse>lambdaQuery().select(WmsWarehouse::getId, WmsWarehouse::getStoreId)
                .in(WmsWarehouse::getId, ids)).forEach(w -> map.put(w.getId(), w.getStoreId()));
        return map;
    }

    /** 门店的全部仓库 ID（含停用） */
    public List<Long> idsOfStores(Collection<Long> storeIds) {
        if (storeIds.isEmpty()) {
            return List.of();
        }
        return warehouseMapper.selectList(Wrappers.<WmsWarehouse>lambdaQuery().select(WmsWarehouse::getId)
                .in(WmsWarehouse::getStoreId, storeIds)).stream().map(WmsWarehouse::getId).toList();
    }

    /**
     * 新建门店时创建同名仓库（编码沿用门店编码，已被占用时加序号）
     *
     * @return 仓库 ID
     */
    public Long createForStore(Long storeId, String storeCode, String storeName) {
        String code = storeCode;
        for (int i = 1; warehouseMapper.exists(Wrappers.<WmsWarehouse>lambdaQuery().eq(WmsWarehouse::getCode, code)); i++) {
            code = storeCode + "-" + i;
        }
        WmsWarehouse w = new WmsWarehouse();
        w.setCode(code);
        w.setName(storeName);
        w.setStoreId(storeId);
        w.setType(WarehouseType.NORMAL);
        w.setStatus(EnableStatus.ENABLED);
        w.setSort(0);
        warehouseMapper.insert(w);
        return w.getId();
    }

    /** 门店的默认仓库必须是该门店启用的仓库 */
    public void checkStoreWarehouse(Long storeId, Long warehouseId) {
        WmsWarehouse w = get(warehouseId);
        if (!Objects.equals(w.getStoreId(), storeId)) {
            throw new BizException("wms.error.warehouseNotInStore", w.getName());
        }
        if (w.getStatus() != EnableStatus.ENABLED) {
            throw new BizException("wms.error.disabled", Messages.get("wms.label.warehouse"), w.getName());
        }
    }

    public void create(WmsWarehouse warehouse) {
        checkCodeUnique(warehouse.getCode(), null);
        warehouse.setId(null);
        warehouse.setStoreId(checkStore(warehouse.getStoreId()));
        warehouse.setType(normalizeType(warehouse.getType()));
        checkPartner(warehouse);
        if (warehouse.getStatus() == null) warehouse.setStatus(EnableStatus.ENABLED);
        if (warehouse.getSort() == null) warehouse.setSort(0);
        warehouseMapper.insert(warehouse);
    }

    @Transactional
    public void update(Long id, WmsWarehouse warehouse) {
        WmsWarehouse old = get(id);
        checkAccessible(old);
        checkCodeUnique(warehouse.getCode(), id);
        warehouse.setStoreId(checkStore(warehouse.getStoreId() == null ? old.getStoreId() : warehouse.getStoreId()));
        if (!Objects.equals(old.getStoreId(), warehouse.getStoreId()) && used(id)) {
            throw new BizException("wms.error.warehouseUsedCannotChangeStore", old.getName());
        }
        warehouse.setId(id);
        warehouse.setType(normalizeType(warehouse.getType()));
        checkPartner(warehouse);
        if (warehouse.getSort() == null) warehouse.setSort(0);
        warehouse.setStatus(null);
        warehouseMapper.updateById(warehouse);
        warehouseMapper.update(Wrappers.<WmsWarehouse>lambdaUpdate().set(WmsWarehouse::getPartnerId, warehouse.getPartnerId())
                .eq(WmsWarehouse::getId, id));
    }

    public void changeStatus(Long id, EnableStatus status) {
        checkAccessible(get(id));
        WmsWarehouse entity = new WmsWarehouse();
        entity.setId(id);
        entity.setStatus(status);
        warehouseMapper.updateById(entity);
    }

    /** 已有库存或单据的仓库不能删除（可以停用），删除时一并删除其库位 */
    @Transactional
    public void delete(List<Long> ids) {
        for (WmsWarehouse warehouse : warehouseMapper.selectByIds(ids)) {
            Long id = warehouse.getId();
            checkAccessible(warehouse);
            if (used(id)) {
                throw new BizException("wms.error.warehouseHasStockDocumentsCannotDeleted", warehouse.getName());
            }
            referenceGuard.checkDeletable(RefType.WAREHOUSE, id, Messages.get("wms.text.warehouse", warehouse.getName()));
        }
        locationMapper.delete(Wrappers.<WmsLocation>lambdaQuery().in(WmsLocation::getWarehouseId, ids));
        limitService.deleteByWarehouses(ids);
        warehouseMapper.deleteByIds(ids);
    }

    /** 有库存或单据 */
    private boolean used(Long id) {
        return stockMapper.exists(Wrappers.<WmsStock>lambdaQuery().eq(WmsStock::getWarehouseId, id))
                || docMapper.exists(Wrappers.<WmsStockDoc>lambdaQuery()
                .and(w -> w.eq(WmsStockDoc::getWarehouseId, id).or().eq(WmsStockDoc::getToWarehouseId, id)))
                || checkMapper.exists(Wrappers.<WmsCheck>lambdaQuery().eq(WmsCheck::getWarehouseId, id));
    }

    /** 门店为空时属于总部；只能放在可管辖的门店下 */
    private Long checkStore(Long storeId) {
        Long id = storeId == null ? storeLookup.headquarters().id() : storeLookup.require(storeId).id();
        if (!SecurityUtils.getDataScopeOrAll().canAccessStore(id)) {
            throw new BizException("framework.error.storeNotAccessible");
        }
        return id;
    }

    /** 维护仓库资料需要可管辖其所属门店 */
    private void checkAccessible(WmsWarehouse warehouse) {
        if (!SecurityUtils.getDataScopeOrAll().canAccessStore(warehouse.getStoreId())) {
            throw new BizException("wms.error.warehouseStoreNotAccessible", warehouse.getName());
        }
    }

    private void checkCodeUnique(String code, Long id) {
        WmsWarehouse exist = warehouseMapper.selectOne(Wrappers.<WmsWarehouse>lambdaQuery().eq(WmsWarehouse::getCode, code));
        if (exist != null && !Objects.equals(exist.getId(), id)) {
            throw new BizException("wms.error.warehouseCodeAlreadyExists");
        }
    }

    /** 未选择仓库类型时为普通仓 */
    private static WarehouseType normalizeType(WarehouseType type) {
        return type == null ? WarehouseType.NORMAL : type;
    }

    /** 寄售仓必须指定客户（客户寄售仓）/ 供应商（供应商寄售仓），其他仓库不关联往来单位 */
    private static void checkPartner(WmsWarehouse w) {
        if (w.getType().isConsign()) {
            if (w.getPartnerId() == null) {
                if (w.getType() == WarehouseType.CONSIGN_OUT) {
                    throw new BizException("wms.error.selectCustomerCustomerConsignmentWarehouse");
                }
                throw new BizException("wms.error.selectSupplierSupplierConsignmentWarehouse");
            }
        } else {
            w.setPartnerId(null);
        }
    }
}
