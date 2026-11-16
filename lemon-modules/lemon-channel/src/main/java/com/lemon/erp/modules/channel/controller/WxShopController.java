package com.lemon.erp.modules.channel.controller;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.channel.client.WxShopClient;
import com.lemon.erp.modules.channel.dto.WxShopRequests;
import com.lemon.erp.modules.channel.dto.WxSyncResult;
import com.lemon.erp.modules.channel.entity.WxShop;
import com.lemon.erp.modules.channel.model.WxSyncType;
import com.lemon.erp.modules.channel.service.WxShopService;
import com.lemon.erp.modules.channel.service.WxShopSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/channel/wx-shop")
@RequiredArgsConstructor
public class WxShopController {

    private final WxShopService shopService;
    private final WxShopSyncService syncService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('channel:wxShop:list')")
    public R<PageResult<WxShop>> page(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) EnableStatus status,
                                      @RequestParam(required = false) Long storeId) {
        return R.ok(shopService.page(current, size, keyword, status, storeId));
    }

    /** 店铺下拉（商品映射、订单、售后、日志页面的筛选），有任一渠道页面权限即可 */
    @GetMapping("/options")
    @PreAuthorize("@ss.hasAnyPerm('channel:wxShop:list', 'channel:wxProduct:list', 'channel:wxOrder:list', 'channel:wxAftersale:list', 'channel:wxSyncLog:list')")
    public R<List<WxShop>> options() {
        return R.ok(shopService.options());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('channel:wxShop:list')")
    public R<WxShop> get(@PathVariable Long id) {
        return R.ok(shopService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('channel:wxShop:add')")
    @OperLog(title = "微信小店-店铺设置", type = OperType.INSERT) // i18n-ignore
    public R<Long> create(@Validated @RequestBody WxShop shop) {
        return R.ok(shopService.create(shop));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('channel:wxShop:edit')")
    @OperLog(title = "微信小店-店铺设置", type = OperType.UPDATE) // i18n-ignore
    public R<Void> update(@PathVariable Long id, @Validated @RequestBody WxShop shop) {
        shopService.update(id, shop);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('channel:wxShop:remove')")
    @OperLog(title = "微信小店-店铺设置", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable List<Long> ids) {
        shopService.delete(ids);
        return R.ok();
    }

    /** 测试连接：获取接口调用凭证并读取商品列表，返回说明文字 */
    @PostMapping("/{id}/test")
    @PreAuthorize("@ss.hasPerm('channel:wxShop:edit')")
    public R<String> test(@PathVariable Long id) {
        return R.ok(syncService.testText(id));
    }

    /** 立即同步：PRODUCT 商品（并自动匹配）/ STOCK 库存（全部推送）/ ORDER 订单 / AFTERSALE 售后单 */
    @PostMapping("/{id}/sync")
    @PreAuthorize("@ss.hasPerm('channel:wxShop:sync')")
    @OperLog(title = "微信小店-立即同步", type = OperType.OTHER) // i18n-ignore
    public R<WxSyncResult> sync(@PathVariable Long id, @RequestParam WxSyncType type) {
        return R.ok(syncService.syncNow(id, type));
    }

    /** 快递公司列表（发货时选择） */
    @GetMapping("/{id}/delivery-companies")
    @PreAuthorize("@ss.hasPerm('channel:wxOrder:ship')")
    public R<List<WxShopClient.DeliveryCompany>> deliveryCompanies(@PathVariable Long id) {
        return R.ok(syncService.deliveryCompanies(id));
    }

    /** 模拟店铺：用物料生成模拟商品（只有模拟店铺可用） */
    @PostMapping("/{id}/mock/products")
    @PreAuthorize("@ss.hasPerm('channel:wxShop:edit')")
    @OperLog(title = "微信小店-生成模拟商品", type = OperType.OTHER) // i18n-ignore
    public R<Integer> mockProducts(@PathVariable Long id) {
        return R.ok(syncService.mockProducts(id));
    }

    /** 模拟店铺：生成已付款的模拟订单并同步（只有模拟店铺可用） */
    @PostMapping("/{id}/mock/orders")
    @PreAuthorize("@ss.hasPerm('channel:wxShop:edit')")
    @OperLog(title = "微信小店-生成模拟订单", type = OperType.OTHER) // i18n-ignore
    public R<Integer> mockOrders(@PathVariable Long id, @Valid @RequestBody WxShopRequests.MockOrders req) {
        return R.ok(syncService.mockOrders(id, req.count() == null ? 1 : req.count()));
    }
}
