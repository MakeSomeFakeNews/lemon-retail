package com.lemon.erp.modules.store.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.ref.RefType;
import com.lemon.erp.framework.ref.ReferenceChecker;
import com.lemon.erp.modules.store.entity.StoreInfo;
import com.lemon.erp.modules.store.mapper.StoreInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 删除价格方案、部门前检查门店是否引用（零售价方案、配送价方案、对应部门）
 */
@Component
@RequiredArgsConstructor
public class StoreReferenceChecker implements ReferenceChecker {

    private final StoreInfoMapper storeMapper;

    @Override
    public String findReference(RefType type, Long id) {
        boolean used = switch (type) {
            case PRICE_SCHEME -> storeMapper.exists(Wrappers.<StoreInfo>lambdaQuery()
                    .and(w -> w.eq(StoreInfo::getPriceSchemeId, id).or().eq(StoreInfo::getDeliverySchemeId, id)));
            case DEPT -> storeMapper.exists(Wrappers.<StoreInfo>lambdaQuery().eq(StoreInfo::getDeptId, id));
            default -> false;
        };
        return used ? "store.label.store" : null;
    }
}
