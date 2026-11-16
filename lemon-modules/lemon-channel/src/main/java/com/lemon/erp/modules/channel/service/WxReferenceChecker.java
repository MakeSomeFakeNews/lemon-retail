package com.lemon.erp.modules.channel.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.ref.RefType;
import com.lemon.erp.framework.ref.ReferenceChecker;
import com.lemon.erp.modules.channel.entity.WxOrderItem;
import com.lemon.erp.modules.channel.entity.WxProduct;
import com.lemon.erp.modules.channel.entity.WxShop;
import com.lemon.erp.modules.channel.mapper.WxOrderItemMapper;
import com.lemon.erp.modules.channel.mapper.WxProductMapper;
import com.lemon.erp.modules.channel.mapper.WxShopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 删除门店、仓库、客户、物料前检查微信小店是否引用（店铺绑定的门店 / 发货仓库 / 客户，商品映射、渠道订单的物料）
 */
@Component
@RequiredArgsConstructor
public class WxReferenceChecker implements ReferenceChecker {

    private final WxShopMapper shopMapper;
    private final WxProductMapper productMapper;
    private final WxOrderItemMapper itemMapper;

    @Override
    public String findReference(RefType type, Long id) {
        return switch (type) {
            case STORE -> shopMapper.exists(Wrappers.<WxShop>lambdaQuery().eq(WxShop::getStoreId, id)) ? "channel.label.wxShop" : null;
            case WAREHOUSE -> shopMapper.exists(Wrappers.<WxShop>lambdaQuery().eq(WxShop::getWarehouseId, id)) ? "channel.label.wxShop" : null;
            case CUSTOMER -> shopMapper.exists(Wrappers.<WxShop>lambdaQuery().eq(WxShop::getCustomerId, id)) ? "channel.label.wxShop" : null;
            case MATERIAL -> productMapper.exists(Wrappers.<WxProduct>lambdaQuery().eq(WxProduct::getMaterialId, id))
                    || itemMapper.exists(Wrappers.<WxOrderItem>lambdaQuery().eq(WxOrderItem::getMaterialId, id)) ? "channel.label.wxProduct" : null;
            default -> null;
        };
    }
}
