package com.lemon.erp.modules.dist.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermissionRule;
import com.lemon.erp.framework.store.StoreType;
import com.lemon.erp.modules.store.entity.StoreInfo;
import com.lemon.erp.modules.store.service.StoreInfoService;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsWarehouse;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 配送模块的公共校验：仓库与门店、供货方（总部 / 配送中心）、物料、门店范围
 */
@Component
@RequiredArgsConstructor
public class DistSupport {

    /** 要货单：按要货门店、供货门店的仓库过滤（双方都能看到），不按制单人部门过滤（总部审核门店的单据） */
    public static final DataPermissionRule REQUISITION_RULE = DataPermissionRule.byWarehouse("dist_requisition", "warehouse_id,supply_warehouse_id");
    /** 配送单 / 退仓单：按发货、收货仓库过滤 */
    public static final DataPermissionRule DELIVERY_RULE = DataPermissionRule.byWarehouse("dist_delivery", "from_warehouse_id,to_warehouse_id");

    private final WmsWarehouseService warehouseService;
    private final WmsMaterialService materialService;
    private final StoreInfoService storeService;

    public WmsWarehouse warehouse(Long id) {
        WmsWarehouse w = warehouseService.get(id);
        if (w.getStoreId() == null) {
            throw new BizException("dist.error.warehouseWithoutStore", w.getName());
        }
        return w;
    }

    /** 当前用户可使用（门店可管辖、可以开单）的仓库 */
    public WmsWarehouse usableWarehouse(Long id) {
        WmsWarehouse w = warehouse(id);
        warehouseService.checkUsable(w);
        return w;
    }

    public StoreInfo store(Long id) {
        return storeService.get(id);
    }

    /** 供货方（发货的配送中心 / 退仓的收货方）必须是总部或配送中心的仓库 */
    public StoreInfo supplyStore(WmsWarehouse w) {
        StoreInfo store = storeService.get(w.getStoreId());
        if (store.getType() != StoreType.HQ && store.getType() != StoreType.DC) {
            throw new BizException("dist.error.supplyMustBeHqOrDc", w.getName());
        }
        return store;
    }

    /**
     * 门店的默认供货仓库：上级配送门店（为空取总部）的默认仓库
     */
    public Long defaultSupplyWarehouse(StoreInfo store) {
        Long supplyStoreId = store.getSupplyStoreId();
        StoreInfo supply = supplyStoreId == null ? null : storeService.get(supplyStoreId);
        if (supply == null || supply.getDefaultWarehouseId() == null) {
            throw new BizException("dist.error.supplyWarehouseRequired", store.getName());
        }
        return supply.getDefaultWarehouseId();
    }

    /** 当前用户可管辖该门店（门店范围）时为 true；未登录的系统操作不受限 */
    public static boolean canAccessStore(Long storeId) {
        return SecurityUtils.getDataScopeOrAll().canAccessStore(storeId);
    }

    public static void checkStoreAccess(Long storeId, String storeName) {
        if (!canAccessStore(storeId)) {
            throw new BizException("dist.error.storeNotAccessible", storeName == null ? String.valueOf(storeId) : storeName);
        }
    }

    /** 物料必须存在、不是多规格款式；明细中物料不能重复 */
    public Map<Long, WmsMaterial> materials(Collection<Long> ids, boolean unique) {
        if (unique) {
            Set<Long> seen = new HashSet<>();
            for (Long id : ids) {
                if (!seen.add(id)) {
                    throw new BizException("dist.error.duplicateMaterial", materialService.get(id).getName());
                }
            }
        }
        Map<Long, WmsMaterial> map = materialService.mapByIds(ids);
        for (Long id : ids) {
            WmsMaterial m = map.get(id);
            if (m == null) {
                throw new BizException("dist.error.materialNotFound");
            }
            if (Integer.valueOf(1).equals(m.getIsTemplate())) {
                throw new BizException("dist.error.materialIsTemplate", m.getName());
            }
        }
        return map;
    }

    public static List<String> names(Collection<? extends Enum<?>> values) {
        return values.stream().map(Enum::name).toList();
    }
}
