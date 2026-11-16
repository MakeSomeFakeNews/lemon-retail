package com.lemon.erp.framework.store;

import java.math.BigDecimal;

/**
 * 加盟店的货款额度检查，由连锁经营模块实现（{@code FranchiseCreditService}）：预付货款的加盟店按预付余额、月结的加盟店按信用额度，
 * 按业务参数 {@code chain.franchise.creditControl} 不控制 / 提示 / 禁止。配送模块在要货单提交、配送单发货前调用（发货前按将要生成的配送金额），
 * 不依赖连锁经营模块；没有实现时不检查
 */
public interface FranchiseCreditChecker {

    /**
     * 金额超过可用余额（禁止时）抛出 BizException；不是加盟店、没有设置结算方式时不检查
     *
     * @param storeId 收货门店
     * @param amount  本单金额（配送价 × 数量）
     * @param docName 单据名称（消息键），用于提示
     */
    void check(Long storeId, BigDecimal amount, String docName);
}
