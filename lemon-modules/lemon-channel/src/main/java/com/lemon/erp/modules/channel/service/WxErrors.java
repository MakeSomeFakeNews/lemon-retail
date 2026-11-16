package com.lemon.erp.modules.channel.service;

import com.lemon.erp.common.exception.BizException;

/**
 * 记录到订单、售后单、同步日志中的错误说明（默认语言，存库）
 */
final class WxErrors {

    private static final int MAX = 500;

    private WxErrors() {
    }

    static String text(Throwable e) {
        String s = e instanceof BizException ? e.getMessage() : e.getClass().getSimpleName() + ": " + e.getMessage();
        return s == null || s.length() <= MAX ? s : s.substring(0, MAX);
    }
}
