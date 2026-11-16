package com.lemon.erp.modules.chain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.store.StoreType;
import com.lemon.erp.modules.chain.dto.FranchiseRequests;
import com.lemon.erp.modules.chain.entity.ChainFranchiseAccount;
import com.lemon.erp.modules.chain.mapper.ChainFranchiseAccountMapper;
import com.lemon.erp.modules.fin.dto.AdvanceRequest;
import com.lemon.erp.modules.fin.dto.SettlementItemRequest;
import com.lemon.erp.modules.fin.entity.FinAdvance;
import com.lemon.erp.modules.fin.entity.FinBill;
import com.lemon.erp.modules.fin.model.BillKind;
import com.lemon.erp.modules.fin.model.FinConstants;
import com.lemon.erp.modules.fin.model.SettleKind;
import com.lemon.erp.modules.fin.service.FinAdvanceService;
import com.lemon.erp.modules.fin.service.FinBillService;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.scm.service.ScmCustomerService;
import com.lemon.erp.modules.store.entity.StoreInfo;
import com.lemon.erp.modules.store.model.FranchiseSettleMode;
import com.lemon.erp.modules.store.service.StoreInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 加盟商账户（FI-07）：每个加盟店一个账户，关联一个供应链客户（首次使用时自动创建，链接保存在 chain_franchise_account，不修改门店资料），
 * 加盟应收、预收款、收款核销、往来对账都复用财务模块：
 * <ul>
 *   <li>余额 = 未核销的应收 − 未冲抵的预收（财务 {@code PartnerBalanceProvider}，正数为欠款、负数为预付结余）；</li>
 *   <li>可用额度：预付货款 = 预付结余（−余额）；月结 = 信用额度 − 余额（没有设置额度时不限）；</li>
 *   <li>充值（预付款）：登记一张预收款（财务预收，借 银行 / 贷 预收账款），并自动冲抵未核销的加盟应收（先到期先冲）；</li>
 *   <li>预付货款的加盟店在生成加盟应收时也自动用预收款冲抵（见 {@link FranchiseSettlementService}）。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class FranchiseAccountService {

    private final ChainFranchiseAccountMapper accountMapper;
    private final StoreInfoService storeService;
    private final ScmCustomerService customerService;
    private final FinBillService billService;
    private final FinAdvanceService advanceService;

    public PageResult<ChainFranchiseAccount> page(long current, long size, String keyword, Long storeId, FranchiseSettleMode settleMode) {
        Page<ChainFranchiseAccount> page = Page.of(current, size);
        accountMapper.selectPageList(page, keyword, storeId, settleMode == null ? null : settleMode.name(), ChainSupport.scopeStores());
        page.getRecords().forEach(FranchiseAccountService::derive);
        return PageResult.of(page);
    }

    /** 加盟店的账户与余额（门店须可管辖） */
    public ChainFranchiseAccount detail(Long storeId) {
        ChainSupport.checkStore(storeId);
        return view(storeId);
    }

    /** 账户与余额，不检查门店范围（额度检查使用） */
    ChainFranchiseAccount view(Long storeId) {
        ChainFranchiseAccount a = accountMapper.selectByStore(storeId);
        if (a == null) {
            throw new BizException("framework.error.storeNotFound");
        }
        derive(a);
        return a;
    }

    /** 余额、可用额度 */
    private static void derive(ChainFranchiseAccount a) {
        BigDecimal balance = Decimals.nz(a.getReceivable()).subtract(Decimals.nz(a.getPrepaid()));
        a.setBalance(balance);
        if (a.getSettleMode() == FranchiseSettleMode.PREPAID) {
            a.setAvailable(balance.negate());
        } else if (a.getCreditLimit() != null) {
            a.setAvailable(a.getCreditLimit().subtract(balance));
        }
    }

    /**
     * 加盟店的账户，没有时创建：按加盟商名称（没有时为门店名称）新建一个客户。只能为加盟店创建
     */
    @Transactional
    public ChainFranchiseAccount ensure(Long storeId) {
        ChainFranchiseAccount a = accountMapper.selectOne(Wrappers.<ChainFranchiseAccount>lambdaQuery().eq(ChainFranchiseAccount::getStoreId, storeId));
        if (a != null) {
            return a;
        }
        StoreInfo store = storeService.get(storeId);
        if (store.getType() != StoreType.FRANCHISE) {
            throw new BizException("chain.error.notFranchise", store.getName());
        }
        ScmCustomer c = new ScmCustomer();
        c.setName(StringUtils.hasText(store.getFranchisee()) ? store.getFranchisee() : store.getName());
        c.setContact(store.getContact());
        c.setPhone(store.getPhone());
        c.setAddress(store.getAddress());
        c.setRemark("加盟店「" + store.getName() + "」的加盟商（连锁经营自动创建）"); // i18n-ignore 存入客户资料
        customerService.create(c);
        a = new ChainFranchiseAccount();
        a.setStoreId(storeId);
        a.setCustomerId(c.getId());
        accountMapper.insert(a);
        return a;
    }

    /** 修改信用额度（月结）、备注 */
    @Transactional
    public void update(Long storeId, FranchiseRequests.Account req) {
        ChainSupport.checkStore(storeId);
        ChainFranchiseAccount a = ensure(storeId);
        a.setCreditLimit(req.creditLimit());
        a.setRemark(req.remark());
        accountMapper.updateById(a);
    }

    /**
     * 充值（预付货款）：登记预收款，再用预收款自动冲抵未核销的加盟应收
     *
     * @return 预收款单 ID
     */
    @Transactional
    public Long topup(Long storeId, FranchiseRequests.Topup req) {
        ChainSupport.checkStore(storeId);
        ChainFranchiseAccount a = ensure(storeId);
        LocalDate date = req.date() == null ? LocalDate.now() : req.date();
        StoreInfo store = storeService.get(storeId);
        String remark = StringUtils.hasText(req.remark()) ? req.remark() : "加盟店「" + store.getName() + "」预付货款"; // i18n-ignore 存入单据
        Long advanceId = advanceService.create(new AdvanceRequest(SettleKind.RECEIPT, a.getCustomerId(), date, req.accountId(), req.method(),
                req.amount(), remark));
        autoOffset(a.getCustomerId(), date);
        return advanceId;
    }

    /**
     * 用未冲抵的预收款冲抵未核销的加盟应收（蓝字），预收款按日期从早到晚、应收单按日期 / 到期日从早到晚依次分配；
     * 每张预收款冲抵一次（财务生成转账凭证 借 预收账款 / 贷 应收账款）。只处理本位币
     */
    @Transactional
    public void autoOffset(Long customerId, LocalDate date) {
        List<FinBill> bills = new ArrayList<>(billService.unsettled(BillKind.AR, customerId).stream()
                .filter(b -> b.getAmount().signum() > 0 && isBase(b.getCurrency())).toList());
        List<BigDecimal> remaining = new ArrayList<>(bills.stream().map(b -> b.getAmount().subtract(Decimals.nz(b.getSettledAmount()))).toList());
        for (FinAdvance adv : advanceService.open(SettleKind.RECEIPT, customerId)) {
            if (!isBase(adv.getCurrency())) {
                continue;
            }
            BigDecimal left = Decimals.nz(adv.getAmount()).subtract(Decimals.nz(adv.getUsedAmount()));
            List<SettlementItemRequest> items = new ArrayList<>();
            for (int i = 0; i < bills.size() && left.signum() > 0; i++) {
                BigDecimal r = remaining.get(i);
                if (r.signum() <= 0) {
                    continue;
                }
                BigDecimal use = r.min(left);
                items.add(new SettlementItemRequest(bills.get(i).getId(), use));
                remaining.set(i, r.subtract(use));
                left = left.subtract(use);
            }
            if (!items.isEmpty()) {
                advanceService.apply(adv.getId(), date, items);
            }
            if (remaining.stream().noneMatch(r -> r.signum() > 0)) {
                return;
            }
        }
    }

    private static boolean isBase(String currency) {
        return currency == null || FinConstants.BASE_CURRENCY.equals(currency);
    }
}
