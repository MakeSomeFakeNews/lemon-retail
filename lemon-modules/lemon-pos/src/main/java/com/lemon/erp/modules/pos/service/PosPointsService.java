package com.lemon.erp.modules.pos.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.dto.PosRequests;
import com.lemon.erp.modules.pos.dto.PosViews;
import com.lemon.erp.modules.pos.entity.PosMember;
import com.lemon.erp.modules.pos.entity.PosMemberRule;
import com.lemon.erp.modules.pos.entity.PosPointsLog;
import com.lemon.erp.modules.pos.mapper.PosMemberMapper;
import com.lemon.erp.modules.pos.mapper.PosPointsLogMapper;
import com.lemon.erp.modules.pos.model.PointsType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 会员积分：所有变动经 {@link #change} 更新会员积分并写积分流水（同一事务）。
 * <ul>
 *   <li>消费获得：积分 = 取整(可积分金额 ÷ 每消费金额 × 获得积分 × 等级倍率)，可积分金额 = 不积分商品以外各行的实收 × (应收 − 积分抵现) ÷ 应收</li>
 *   <li>抵现：扣减时积分不足报错（条件更新）；退货 / 作废按原收款退回积分</li>
 *   <li>退货扣回：按退款占原单应收的比例扣回获得的积分，全部退完时扣回剩余；会员已用掉时允许为负</li>
 *   <li>调整、清零：{@code pos:member:points}（敏感操作，记操作日志）</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PosPointsService {

    static final String POINTS_PERM = "pos:member:points";

    private final PosMemberMapper memberMapper;
    private final PosPointsLogMapper logMapper;
    private final PosMemberService memberService;

    public PageResult<PosPointsLog> page(long current, long size, PosQueries.PointsLog q) {
        Page<PosPointsLog> page = Page.of(current, size);
        logMapper.selectPageList(page, q, q.end() == null ? null : q.end().plusDays(1).atStartOfDay());
        return PageResult.of(page);
    }

    /**
     * 积分变动：points 增加为正、减少为负
     *
     * @param allowNegative 减少时允许积分为负（退货扣回已用掉的积分、调整）；否则积分不足报错
     * @return 变动后积分
     */
    public BigDecimal change(Long memberId, Long storeId, PointsType type, BigDecimal points, Long saleId, String sourceNo, String reason,
                             boolean allowNegative) {
        if (points.signum() == 0) {
            return memberMapper.selectAccount(memberId).getPoints();
        }
        if (points.signum() < 0 && !allowNegative) {
            if (memberMapper.deductPoints(memberId, points.negate()) == 0) {
                PosMember m = memberMapper.selectAccount(memberId);
                throw new BizException("pos.error.pointsInsufficient", m == null ? "" : m.getPoints().stripTrailingZeros().toPlainString(),
                        points.negate().stripTrailingZeros().toPlainString());
            }
        } else {
            memberMapper.addPoints(memberId, points);
        }
        BigDecimal after = memberMapper.selectAccount(memberId).getPoints();
        PosPointsLog l = new PosPointsLog();
        l.setMemberId(memberId);
        l.setStoreId(storeId);
        l.setChangeType(type);
        l.setPoints(points);
        l.setBalanceAfter(after);
        l.setSaleId(saleId);
        l.setSourceNo(sourceNo);
        l.setReason(reason);
        l.setCreateBy(SecurityUtils.getUserIdOrNull());
        logMapper.insert(l);
        return after;
    }

    /**
     * 消费可得积分：取整(可积分金额 ÷ 每消费金额 × 获得积分 × 等级倍率)；规则未启用或会员为空时为 0
     */
    public static BigDecimal earned(PosMemberRule rule, PosMember member, BigDecimal eligibleAmount) {
        if (member == null || !Boolean.TRUE.equals(rule.getPointsEnabled()) || eligibleAmount.signum() <= 0
                || rule.getEarnAmount().signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = member.getPointsRate() == null ? BigDecimal.ONE : member.getPointsRate();
        return eligibleAmount.multiply(rule.getEarnPoints()).multiply(rate)
                .divide(rule.getEarnAmount(), 0, RoundingMode.DOWN);
    }

    /** 手工调整（pos:member:points 由控制器校验）：减少时允许为负 */
    @Transactional
    public BigDecimal adjust(PosRequests.PointsAdjust req) {
        memberService.get(req.memberId());
        if (req.points().signum() == 0) {
            throw new BizException("pos.error.pointsZero");
        }
        return change(req.memberId(), null, PointsType.ADJUST, req.points().setScale(0, RoundingMode.DOWN), null, null, req.reason(), true);
    }

    /**
     * 积分清零：清除 before 之前获得、至今还没有用掉的积分（按先获得先使用：可清零 = min(当前积分, before 前增加的积分 − 全部减少的积分)），
     * before 为空时清除全部积分。逐个会员条件扣减并写清零流水
     */
    @Transactional
    public PosViews.PointsCleared clear(PosRequests.PointsClear req) {
        LocalDateTime before = req.before() == null ? null : req.before().atStartOfDay();
        List<PosPointsLogMapper.Clearable> rows = logMapper.selectClearable(before);
        List<PosPointsLog> logs = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        Long userId = SecurityUtils.getUserIdOrNull();
        for (PosPointsLogMapper.Clearable r : rows) {
            BigDecimal x = before == null ? r.getPoints() : r.getPoints().min(r.getEarnedBefore().subtract(r.getSpent()));
            if (x.signum() <= 0 || memberMapper.deductPoints(r.getMemberId(), x) == 0) {
                continue;
            }
            PosPointsLog l = new PosPointsLog();
            l.setMemberId(r.getMemberId());
            l.setChangeType(PointsType.CLEAR);
            l.setPoints(x.negate());
            l.setBalanceAfter(r.getPoints().subtract(x));
            l.setReason(req.reason());
            l.setCreateBy(userId);
            logs.add(l);
            total = total.add(x);
        }
        for (int i = 0; i < logs.size(); i += 500) {
            logMapper.insertBatch(logs.subList(i, Math.min(logs.size(), i + 500)));
        }
        return new PosViews.PointsCleared(logs.size(), total);
    }
}
