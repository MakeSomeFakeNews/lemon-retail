package com.lemon.erp.modules.channel.service;

import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.datascope.DataScope;
import com.lemon.erp.framework.tenant.TenantContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 定时任务、消息推送等没有登录用户的线程中处理渠道订单：以系统身份执行（全部数据、制单人 / 审核人为空），
 * 生成销售订单、出库、退货的 Service 依赖当前用户。已有登录用户（手工操作）时保持当前用户
 */
final class WxSystemIdentity {

    private WxSystemIdentity() {
    }

    static <T> T call(Supplier<T> action) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser) {
            return action.get();
        }
        SecurityContext saved = SecurityContextHolder.getContext();
        LoginUser system = new LoginUser(null, "system", null, Set.of(), Set.of(PermissionService.ALL_PERMISSION), DataScope.ALL,
                null, TenantContext.currentOrPlatform());
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(system, null, List.of()));
        SecurityContextHolder.setContext(ctx);
        try {
            return action.get();
        } finally {
            SecurityContextHolder.setContext(saved);
        }
    }

    static void run(Runnable action) {
        call(() -> {
            action.run();
            return null;
        });
    }
}
