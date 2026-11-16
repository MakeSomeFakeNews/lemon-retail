package com.lemon.erp.modules.pos.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.pos.entity.PosMember;
import com.lemon.erp.modules.pos.entity.PosMemberLevel;
import com.lemon.erp.modules.pos.mapper.PosMemberLevelMapper;
import com.lemon.erp.modules.pos.mapper.PosMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 会员等级：会员折扣（没有方案会员价的商品按 门店价 × 折扣），升级门槛（累计消费达到时自动升级，只升不降）
 */
@Service
@RequiredArgsConstructor
public class PosMemberLevelService {

    private final PosMemberLevelMapper levelMapper;
    private final PosMemberMapper memberMapper;

    public List<PosMemberLevel> list() {
        return levelMapper.selectWithCount();
    }

    public PosMemberLevel get(Long id) {
        PosMemberLevel l = id == null ? null : levelMapper.selectById(id);
        if (l == null) {
            throw new BizException("pos.error.levelNotFound");
        }
        return l;
    }

    public Long create(PosMemberLevel l) {
        l.setId(null);
        checkNameUnique(l.getName(), null);
        if (l.getStatus() == null) l.setStatus(EnableStatus.ENABLED);
        if (l.getSort() == null) l.setSort(0);
        levelMapper.insert(l);
        return l.getId();
    }

    public void update(Long id, PosMemberLevel l) {
        get(id);
        l.setId(id);
        checkNameUnique(l.getName(), id);
        levelMapper.updateById(l);
    }

    public void delete(List<Long> ids) {
        for (Long id : ids) {
            PosMemberLevel l = get(id);
            if (memberMapper.exists(Wrappers.<PosMember>lambdaQuery().eq(PosMember::getLevelId, id))) {
                throw new BizException("pos.error.levelInUse", l.getName());
            }
        }
        levelMapper.deleteByIds(ids);
    }

    /** 启用的等级（会员表单选择） */
    public void requireEnabled(Long id) {
        PosMemberLevel l = get(id);
        if (l.getStatus() != EnableStatus.ENABLED) {
            throw new BizException("pos.error.levelDisabled", l.getName());
        }
    }

    /**
     * 累计消费达到更高等级的门槛时升级（只升不降；没有门槛的等级只能手动设置）
     */
    public void upgrade(Long memberId) {
        PosMember m = memberMapper.selectById(memberId);
        if (m == null || m.getTotalAmount() == null) {
            return;
        }
        List<PosMemberLevel> levels = levelMapper.selectList(Wrappers.<PosMemberLevel>lambdaQuery()
                .eq(PosMemberLevel::getStatus, EnableStatus.ENABLED).isNotNull(PosMemberLevel::getUpgradeAmount));
        BigDecimal current = levels.stream().filter(l -> Objects.equals(l.getId(), m.getLevelId())).map(PosMemberLevel::getUpgradeAmount)
                .findFirst().orElse(BigDecimal.ZERO);
        levels.stream()
                .filter(l -> l.getUpgradeAmount().compareTo(m.getTotalAmount()) <= 0 && l.getUpgradeAmount().compareTo(current) > 0)
                .max(Comparator.comparing(PosMemberLevel::getUpgradeAmount))
                .ifPresent(l -> memberMapper.update(Wrappers.<PosMember>lambdaUpdate().set(PosMember::getLevelId, l.getId())
                        .eq(PosMember::getId, memberId)));
    }

    /** 新会员的默认等级：没有升级门槛或门槛为 0 的启用等级中排序最前的，没有时为空 */
    public Long defaultLevelId() {
        return levelMapper.selectList(Wrappers.<PosMemberLevel>lambdaQuery().eq(PosMemberLevel::getStatus, EnableStatus.ENABLED)
                        .orderByAsc(PosMemberLevel::getSort).orderByAsc(PosMemberLevel::getId)).stream()
                .filter(l -> l.getUpgradeAmount() == null || l.getUpgradeAmount().signum() == 0)
                .map(PosMemberLevel::getId).findFirst().orElse(null);
    }

    private void checkNameUnique(String name, Long id) {
        PosMemberLevel exist = levelMapper.selectOne(Wrappers.<PosMemberLevel>lambdaQuery().eq(PosMemberLevel::getName, name));
        if (exist != null && !Objects.equals(exist.getId(), id)) {
            throw new BizException("pos.error.levelNameExists", name);
        }
    }
}
