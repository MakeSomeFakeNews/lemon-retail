package com.lemon.erp.modules.pos.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.ref.RefType;
import com.lemon.erp.framework.ref.ReferenceChecker;
import com.lemon.erp.modules.pos.entity.PosMember;
import com.lemon.erp.modules.pos.entity.PosSaleItem;
import com.lemon.erp.modules.pos.entity.PosSpecial;
import com.lemon.erp.modules.pos.entity.PosSpecialItem;
import com.lemon.erp.modules.pos.entity.PosSpecialTarget;
import com.lemon.erp.modules.pos.entity.PosTerminal;
import com.lemon.erp.modules.pos.mapper.PosMemberMapper;
import com.lemon.erp.modules.pos.mapper.PosSaleItemMapper;
import com.lemon.erp.modules.pos.mapper.PosSpecialItemMapper;
import com.lemon.erp.modules.pos.mapper.PosSpecialMapper;
import com.lemon.erp.modules.pos.mapper.PosSpecialTargetMapper;
import com.lemon.erp.modules.pos.mapper.PosTerminalMapper;
import com.lemon.erp.modules.pos.model.SpecialScope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 删除门店、仓库、物料、区域前检查零售前台是否引用：收银台（门店、仓库）、POS 单据与特价活动（物料）、会员（开卡门店）、特价活动的适用门店 / 区域
 */
@Component
@RequiredArgsConstructor
public class PosReferenceChecker implements ReferenceChecker {

    private final PosTerminalMapper terminalMapper;
    private final PosSaleItemMapper saleItemMapper;
    private final PosSpecialItemMapper specialItemMapper;
    private final PosSpecialTargetMapper targetMapper;
    private final PosSpecialMapper specialMapper;
    private final PosMemberMapper memberMapper;

    @Override
    public String findReference(RefType type, Long id) {
        return switch (type) {
            case STORE -> terminalMapper.exists(Wrappers.<PosTerminal>lambdaQuery().eq(PosTerminal::getStoreId, id)) ? "pos.label.terminal"
                    : memberMapper.exists(Wrappers.<PosMember>lambdaQuery().eq(PosMember::getStoreId, id)) ? "pos.label.member"
                    : targetUsed(id, SpecialScope.STORES) ? "pos.label.special" : null;
            case REGION -> targetUsed(id, SpecialScope.REGIONS) ? "pos.label.special" : null;
            case WAREHOUSE -> terminalMapper.exists(Wrappers.<PosTerminal>lambdaQuery().eq(PosTerminal::getWarehouseId, id)) ? "pos.label.terminal" : null;
            case MATERIAL -> saleItemMapper.exists(Wrappers.<PosSaleItem>lambdaQuery().eq(PosSaleItem::getMaterialId, id)) ? "pos.label.saleDoc"
                    : specialItemMapper.exists(Wrappers.<PosSpecialItem>lambdaQuery().eq(PosSpecialItem::getMaterialId, id)) ? "pos.label.special" : null;
            default -> null;
        };
    }

    private boolean targetUsed(Long targetId, SpecialScope scope) {
        return targetMapper.selectList(Wrappers.<PosSpecialTarget>lambdaQuery().eq(PosSpecialTarget::getTargetId, targetId)).stream()
                .anyMatch(t -> specialMapper.exists(Wrappers.<PosSpecial>lambdaQuery().eq(PosSpecial::getId, t.getSpecialId())
                        .eq(PosSpecial::getStoreScope, scope)));
    }
}
