package com.lemon.erp.modules.channel.service;

import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.modules.channel.client.WxCallbackCrypto;
import com.lemon.erp.modules.channel.entity.WxShop;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 微信小店消息推送（免登录）：按地址中的店铺 ID 跨租户找到店铺（停用的不处理），用店铺的推送 Token 校验签名，
 * 安全模式下用 EncodingAESKey 解密（校验 msg_signature 和 AppID）；校验通过后切换到店铺所属租户，异步拉取事件涉及的订单 / 售后单，
 * 请求中只做校验，立即返回
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxCallbackService {

    private final WxShopService shopService;
    private final WxShopSyncService syncService;

    /**
     * 推送地址校验（小店后台保存推送配置时发起）
     *
     * @return 校验通过时返回 echostr（安全模式下为解密后的明文），否则返回 null
     */
    public String verify(Long shopId, String signature, String timestamp, String nonce, String echostr, String msgSignature) {
        WxShop shop = shopService.findForCallback(shopId);
        if (shop == null || !StringUtils.hasText(shop.getPushToken()) || echostr == null) {
            return null;
        }
        String token = shop.getPushToken();
        if (StringUtils.hasText(msgSignature) && StringUtils.hasText(shop.getAesKey())) {
            if (!WxCallbackCrypto.verify(msgSignature, token, timestamp, nonce, echostr)) {
                return null;
            }
            try {
                return WxCallbackCrypto.decrypt(shop.getAesKey(), echostr, shop.getAppId());
            } catch (IllegalArgumentException e) {
                log.warn("微信小店 {} 推送地址校验解密失败", shopId, e);
                return null;
            }
        }
        return WxCallbackCrypto.verify(signature, token, timestamp, nonce) ? echostr : null;
    }

    /**
     * 接收事件
     *
     * @return 签名、解密校验通过返回 true（已提交异步处理）
     */
    public boolean receive(Long shopId, String signature, String timestamp, String nonce, String msgSignature, String body) {
        WxShop shop = shopService.findForCallback(shopId);
        if (shop == null || !StringUtils.hasText(shop.getPushToken())) {
            return false;
        }
        String token = shop.getPushToken();
        if (!WxCallbackCrypto.verify(signature, token, timestamp, nonce)) {
            return false;
        }
        WxCallbackCrypto.Message message;
        try {
            message = WxCallbackCrypto.parse(body);
            if (message.encrypt() != null) {
                if (!StringUtils.hasText(shop.getAesKey())
                        || StringUtils.hasText(msgSignature) && !WxCallbackCrypto.verify(msgSignature, token, timestamp, nonce, message.encrypt())) {
                    return false;
                }
                message = WxCallbackCrypto.parse(WxCallbackCrypto.decrypt(shop.getAesKey(), message.encrypt(), shop.getAppId()));
            }
        } catch (IllegalArgumentException e) {
            log.warn("微信小店 {} 推送消息解析失败", shopId, e);
            return false;
        }
        WxCallbackCrypto.Message event = message;
        // 异步线程按提交时的租户执行（TenantTaskDecorator）
        TenantContext.runAs(WxShopService.tenantOf(shop), () -> syncService.handleEvent(shop.getId(), event));
        return true;
    }
}
