package com.lemon.erp.modules.pos.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.store.StoreLookup;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.dto.PosRequests;
import com.lemon.erp.modules.pos.entity.PosMember;
import com.lemon.erp.modules.pos.entity.PosPointsLog;
import com.lemon.erp.modules.pos.entity.PosSale;
import com.lemon.erp.modules.pos.entity.PosStoredValueLog;
import com.lemon.erp.modules.pos.entity.PosTerminal;
import com.lemon.erp.modules.pos.mapper.PosMemberMapper;
import com.lemon.erp.modules.pos.mapper.PosPointsLogMapper;
import com.lemon.erp.modules.pos.mapper.PosSaleMapper;
import com.lemon.erp.modules.pos.mapper.PosStoredValueLogMapper;
import com.lemon.erp.modules.pos.model.MemberGender;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * 会员：手机号在租户内唯一，会员号按编码规则（HY）生成；全部门店通用（会员列表不按门店过滤，消费记录按门店过滤）。
 * 手机号默认脱敏（138****0000），有「查看会员手机号」{@code pos:member:sensitive} 权限时返回完整号码
 */
@Service
@RequiredArgsConstructor
public class PosMemberService {

    static final String SENSITIVE_PERM = "pos:member:sensitive";

    private final PosMemberMapper memberMapper;
    private final PosSaleMapper saleMapper;
    private final PosStoredValueLogMapper storedValueLogMapper;
    private final PosPointsLogMapper pointsLogMapper;
    private final PosMemberLevelService levelService;
    private final StoreLookup storeLookup;
    private final DocNoService docNoService;
    private final PermissionService permissionService;

    public PageResult<PosMember> page(long current, long size, PosQueries.Member q) {
        Page<PosMember> page = Page.of(current, size);
        memberMapper.selectPageList(page, q);
        mask(page.getRecords());
        return PageResult.of(page);
    }

    public PosMember get(Long id) {
        PosMember m = id == null ? null : memberMapper.selectDetail(id);
        if (m == null) {
            throw new BizException("pos.error.memberNotFound");
        }
        return m;
    }

    public PosMember detail(Long id) {
        PosMember m = get(id);
        mask(List.of(m));
        return m;
    }

    /** 收银识别会员：按手机号或会员号精确查找，停用的会员不能使用 */
    public PosMember identify(String code) {
        PosMember m = StringUtils.hasText(code) ? memberMapper.selectByCode(code.trim()) : null;
        if (m == null) {
            throw new BizException("pos.error.memberNotFound");
        }
        if (m.getStatus() != EnableStatus.ENABLED) {
            throw new BizException("pos.error.memberDisabled", m.getMemberCode());
        }
        mask(List.of(m));
        return m;
    }

    /** 结算使用的会员（不脱敏、不校验权限），停用的报错 */
    public PosMember requireActive(Long id) {
        PosMember m = get(id);
        if (m.getStatus() != EnableStatus.ENABLED) {
            throw new BizException("pos.error.memberDisabled", m.getMemberCode());
        }
        return m;
    }

    @Transactional
    public Long create(PosMember m) {
        m.setId(null);
        prepare(m, null);
        m.setTotalAmount(BigDecimal.ZERO);
        m.setVisitCount(0);
        m.setPoints(BigDecimal.ZERO);
        m.setBalance(BigDecimal.ZERO);
        try {
            memberMapper.insert(m);
        } catch (DuplicateKeyException e) {
            throw new BizException("pos.error.memberPhoneExists", m.getPhone());
        }
        return m.getId();
    }

    /** 收银时快速登记：开卡门店为收银台的门店，等级为默认等级 */
    @Transactional
    public PosMember quickRegister(PosRequests.QuickMember req, PosTerminal terminal) {
        PosMember m = new PosMember();
        m.setPhone(req.phone().trim());
        m.setName(req.name());
        m.setStoreId(terminal.getStoreId());
        Long id = create(m);
        PosMember created = get(id);
        mask(List.of(created));
        return created;
    }

    /** 修改资料（累计消费、次数、积分、储值不能修改） */
    public void update(Long id, PosMember m) {
        PosMember old = get(id);
        m.setId(id);
        if (!StringUtils.hasText(m.getMemberCode())) {
            m.setMemberCode(old.getMemberCode());
        }
        // 没有查看手机号权限的人看到的是脱敏号码，原样提交时保持不变
        if (m.getPhone() != null && m.getPhone().contains("*")) {
            m.setPhone(old.getPhone());
        }
        prepare(m, id);
        m.setTotalAmount(null);
        m.setVisitCount(null);
        m.setPoints(null);
        m.setBalance(null);
        m.setLastVisitTime(null);
        try {
            memberMapper.updateById(m);
        } catch (DuplicateKeyException e) {
            throw new BizException("pos.error.memberPhoneExists", m.getPhone());
        }
    }

    public void changeStatus(Long id, EnableStatus status) {
        get(id);
        memberMapper.update(Wrappers.<PosMember>lambdaUpdate().set(PosMember::getStatus, status).eq(PosMember::getId, id));
    }

    /** 有消费、积分或储值记录的会员不能删除（停用） */
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            PosMember m = get(id);
            if (saleMapper.exists(Wrappers.<PosSale>lambdaQuery().eq(PosSale::getMemberId, id))
                    || storedValueLogMapper.exists(Wrappers.<PosStoredValueLog>lambdaQuery().eq(PosStoredValueLog::getMemberId, id))
                    || pointsLogMapper.exists(Wrappers.<PosPointsLog>lambdaQuery().eq(PosPointsLog::getMemberId, id))) {
                throw new BizException("pos.error.memberInUse", m.getMemberCode());
            }
        }
        memberMapper.deleteByIds(ids);
    }

    /** 结算后更新消费统计并按累计消费升级（退货为负金额、次数 0） */
    public void addStats(Long memberId, BigDecimal amount, int visits) {
        memberMapper.addStats(memberId, amount, visits);
        if (amount.signum() > 0) {
            levelService.upgrade(memberId);
        }
    }

    private void prepare(PosMember m, Long id) {
        if (!StringUtils.hasText(m.getMemberCode())) {
            m.setMemberCode(docNoService.next(PosDocNos.MEMBER));
        }
        m.setPhone(m.getPhone().trim());
        if (m.getPhone().contains("*")) {
            throw new BizException("pos.error.memberPhoneMasked");
        }
        PosMember samePhone = memberMapper.selectOne(Wrappers.<PosMember>lambdaQuery().eq(PosMember::getPhone, m.getPhone()));
        if (samePhone != null && !Objects.equals(samePhone.getId(), id)) {
            throw new BizException("pos.error.memberPhoneExists", m.getPhone());
        }
        PosMember sameCode = memberMapper.selectOne(Wrappers.<PosMember>lambdaQuery().eq(PosMember::getMemberCode, m.getMemberCode()));
        if (sameCode != null && !Objects.equals(sameCode.getId(), id)) {
            throw new BizException("pos.error.memberCodeExists", m.getMemberCode());
        }
        if (m.getLevelId() != null) {
            levelService.requireEnabled(m.getLevelId());
        } else if (id == null) {
            m.setLevelId(levelService.defaultLevelId());
        }
        if (m.getStoreId() != null) {
            storeLookup.require(m.getStoreId());
        }
        if (m.getGender() == null) m.setGender(MemberGender.UNKNOWN);
        if (m.getStatus() == null && id == null) m.setStatus(EnableStatus.ENABLED);
    }

    private void mask(List<PosMember> members) {
        if (permissionService.hasPerm(SENSITIVE_PERM)) {
            return;
        }
        members.forEach(m -> m.setPhone(maskPhone(m.getPhone())));
    }

    /** 手机号脱敏：保留前 3 位和后 4 位，过短时只保留后 2 位 */
    public static String maskPhone(String phone) {
        if (phone == null) {
            return null;
        }
        int n = phone.length();
        if (n >= 8) {
            return phone.substring(0, 3) + "****" + phone.substring(n - 4);
        }
        return "*".repeat(Math.max(0, n - 2)) + phone.substring(Math.max(0, n - 2));
    }
}
