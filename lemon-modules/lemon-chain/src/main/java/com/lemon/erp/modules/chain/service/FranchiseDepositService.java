package com.lemon.erp.modules.chain.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.store.StoreType;
import com.lemon.erp.modules.chain.dto.FranchiseRequests;
import com.lemon.erp.modules.chain.entity.ChainFranchiseDeposit;
import com.lemon.erp.modules.chain.mapper.ChainFranchiseDepositMapper;
import com.lemon.erp.modules.chain.model.FranchiseDepositKind;
import com.lemon.erp.modules.fin.service.FinAccountService;
import com.lemon.erp.modules.store.entity.StoreInfo;
import com.lemon.erp.modules.store.service.StoreInfoService;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 加盟保证金（§11.2 加盟信息）：收取、退还的登记与余额（收取 − 退还）。只做台账，不进入加盟应收（保证金是负债不是货款），
 * 资金的账务由出纳另行登记（如其他应付款）；退还不能超过余额
 */
@Service
@RequiredArgsConstructor
public class FranchiseDepositService {

    private final ChainFranchiseDepositMapper depositMapper;
    private final StoreInfoService storeService;
    private final FinAccountService accountService;
    private final DocNoService docNoService;

    public PageResult<ChainFranchiseDeposit> page(long current, long size, Long storeId, FranchiseDepositKind kind, LocalDate begin, LocalDate end) {
        Page<ChainFranchiseDeposit> page = Page.of(current, size);
        depositMapper.selectPageList(page, storeId, kind == null ? null : kind.name(), begin, end, ChainSupport.scopeStores());
        return PageResult.of(page);
    }

    public BigDecimal balance(Long storeId) {
        ChainSupport.checkStore(storeId);
        return Decimals.nz(depositMapper.selectBalance(storeId));
    }

    @Transactional
    public Long create(FranchiseRequests.Deposit req) {
        ChainSupport.checkStore(req.storeId());
        StoreInfo store = storeService.get(req.storeId());
        if (store.getType() != StoreType.FRANCHISE) {
            throw new BizException("chain.error.notFranchise", store.getName());
        }
        if (req.accountId() != null) {
            accountService.requireCashAccount(req.accountId());
        }
        if (req.kind() == FranchiseDepositKind.REFUND) {
            BigDecimal balance = Decimals.nz(depositMapper.selectBalance(req.storeId()));
            if (req.amount().compareTo(balance) > 0) {
                throw new BizException("chain.error.refundExceedsDeposit", balance.toPlainString());
            }
        }
        ChainFranchiseDeposit d = new ChainFranchiseDeposit();
        d.setDepositNo(docNoService.next(ChainDocNos.DEPOSIT));
        d.setStoreId(req.storeId());
        d.setKind(req.kind());
        d.setBizDate(req.bizDate());
        d.setAmount(req.amount());
        d.setAccountId(req.accountId());
        d.setRemark(req.remark());
        depositMapper.insert(d);
        return d.getId();
    }

    /** 删除登记（收取的删除后余额不能为负） */
    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            ChainFranchiseDeposit d = depositMapper.selectById(id);
            if (d == null) {
                continue;
            }
            ChainSupport.checkStore(d.getStoreId());
            depositMapper.deleteById(id);
            if (Decimals.nz(depositMapper.selectBalance(d.getStoreId())).signum() < 0) {
                throw new BizException("chain.error.depositBalanceNegative", d.getDepositNo());
            }
        }
    }
}
