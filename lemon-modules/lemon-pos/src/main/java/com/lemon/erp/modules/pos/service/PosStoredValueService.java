package com.lemon.erp.modules.pos.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataPermissionContext;
import com.lemon.erp.framework.security.datascope.DataPermissionRule;
import com.lemon.erp.framework.store.StoreLookup;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.dto.PosRequests;
import com.lemon.erp.modules.pos.dto.PosViews;
import com.lemon.erp.modules.pos.entity.PosMember;
import com.lemon.erp.modules.pos.entity.PosStoredValueLog;
import com.lemon.erp.modules.pos.mapper.PosMemberMapper;
import com.lemon.erp.modules.pos.mapper.PosStoredValueLogMapper;
import com.lemon.erp.modules.pos.model.StoredValueType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 会员储值：余额全局一份（全部门店通用），每笔变动记录门店（充值为收款门店，消费 / 退回为消费门店），用于跨店结算。
 * 所有变动经 {@link #change} 更新余额并写储值流水（同一事务）；减少时条件更新（余额不足不更新），并发重复扣款时只有余额足够的成功，余额不会为负
 */
@Service
@RequiredArgsConstructor
public class PosStoredValueService {

    static final String STORED_VALUE_PERM = "pos:member:storedValue";
    private static final int MAX_DAYS = 366;
    private static final DataPermissionRule RULE = new DataPermissionRule("pos_stored_value_log", null, null, "store_id", null);

    private final PosMemberMapper memberMapper;
    private final PosStoredValueLogMapper logMapper;
    private final PosMemberService memberService;
    private final StoreLookup storeLookup;

    /** 储值流水，按用户的门店范围过滤 */
    @DataPermission(table = "pos_stored_value_log", deptColumn = "", userColumn = "", storeColumn = "store_id")
    public PageResult<PosStoredValueLog> page(long current, long size, PosQueries.StoredValueLog q) {
        Page<PosStoredValueLog> page = Page.of(current, size);
        logMapper.selectPageList(page, q);
        return PageResult.of(page);
    }

    /** 会员的储值流水（会员详情，全部门店） */
    public PageResult<PosStoredValueLog> pageOfMember(long current, long size, Long memberId) {
        Page<PosStoredValueLog> page = Page.of(current, size);
        logMapper.selectPageList(page, new PosQueries.StoredValueLog(null, memberId, null, null, null, null));
        return PageResult.of(page);
    }

    /**
     * 余额变动：amount 增加为正、减少为负；减少时余额不足报错
     *
     * @return 变动后余额
     */
    public BigDecimal change(Long memberId, Long storeId, StoredValueType type, BigDecimal amount, LocalDate date, Long saleId, Long topupId,
                             String sourceNo, String remark) {
        BigDecimal x = Decimals.money(amount);
        if (x.signum() < 0) {
            if (memberMapper.deductBalance(memberId, x.negate()) == 0) {
                PosMember m = memberMapper.selectAccount(memberId);
                throw new BizException("pos.error.balanceInsufficient", m == null ? "" : m.getBalance().toPlainString(), x.negate().toPlainString());
            }
        } else if (x.signum() > 0) {
            memberMapper.addBalance(memberId, x);
        }
        BigDecimal after = memberMapper.selectAccount(memberId).getBalance();
        PosStoredValueLog l = new PosStoredValueLog();
        l.setMemberId(memberId);
        l.setStoreId(storeId);
        l.setChangeType(type);
        l.setAmount(x);
        l.setBalanceAfter(after);
        l.setBusinessDate(date);
        l.setSaleId(saleId);
        l.setTopupId(topupId);
        l.setSourceNo(sourceNo);
        l.setRemark(remark);
        l.setCreateBy(SecurityUtils.getUserIdOrNull());
        logMapper.insert(l);
        return after;
    }

    /** 手工调整（pos:member:storedValue 由控制器校验）：门店为空时记总部；减少不能超过余额 */
    @Transactional
    public BigDecimal adjust(PosRequests.StoredValueAdjust req) {
        memberService.get(req.memberId());
        if (req.amount().signum() == 0) {
            throw new BizException("pos.error.amountZero");
        }
        Long storeId = req.storeId() != null ? storeLookup.require(req.storeId()).id() : storeLookup.headquarters().id();
        PosTerminalService.checkStore(storeId);
        return change(req.memberId(), storeId, StoredValueType.ADJUST, req.amount(), LocalDate.now(), null, null, null, req.reason());
    }

    /**
     * 储值跨店结算（营业日期范围，按门店）：结算金额 = 本店储值消费净额（消费 − 退回）− 本店充值实收，
     * 正数为总部应付门店（门店提供了商品、钱在别的门店或总部），负数为门店应缴总部（门店收了充值款、会员还没在本店消费完）。
     * 赠送、调整单列不参与结算。按用户的门店范围过滤
     */
    public PosViews.StoredValueSettle settle(PosQueries.StoredValueSettle query) {
        LocalDate end = query.end() == null ? LocalDate.now() : query.end();
        LocalDate begin = query.begin() == null ? end.withDayOfMonth(1) : query.begin();
        if (begin.isAfter(end) || ChronoUnit.DAYS.between(begin, end) >= MAX_DAYS) {
            throw new BizException("pos.error.reportRangeInvalid", MAX_DAYS);
        }
        PosQueries.StoredValueSettle q = new PosQueries.StoredValueSettle(query.storeId(), begin, end);
        List<PosViews.StoredValueSettleRow> rows = DataPermissionContext.execute(RULE, () -> logMapper.selectSettle(q));
        PosViews.StoredValueSettleRow total = new PosViews.StoredValueSettleRow();
        total.setTopupAmount(BigDecimal.ZERO);
        total.setTopupCount(0);
        total.setBonusAmount(BigDecimal.ZERO);
        total.setConsumeAmount(BigDecimal.ZERO);
        total.setRefundAmount(BigDecimal.ZERO);
        total.setAdjustAmount(BigDecimal.ZERO);
        for (PosViews.StoredValueSettleRow r : rows) {
            r.setNetConsume(r.getConsumeAmount().subtract(r.getRefundAmount()));
            r.setSettleAmount(r.getNetConsume().subtract(r.getTopupAmount()));
            total.setTopupAmount(total.getTopupAmount().add(r.getTopupAmount()));
            total.setTopupCount(total.getTopupCount() + r.getTopupCount());
            total.setBonusAmount(total.getBonusAmount().add(r.getBonusAmount()));
            total.setConsumeAmount(total.getConsumeAmount().add(r.getConsumeAmount()));
            total.setRefundAmount(total.getRefundAmount().add(r.getRefundAmount()));
            total.setAdjustAmount(total.getAdjustAmount().add(r.getAdjustAmount()));
        }
        total.setNetConsume(total.getConsumeAmount().subtract(total.getRefundAmount()));
        total.setSettleAmount(total.getNetConsume().subtract(total.getTopupAmount()));
        return new PosViews.StoredValueSettle(rows, total);
    }
}
