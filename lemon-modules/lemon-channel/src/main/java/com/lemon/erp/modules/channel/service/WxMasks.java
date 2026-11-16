package com.lemon.erp.modules.channel.service;

/**
 * 买家与收货信息脱敏
 */
final class WxMasks {

    private WxMasks() {
    }

    /** 买家标识：保留前 3 位和后 2 位 */
    static String buyer(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.length() <= 5 ? s.charAt(0) + "***" : s.substring(0, 3) + "***" + s.substring(s.length() - 2);
    }

    /** 姓名：保留第一个字 */
    static String name(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.codePointCount(0, s.length()) <= 1 ? "*" : new String(Character.toChars(s.codePointAt(0))) + "**";
    }

    /** 电话：保留前 3 位和后 4 位 */
    static String phone(String s) {
        if (s == null || s.length() < 7) {
            return s == null ? null : "****";
        }
        return s.substring(0, 3) + "****" + s.substring(s.length() - 4);
    }

    /** 地址：保留前 6 个字 */
    static String address(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.length() <= 6 ? s.charAt(0) + "****" : s.substring(0, 6) + "****";
    }
}
