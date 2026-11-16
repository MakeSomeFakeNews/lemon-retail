package com.lemon.erp.modules.channel.client;

import com.lemon.erp.modules.channel.model.WxShopMode;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 按店铺的对接方式选择接口实现：MOCK → 模拟店铺，LIVE → 微信小店接口
 */
@Component
public class WxShopClients {

    private final Map<WxShopMode, WxShopClient> clients = new EnumMap<>(WxShopMode.class);

    public WxShopClients(List<WxShopClient> list) {
        list.forEach(c -> clients.put(c.mode(), c));
    }

    public WxShopClient of(WxShopMode mode) {
        WxShopClient client = clients.get(mode);
        if (client == null) {
            throw new IllegalStateException("没有对接方式 " + mode + " 的实现"); // i18n-ignore 内部错误
        }
        return client;
    }

    /** 店铺配置修改后清理全部实现中的凭证缓存 */
    public void evictToken(Long shopId) {
        clients.values().forEach(c -> c.evictToken(shopId));
    }
}
