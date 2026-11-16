package com.lemon.erp.modules.channel.dto;

/**
 * 一次同步的结果（同时写入同步日志）
 *
 * @param success 整体成功（接口调用中断时为 false）
 * @param message 第一条失败的原因
 */
public record WxSyncResult(int total, int succeeded, int failed, boolean success, String message) {
}
