package com.lemon.erp.modules.chain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.store.StoreType;
import com.lemon.erp.modules.chain.dto.ChainViews;
import com.lemon.erp.modules.chain.entity.ChainFranchiseAccount;
import com.lemon.erp.modules.chain.entity.ChainFranchiseLedger;
import com.lemon.erp.modules.chain.mapper.ChainFranchiseLedgerMapper;
import com.lemon.erp.modules.chain.model.FranchiseLedgerKind;
import com.lemon.erp.modules.fin.entity.FinBill;
import com.lemon.erp.modules.fin.model.BillKind;
import com.lemon.erp.modules.fin.model.BillSource;
import com.lemon.erp.modules.fin.service.FinBillService;
import com.lemon.erp.modules.scm.entity.ScmPriceSchemeItem;
import com.lemon.erp.modules.scm.service.ScmPriceSchemeService;
import com.lemon.erp.modules.store.entity.StoreInfo;
import com.lemon.erp.modules.store.model.FranchiseSettleMode;
import com.lemon.erp.modules.store.service.StoreInfoService;
import com.lemon.erp.modules.wms.event.TransferSettledEvent;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 加盟结算的应收（FI-07）：监听仓库的调拨确认 / 冲销（{@link TransferSettledEvent}，与调拨同一事务），
 * 调拨的一方是加盟店、另一方不是加盟店时，按金额生成加盟商的应收单（财务 fin_bill，客户为加盟商账户的客户）：
 * <ul>
 *   <li><b>时点：发货</b>——配送单发货生成在途调拨（调出确认）时即形成应收（货物离开配送中心、归加盟商承担，与门店往来同一时点）；
 *       收货少收 / 破损生成的反向差异调拨（加盟店 → 配送中心，同样按配送价）冲回为红字应收，差异处理不再影响应收；</li>
 *   <li>配送给加盟店（差价调拨）：金额 = 调拨金额（配送价 × 数量），来源 FRANCHISE_DELIVERY；</li>
 *   <li>加盟店退回（门店退仓等同价调拨，加盟店调出）：金额 = −Σ 数量 × 配送价方案价（没有方案价时按调拨金额，即调出成本），来源 FRANCHISE_RETURN；
 *       退仓收货少收生成的反向调拨（配送中心 → 加盟店）按同样价格冲回；</li>
 *   <li>冲销：按原调拨生成的应收金额记一张相反的单据。</li>
 * </ul>
 * 预付货款的加盟店生成蓝字应收后自动用预收款冲抵。加盟店之间、非加盟门店之间的调拨不处理
 */
@Service
@RequiredArgsConstructor
public class FranchiseSettlementService {

    private final ChainFranchiseLedgerMapper ledgerMapper;
    private final FranchiseAccountService accountService;
    private final StoreInfoService storeService;
    private final WmsWarehouseService warehouseService;
    private final ScmPriceSchemeService schemeService;
    private final FinBillService billService;

    @EventListener
    public void onTransferSettled(TransferSettledEvent e) {
        if (e.toWarehouseId() == null) {
            return;
        }
        Map<Long, Long> stores = warehouseService.storeIds(List.of(e.warehouseId(), e.toWarehouseId()));
        Long fromId = stores.get(e.warehouseId());
        Long toId = stores.get(e.toWarehouseId());
        if (fromId == null || toId == null || Objects.equals(fromId, toId)) {
            return;
        }
        StoreInfo from = storeService.get(fromId);
        StoreInfo to = storeService.get(toId);
        boolean toFranchise = to.getType() == StoreType.FRANCHISE;
        boolean fromFranchise = from.getType() == StoreType.FRANCHISE;
        if (toFranchise == fromFranchise) {
            return;
        }
        StoreInfo franchise = toFranchise ? to : from;
        BigDecimal amount;
        if (e.reversedId() != null) {
            // 冲销：原调拨生成过应收时记相反金额
            BigDecimal original = ledgerMapper.selectList(Wrappers.<ChainFranchiseLedger>lambdaQuery()
                    .eq(ChainFranchiseLedger::getTransferId, e.reversedId())).stream()
                    .map(ChainFranchiseLedger::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            amount = original.negate();
        } else if (e.differential()) {
            amount = Decimals.nz(e.transferAmount());
            amount = toFranchise ? amount : amount.negate();
        } else {
            amount = schemeAmount(franchise, e.docId());
            amount = toFranchise ? amount : amount.negate();
        }
        amount = Decimals.money(amount);
        if (amount.signum() == 0) {
            return;
        }
        ChainFranchiseAccount account = accountService.ensure(franchise.getId());
        String sourceNo = e.sourceNo() != null ? e.sourceNo() : e.docNo();
        FranchiseLedgerKind kind = amount.signum() > 0 ? FranchiseLedgerKind.DELIVERY : FranchiseLedgerKind.RETURN;
        String remark = (kind == FranchiseLedgerKind.DELIVERY ? "加盟配送 " : "加盟退货 ") + sourceNo + " 调拨单 " + e.docNo(); // i18n-ignore 存入单据
        FinBill bill = billService.createGenerated(BillKind.AR, account.getCustomerId(),
                kind == FranchiseLedgerKind.DELIVERY ? BillSource.FRANCHISE_DELIVERY : BillSource.FRANCHISE_RETURN,
                e.docId(), sourceNo, e.bizDate(), amount, remark);
        ChainFranchiseLedger l = new ChainFranchiseLedger();
        l.setStoreId(franchise.getId());
        l.setKind(kind);
        l.setTransferId(e.docId());
        l.setTransferNo(e.docNo());
        l.setSourceNo(e.sourceNo());
        l.setBizDate(e.bizDate());
        l.setAmount(amount);
        l.setBillId(bill.getId());
        l.setBillNo(bill.getBillNo());
        ledgerMapper.insert(l);
        if (amount.signum() > 0 && franchise.getSettleMode() == FranchiseSettleMode.PREPAID) {
            accountService.autoOffset(account.getCustomerId(), e.bizDate() == null ? LocalDate.now() : e.bizDate());
        }
    }

    /** 同价调拨（退仓等）按加盟店配送价方案的价格计算金额，没有方案价的物料取调拨金额（调出成本） */
    private BigDecimal schemeAmount(StoreInfo franchise, Long transferId) {
        List<ChainViews.TransferLine> lines = ledgerMapper.selectTransferLines(transferId);
        Map<Long, ScmPriceSchemeItem> prices = franchise.getDeliverySchemeId() == null ? Map.of()
                : schemeService.enabledPrices(franchise.getDeliverySchemeId(), lines.stream().map(ChainViews.TransferLine::getMaterialId).distinct().toList());
        BigDecimal total = BigDecimal.ZERO;
        for (ChainViews.TransferLine l : lines) {
            ScmPriceSchemeItem p = prices.get(l.getMaterialId());
            BigDecimal amount = p != null && p.getRetailPrice() != null
                    ? Decimals.money(Decimals.nz(l.getQty()).multiply(p.getRetailPrice())) : Decimals.nz(l.getTransferAmount());
            total = total.add(amount);
        }
        return total;
    }

    /** 加盟往来明细（配送、退回生成的应收），按门店范围 */
    public PageResult<ChainFranchiseLedger> page(long current, long size, Long storeId, LocalDate begin, LocalDate end, String keyword) {
        Page<ChainFranchiseLedger> page = Page.of(current, size);
        ledgerMapper.selectPageList(page, storeId, begin, end, keyword, ChainSupport.scopeStores());
        return PageResult.of(page);
    }
}
