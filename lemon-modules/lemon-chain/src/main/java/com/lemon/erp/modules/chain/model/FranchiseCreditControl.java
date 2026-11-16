package com.lemon.erp.modules.chain.model;

import com.lemon.erp.framework.setting.OptionEnum;

/**
 * 加盟店货款额度控制（业务参数 chain.franchise.creditControl），label 为消息键：
 * 要货单提交、配送单发货时金额超过预付余额（预付货款）或信用额度剩余（月结，设置了额度时）
 */
public enum FranchiseCreditControl implements OptionEnum {
    /** 不控制 */
    NONE("chain.label.creditControlNone"),
    /** 提示：允许继续（记录日志），加盟商账户显示余额不足 */
    WARN("chain.label.creditControlWarn"),
    /** 禁止：不能提交 / 发货 */
    BLOCK("chain.label.creditControlBlock");

    private final String label;

    FranchiseCreditControl(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
