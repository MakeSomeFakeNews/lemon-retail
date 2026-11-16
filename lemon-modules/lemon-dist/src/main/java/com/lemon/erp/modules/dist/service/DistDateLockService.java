package com.lemon.erp.modules.dist.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.period.BizDateLockChecker;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.dist.dto.DateLockRequest;
import com.lemon.erp.modules.dist.entity.DistDateLock;
import com.lemon.erp.modules.dist.mapper.DistDateLockMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 日期锁定（§14 规则 7、FI-11）：每个租户一条全公司的锁定日期，门店可另设（两者都检查，取较晚的）。
 * 业务日期不晚于锁定日期的单据不能新增、修改、确认、作废：仓库出入库 / 调拨 / 盘点、供应链的出入库与退货（生成仓库单据时）、
 * 财务的收付款、应收应付、费用等（经 {@code BizPeriodGuard}，与会计期间结账一起检查）以及本模块的要货单、配送单。
 * 实现 framework 的 {@link BizDateLockChecker}，没有设置时不限制
 */
@Service
@RequiredArgsConstructor
public class DistDateLockService implements BizDateLockChecker {

    private static final long COMPANY = 0L;

    private final DistDateLockMapper lockMapper;

    @Override
    public void check(LocalDate bizDate, Long warehouseId, String docName) {
        if (bizDate == null) {
            return;
        }
        LocalDate lock = lockMapper.selectEffectiveDate(warehouseId);
        if (lock != null && !bizDate.isAfter(lock)) {
            throw new BizException("dist.error.dateLocked", bizDate, lock, docName == null ? "" : Messages.get(docName));
        }
    }

    /** 全部锁定设置（全公司为 storeId = 0） */
    public List<DistDateLock> list() {
        return lockMapper.selectAllWithNames();
    }

    /**
     * 设置或解除（lockDate 为空）锁定：门店的锁定须可管辖该门店；全公司的锁定只有全部门店范围的用户可以设置
     */
    public void set(DateLockRequest req) {
        long storeId = req.storeId() == null ? COMPANY : req.storeId();
        if (storeId == COMPANY) {
            if (!SecurityUtils.getDataScope().allStores()) {
                throw new BizException("dist.error.companyLockNeedsAllStores");
            }
        } else {
            DistSupport.checkStoreAccess(storeId, null);
        }
        DistDateLock old = lockMapper.selectOne(Wrappers.<DistDateLock>lambdaQuery().eq(DistDateLock::getStoreId, storeId));
        if (req.lockDate() == null) {
            if (old != null) {
                lockMapper.deleteById(old.getId());
            }
            return;
        }
        if (old == null) {
            DistDateLock l = new DistDateLock();
            l.setStoreId(storeId);
            l.setLockDate(req.lockDate());
            l.setRemark(req.remark());
            l.setUpdateBy(SecurityUtils.getUserId());
            lockMapper.insert(l);
        } else {
            lockMapper.update(Wrappers.<DistDateLock>lambdaUpdate()
                    .set(DistDateLock::getLockDate, req.lockDate())
                    .set(DistDateLock::getRemark, req.remark())
                    .set(DistDateLock::getUpdateBy, SecurityUtils.getUserId())
                    .set(DistDateLock::getUpdateTime, LocalDateTime.now())
                    .eq(DistDateLock::getId, old.getId()));
        }
    }

    /** 门店的锁定日期（含全公司），供页面提示 */
    public LocalDate effective(Long warehouseId) {
        return lockMapper.selectEffectiveDate(warehouseId);
    }
}
