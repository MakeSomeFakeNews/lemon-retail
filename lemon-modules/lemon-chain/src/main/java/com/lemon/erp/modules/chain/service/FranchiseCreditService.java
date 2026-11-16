package com.lemon.erp.modules.chain.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.rule.BizRules;
import com.lemon.erp.framework.store.FranchiseCreditChecker;
import com.lemon.erp.framework.store.StoreType;
import com.lemon.erp.modules.chain.entity.ChainFranchiseAccount;
import com.lemon.erp.modules.chain.model.FranchiseCreditControl;
import com.lemon.erp.modules.store.entity.StoreInfo;
import com.lemon.erp.modules.store.service.StoreInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 加盟店的货款额度控制（配送模块在要货单提交、配送单发货前经 {@link FranchiseCreditChecker} 调用）：
 * 预付货款的加盟店，本单金额不能超过预付结余（预收 − 未核销应收）；月结的加盟店设置了信用额度时，不能超过 额度 − 应收余额。
 * 业务参数 {@code chain.franchise.creditControl}：不控制 / 提示（只记录日志，加盟商账户页显示可用额度为负）/ 禁止（默认）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FranchiseCreditService implements FranchiseCreditChecker {

    private final StoreInfoService storeService;
    private final FranchiseAccountService accountService;
    private final BizRules bizRules;

    @Override
    public void check(Long storeId, BigDecimal amount, String docName) {
        StoreInfo store = storeService.get(storeId);
        if (store.getType() != StoreType.FRANCHISE || store.getSettleMode() == null) {
            return;
        }
        FranchiseCreditControl control = bizRules.option(ChainBizRules.CREDIT_CONTROL, FranchiseCreditControl.class);
        if (control == FranchiseCreditControl.NONE) {
            return;
        }
        ChainFranchiseAccount account = accountService.view(storeId);
        BigDecimal available = account.getAvailable();
        BigDecimal need = Decimals.nz(amount);
        if (available == null || need.compareTo(available) <= 0) {
            return;
        }
        if (control == FranchiseCreditControl.BLOCK) {
            throw new BizException("chain.error.creditInsufficient", store.getName(), Messages.get(docName),
                    need.toPlainString(), Decimals.money(available).toPlainString());
        }
        log.warn("加盟店 {} 的{}金额 {} 超过可用额度 {}", store.getName(), Messages.get(docName), need, available);
    }
}
