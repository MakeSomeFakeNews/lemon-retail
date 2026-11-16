package com.lemon.erp.modules.store.service;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.store.StoreStatus;
import com.lemon.erp.framework.store.StoreType;
import com.lemon.erp.framework.tenant.TenantInitializer;
import com.lemon.erp.modules.fin.service.FinAccountService;
import com.lemon.erp.modules.store.entity.StoreInfo;
import com.lemon.erp.modules.store.mapper.StoreInfoMapper;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 新租户创建总部门店和同名仓库，并把现有的现金、银行科目（从平台复制的会计科目）归属到总部。
 * 在会计科目复制之后执行
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class StoreTenantInitializer implements TenantInitializer {

    /** 总部的默认编码 */
    static final String HQ_CODE = "MD000";

    private final StoreInfoMapper storeMapper;
    private final WmsWarehouseService warehouseService;
    private final FinAccountService accountService;

    @Override
    public void initialize(Context context) {
        StoreInfo hq = new StoreInfo();
        hq.setCode(HQ_CODE);
        hq.setName(Messages.get(Messages.DEFAULT_LOCALE, "store.text.headquarters"));
        hq.setType(StoreType.HQ);
        hq.setStatus(StoreStatus.OPEN);
        hq.setSort(0);
        storeMapper.insert(hq);
        hq.setDefaultWarehouseId(warehouseService.createForStore(hq.getId(), HQ_CODE, hq.getName()));
        hq.setDefaultAccountId(accountService.assignCashAccountsToStore(hq.getId()));
        storeMapper.updateById(hq);
    }
}
