package com.lemon.erp.modules.channel;

import com.jayway.jsonpath.JsonPath;
import com.lemon.erp.modules.channel.client.MockWxShopClient;
import com.lemon.erp.modules.channel.client.WxCallbackCrypto;
import com.lemon.erp.modules.channel.client.WxShopClient;
import com.lemon.erp.support.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * 线上渠道（微信小店，模拟店铺）：商品同步与自动匹配、库存推送、订单生成销售订单、待映射重试、发货（出库 + 回传）、
 * 小店后台发货、取消、售后（发货前仅退款、退货退款生成销售退货）、消息推送签名校验与异步处理、按门店的数据权限与收货信息脱敏。
 * 每个测试新建自己的门店、店铺、物料，按单号查找自己的数据
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WxShopTests extends ApiTestSupport {

    private static final String PUSH_TOKEN = "lemonPushToken";
    private static final String AES_KEY = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";

    @Autowired
    private MockWxShopClient mock;

    @BeforeEach
    void setUp() throws Exception {
        loginAdmin();
    }

    /** 门店 + 店铺 + 一个已映射的商品（售价 50）+ 发货仓库中的库存 */
    private record Ctx(long storeId, long warehouseId, long shopId, long materialId, String productId, String skuId) {
    }

    private Ctx prepare(Object stock, Map<String, Object> shopExtra) throws Exception {
        long store = ((Number) ok(send(post("/store/store"), admin, Map.of("name", "小店门店" + uid(), "type", "DIRECT", "createWarehouse", true)))).longValue();
        long warehouse = ((Number) read("/store/store/" + store, "$.data.defaultWarehouseId")).longValue();
        Map<String, Object> body = new HashMap<>(Map.of("name", "小店" + uid(), "mode", "MOCK", "storeId", store, "warehouseId", warehouse,
                "pushToken", PUSH_TOKEN, "safetyStock", 2));
        body.putAll(shopExtra);
        long shop = ((Number) ok(send(post("/channel/wx-shop"), admin, body))).longValue();
        String code = "WX" + uid();
        long material = newMaterial(code);
        WxShopClient.Product p = mock.addProduct(shop, "柠檬礼盒" + code,
                List.of(new MockWxShopClient.SkuSpec(code, null, "规格:大", new BigDecimal("50"))));
        sync(shop, "PRODUCT");
        if (stock != null) {
            stockIn(warehouse, material, null, stock, null);
        }
        return new Ctx(store, warehouse, shop, material, p.productId(), p.skus().get(0).skuId());
    }

    private long newMaterial(String code) throws Exception {
        ok(send(post("/wms/material"), admin, Map.of("code", code, "name", "小店物料" + code, "unit", "个", "category", "FINISHED",
                "batchEnabled", 0, "serialEnabled", 0)));
        return ((Number) read("/wms/material?keyword=" + code, "$.data.records[0].id")).longValue();
    }

    private Map<String, Object> sync(long shop, String type) throws Exception {
        return ok(send(post("/channel/wx-shop/" + shop + "/sync?type=" + type), admin, Map.of()));
    }

    private Map<String, Object> order(long shop, String orderId) throws Exception {
        List<Map<String, Object>> rows = read("/channel/wx-order?size=50&shopId=" + shop + "&keyword=" + orderId, "$.data.records");
        assertThat(rows).as(orderId).hasSize(1);
        return read("/channel/wx-order/" + rows.get(0).get("id"), "$.data");
    }

    private Map<String, Object> aftersale(long shop, String aftersaleId) throws Exception {
        List<Map<String, Object>> rows = read("/channel/wx-aftersale?size=50&shopId=" + shop + "&keyword=" + aftersaleId, "$.data.records");
        assertThat(rows).as(aftersaleId).hasSize(1);
        return rows.get(0);
    }

    private static long id(Map<String, Object> row) {
        return ((Number) row.get("id")).longValue();
    }

    private static List<MockWxShopClient.LineSpec> line(Ctx c, int qty) {
        return List.of(new MockWxShopClient.LineSpec(c.productId(), c.skuId(), qty));
    }

    // ---------------------------------------------------------------- 商品、库存

    @Test
    void productSyncAutoMatchAndStockPush() throws Exception {
        Ctx c = prepare(10, Map.of());
        // 商家编码匹配不到物料的商品：未映射
        mock.addProduct(c.shopId(), "未知商品", List.of(new MockWxShopClient.SkuSpec("NOPE" + uid(), null, null, BigDecimal.TEN)));
        Map<String, Object> result = sync(c.shopId(), "PRODUCT");
        assertThat(num(result.get("total"))).isEqualTo(2);

        List<Map<String, Object>> mapped = read("/channel/wx-product?size=50&mapped=true&shopId=" + c.shopId(), "$.data.records");
        assertThat(mapped).hasSize(1);
        assertThat(((Number) mapped.get(0).get("materialId")).longValue()).isEqualTo(c.materialId());
        List<Map<String, Object>> unmapped = read("/channel/wx-product?size=50&mapped=false&shopId=" + c.shopId(), "$.data.records");
        assertThat(unmapped).hasSize(1);

        // 推送数量 = 可用库存 10 − 安全库存 2
        Map<String, Object> stock = sync(c.shopId(), "STOCK");
        assertThat(num(stock.get("failed"))).isZero();
        assertThat(mock.stockOf(c.shopId(), c.productId(), c.skuId())).isEqualTo(8);

        // 手工映射：1 件 = 2 个，推送 ⌊8 ÷ 2⌋ = 4
        ok(send(put("/channel/wx-product/" + id(mapped.get(0)) + "/mapping"), admin, Map.of("materialId", c.materialId(), "unitRatio", 2)));
        sync(c.shopId(), "STOCK");
        assertThat(mock.stockOf(c.shopId(), c.productId(), c.skuId())).isEqualTo(4);

        // 同步日志
        List<Map<String, Object>> logs = read("/channel/wx-sync-log?size=50&shopId=" + c.shopId(), "$.data.records");
        assertThat(logs).extracting(l -> l.get("syncType")).contains("PRODUCT", "STOCK");
        // 有映射的物料不能删除
        fail(send(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/wms/material/" + c.materialId()), admin, Map.of()));
    }

    // ---------------------------------------------------------------- 订单 → 销售订单 → 发货 → 退货

    @Test
    void orderShipAndReturn() throws Exception {
        Ctx c = prepare(20, Map.of());
        // 3 件 × 50，运费 8，优惠 6：商品实付 144，单价 48
        String orderId = mock.addOrder(c.shopId(), line(c, 3), new BigDecimal("8"), new BigDecimal("6"), true);
        sync(c.shopId(), "ORDER");
        Map<String, Object> o = order(c.shopId(), orderId);
        assertThat(o.get("erpStatus")).isEqualTo("ORDERED");
        assertThat(num(o.get("paidAmount"))).isEqualTo(152);
        long erpOrderId = ((Number) o.get("erpOrderId")).longValue();
        Map<String, Object> erp = read("/scm/sales/order/" + erpOrderId, "$.data");
        assertThat(erp.get("status")).isEqualTo("APPROVED");
        assertThat(erp.get("source")).isEqualTo("WX_SHOP");
        assertThat((String) erp.get("remark")).contains(orderId);
        List<Map<String, Object>> erpItems = com.lemon.erp.support.TestCasts.cast(erp.get("items"));
        assertThat(num(erpItems.get(0).get("qty"))).isEqualTo(3);
        assertThat(num(erpItems.get(0).get("price"))).isEqualTo(48);
        // 渠道订单不能反审核
        fail(send(post("/scm/sales/order/" + erpOrderId + "/unapprove"), admin, Map.of()));

        // 已付款未出库的数量从推送库存中扣除：20 − 3 − 2 = 15
        sync(c.shopId(), "STOCK");
        assertThat(mock.stockOf(c.shopId(), c.productId(), c.skuId())).isEqualTo(15);

        // 发货：生成销售出库，回传运单号
        List<Map<String, Object>> companies = read("/channel/wx-shop/" + c.shopId() + "/delivery-companies", "$.data");
        assertThat(companies).isNotEmpty();
        String waybill = "SF" + uid();
        ok(send(post("/channel/wx-order/" + id(o) + "/ship"), admin, Map.of("companyId", companies.get(0).get("id"),
                "companyName", companies.get(0).get("name"), "waybillNo", waybill)));
        o = order(c.shopId(), orderId);
        assertThat(o.get("erpStatus")).isEqualTo("OUTBOUND");
        assertThat(o.get("deliveryStatus")).isEqualTo("PUSHED");
        assertThat(o.get("status")).isEqualTo("SHIPPED");
        assertThat((String) o.get("salesOutNo")).isNotBlank();
        assertThat(mock.deliveries(c.shopId())).extracting(WxShopClient.DeliveryRequest::waybillNo).containsExactly(waybill);
        assertThat(stockQty(c.warehouseId(), c.materialId())).isEqualTo(17);
        assertThat(orderStatus("sales", erpOrderId)).isEqualTo("COMPLETED");
        // 不能重复发货
        fail(send(post("/channel/wx-order/" + id(o) + "/ship"), admin, Map.of("companyId", "SF", "waybillNo", "X1")));

        // 退货退款：同意 → 买家寄回 → 确认收货退款 → 销售退货入库
        String asId = mock.addAftersale(c.shopId(), orderId, true, c.skuId(), 1, new BigDecimal("48"), "不喜欢");
        sync(c.shopId(), "AFTERSALE");
        Map<String, Object> a = aftersale(c.shopId(), asId);
        assertThat(a.get("type")).isEqualTo("RETURN");
        assertThat(a.get("status")).isEqualTo("PENDING");
        // 未到收货环节不能确认收货
        fail(send(post("/channel/wx-aftersale/" + id(a) + "/confirm-receive"), admin, Map.of()));
        ok(send(post("/channel/wx-aftersale/" + id(a) + "/accept"), admin, Map.of()));
        assertThat(aftersale(c.shopId(), asId).get("status")).isEqualTo("WAIT_RETURN");
        mock.buyerReturned(c.shopId(), asId);
        ok(send(post("/channel/wx-aftersale/" + id(a) + "/refresh"), admin, Map.of()));
        assertThat(aftersale(c.shopId(), asId).get("status")).isEqualTo("WAIT_RECEIPT");
        ok(send(post("/channel/wx-aftersale/" + id(a) + "/confirm-receive"), admin, Map.of()));
        a = aftersale(c.shopId(), asId);
        assertThat(a.get("status")).isEqualTo("REFUNDED");
        assertThat(a.get("erpStatus")).isEqualTo("DONE");
        assertThat((String) a.get("salesReturnNo")).isNotBlank();
        assertThat(stockQty(c.warehouseId(), c.materialId())).isEqualTo(18);
    }

    // ---------------------------------------------------------------- 待映射、发货前退款、取消、小店后台发货

    @Test
    void unmappedRetryRefundCancelAndPlatformShip() throws Exception {
        Ctx c = prepare(50, Map.of());
        // 第二个商品没有匹配的物料
        WxShopClient.Product other = mock.addProduct(c.shopId(), "新品", List.of(new MockWxShopClient.SkuSpec("NEW" + uid(), null, null,
                new BigDecimal("30"))));
        sync(c.shopId(), "PRODUCT");
        String o1 = mock.addOrder(c.shopId(), List.of(new MockWxShopClient.LineSpec(c.productId(), c.skuId(), 1),
                new MockWxShopClient.LineSpec(other.productId(), other.skus().get(0).skuId(), 2)), BigDecimal.ZERO, BigDecimal.ZERO, true);
        sync(c.shopId(), "ORDER");
        Map<String, Object> order1 = order(c.shopId(), o1);
        assertThat(order1.get("erpStatus")).isEqualTo("UNMAPPED");
        assertThat(order1.get("erpOrderId")).isNull();
        // 未映射时重试仍提示
        fail(send(post("/channel/wx-order/" + id(order1) + "/retry"), admin, Map.of()));
        // 映射后重试：生成销售订单
        long m2 = newMaterial("WXN" + uid());
        List<Map<String, Object>> rows = read("/channel/wx-product?size=50&mapped=false&shopId=" + c.shopId(), "$.data.records");
        ok(send(put("/channel/wx-product/" + id(rows.get(0)) + "/mapping"), admin, Map.of("materialId", m2)));
        ok(send(post("/channel/wx-order/" + id(order1) + "/retry"), admin, Map.of()));
        order1 = order(c.shopId(), o1);
        assertThat(order1.get("erpStatus")).isEqualTo("ORDERED");
        List<Map<String, Object>> items1 = com.lemon.erp.support.TestCasts.cast(order1.get("items"));
        assertThat(items1).extracting(i -> ((Number) i.get("materialId")).longValue()).containsExactly(c.materialId(), m2);

        // 发货前部分退款：2 件退 1 件，发货只出 1 件，订单其余数量关闭
        String o2 = mock.addOrder(c.shopId(), line(c, 2), BigDecimal.ZERO, BigDecimal.ZERO, true);
        sync(c.shopId(), "ORDER");
        String as2 = mock.addAftersale(c.shopId(), o2, false, c.skuId(), 1, new BigDecimal("50"), "不想要了");
        sync(c.shopId(), "AFTERSALE");
        ok(send(post("/channel/wx-aftersale/" + id(aftersale(c.shopId(), as2)) + "/accept"), admin, Map.of()));
        Map<String, Object> a2 = aftersale(c.shopId(), as2);
        assertThat(a2.get("status")).isEqualTo("REFUNDED");
        assertThat(a2.get("erpStatus")).isEqualTo("DONE");
        Map<String, Object> order2 = order(c.shopId(), o2);
        List<Map<String, Object>> items2 = com.lemon.erp.support.TestCasts.cast(order2.get("items"));
        assertThat(num(items2.get(0).get("refundedQty"))).isEqualTo(1);
        double before = stockQty(c.warehouseId(), c.materialId());
        ok(send(post("/channel/wx-order/" + id(order2) + "/ship"), admin, Map.of("companyId", "ZTO", "waybillNo", "ZT" + uid())));
        assertThat(stockQty(c.warehouseId(), c.materialId())).isEqualTo(before - 1);
        assertThat(orderStatus("sales", ((Number) order2.get("erpOrderId")).longValue())).isEqualTo("CLOSED");
        assertThat(mock.deliveries(c.shopId()).get(0).items().get(0).qty()).isEqualTo(1);

        // 发货前全部退款：关闭销售订单，线上订单随后变为取消
        String o3 = mock.addOrder(c.shopId(), line(c, 1), BigDecimal.ZERO, BigDecimal.ZERO, true);
        sync(c.shopId(), "ORDER");
        String as3 = mock.addAftersale(c.shopId(), o3, false, c.skuId(), 1, new BigDecimal("50"), "拍错了");
        sync(c.shopId(), "AFTERSALE");
        ok(send(post("/channel/wx-aftersale/" + id(aftersale(c.shopId(), as3)) + "/accept"), admin, Map.of()));
        Map<String, Object> order3 = order(c.shopId(), o3);
        assertThat(order3.get("erpStatus")).isEqualTo("CLOSED");
        assertThat(orderStatus("sales", ((Number) order3.get("erpOrderId")).longValue())).isEqualTo("CLOSED");
        sync(c.shopId(), "ORDER");
        assertThat(order(c.shopId(), o3).get("status")).isEqualTo("CANCELLED");

        // 发货前取消：关闭销售订单
        String o4 = mock.addOrder(c.shopId(), line(c, 1), BigDecimal.ZERO, BigDecimal.ZERO, true);
        sync(c.shopId(), "ORDER");
        long erp4 = ((Number) order(c.shopId(), o4).get("erpOrderId")).longValue();
        mock.cancel(c.shopId(), o4);
        sync(c.shopId(), "ORDER");
        Map<String, Object> order4 = order(c.shopId(), o4);
        assertThat(order4.get("status")).isEqualTo("CANCELLED");
        assertThat(order4.get("erpStatus")).isEqualTo("CLOSED");
        assertThat(orderStatus("sales", erp4)).isEqualTo("CLOSED");

        // 未付款不处理，付款后生成；在小店后台发货：自动出库
        String o5 = mock.addOrder(c.shopId(), line(c, 2), BigDecimal.ZERO, BigDecimal.ZERO, false);
        sync(c.shopId(), "ORDER");
        assertThat(order(c.shopId(), o5).get("erpStatus")).isEqualTo("NONE");
        mock.pay(c.shopId(), o5);
        sync(c.shopId(), "ORDER");
        assertThat(order(c.shopId(), o5).get("erpStatus")).isEqualTo("ORDERED");
        before = stockQty(c.warehouseId(), c.materialId());
        mock.shipOnPlatform(c.shopId(), o5, "YTO", "YT" + uid());
        sync(c.shopId(), "ORDER");
        Map<String, Object> order5 = order(c.shopId(), o5);
        assertThat(order5.get("erpStatus")).isEqualTo("OUTBOUND");
        assertThat(order5.get("deliveryStatus")).isEqualTo("PLATFORM");
        assertThat(stockQty(c.warehouseId(), c.materialId())).isEqualTo(before - 2);
    }

    @Test
    void manualCreateWhenAutoDisabled() throws Exception {
        Ctx c = prepare(5, Map.of("autoCreateOrder", false));
        String o = mock.addOrder(c.shopId(), line(c, 1), BigDecimal.ZERO, BigDecimal.ZERO, true);
        sync(c.shopId(), "ORDER");
        Map<String, Object> order = order(c.shopId(), o);
        assertThat(order.get("erpStatus")).isEqualTo("PENDING");
        ok(send(post("/channel/wx-order/" + id(order) + "/retry"), admin, Map.of()));
        assertThat(order(c.shopId(), o).get("erpStatus")).isEqualTo("ORDERED");
        // 模拟订单只能在模拟店铺生成；生成后自动同步
        Number count = ok(send(post("/channel/wx-shop/" + c.shopId() + "/mock/orders"), admin, Map.of("count", 2)));
        assertThat(count.intValue()).isEqualTo(2);
    }

    // ---------------------------------------------------------------- 消息推送

    @Test
    void callbackSignatureAndAsyncProcessing() throws Exception {
        Ctx c = prepare(10, Map.of());
        String url = "/channel/wx-shop/callback/" + c.shopId();
        String ts = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = "n" + uid();
        String sig = WxCallbackCrypto.signature(PUSH_TOKEN, ts, nonce);

        // 地址校验：签名不对拒绝，正确时原样返回 echostr（免登录）
        assertThat(mvc.perform(get(url).param("signature", "bad").param("timestamp", ts).param("nonce", nonce).param("echostr", "hello"))
                .andReturn().getResponse().getStatus()).isEqualTo(403);
        MvcResult okResult = mvc.perform(get(url).param("signature", sig).param("timestamp", ts).param("nonce", nonce).param("echostr", "hello"))
                .andReturn();
        assertThat(okResult.getResponse().getStatus()).isEqualTo(200);
        assertThat(okResult.getResponse().getContentAsString()).isEqualTo("hello");

        // 事件：签名不对拒绝，不处理
        String orderId = mock.addOrder(c.shopId(), line(c, 1), BigDecimal.ZERO, BigDecimal.ZERO, true);
        String event = "{\"ToUserName\":\"gh_test\",\"Event\":\"channels_ec_order_pay\",\"order_info\":{\"order_id\":\"" + orderId + "\"}}";
        assertThat(mvc.perform(post(url).param("signature", "bad").param("timestamp", ts).param("nonce", nonce)
                .contentType(MediaType.APPLICATION_JSON).content(event)).andReturn().getResponse().getStatus()).isEqualTo(403);
        // 签名正确：立即返回 success，异步拉取订单并生成销售订单
        MvcResult accepted = mvc.perform(post(url).param("signature", sig).param("timestamp", ts).param("nonce", nonce)
                .contentType(MediaType.APPLICATION_JSON).content(event)).andReturn();
        assertThat(accepted.getResponse().getContentAsString()).isEqualTo("success");
        assertThat(waitErpStatus(c.shopId(), orderId)).isEqualTo("ORDERED");

        // 停用或不存在的店铺拒绝
        assertThat(mvc.perform(get("/channel/wx-shop/callback/999999").param("signature", sig).param("timestamp", ts).param("nonce", nonce)
                .param("echostr", "x")).andReturn().getResponse().getStatus()).isEqualTo(403);

        // 安全模式：加密消息，校验 msg_signature
        Ctx enc = prepare(5, Map.of("aesKey", AES_KEY));
        String encUrl = "/channel/wx-shop/callback/" + enc.shopId();
        String orderId2 = mock.addOrder(enc.shopId(), line(enc, 1), BigDecimal.ZERO, BigDecimal.ZERO, true);
        String cipher = WxCallbackCrypto.encrypt(AES_KEY, null,
                "{\"Event\":\"channels_ec_order_new\",\"order_info\":{\"order_id\":\"" + orderId2 + "\"}}");
        String encBody = "{\"ToUserName\":\"gh_test\",\"Encrypt\":\"" + cipher + "\"}";
        assertThat(mvc.perform(post(encUrl).param("signature", sig).param("timestamp", ts).param("nonce", nonce)
                .param("msg_signature", "bad").contentType(MediaType.APPLICATION_JSON).content(encBody)).andReturn().getResponse().getStatus())
                .isEqualTo(403);
        String msgSig = WxCallbackCrypto.signature(PUSH_TOKEN, ts, nonce, cipher);
        assertThat(mvc.perform(post(encUrl).param("signature", sig).param("timestamp", ts).param("nonce", nonce)
                .param("msg_signature", msgSig).contentType(MediaType.APPLICATION_JSON).content(encBody)).andReturn().getResponse()
                .getContentAsString()).isEqualTo("success");
        assertThat(waitErpStatus(enc.shopId(), orderId2)).isEqualTo("ORDERED");
        // 推送处理记录在同步日志中
        List<Map<String, Object>> logs = read("/channel/wx-sync-log?size=50&shopId=" + enc.shopId(), "$.data.records");
        assertThat(logs).extracting(l -> l.get("syncTrigger")).contains("CALLBACK");
    }

    /** 等待异步处理完成，返回 ERP 处理状态（超时返回最后一次的状态） */
    private String waitErpStatus(long shop, String orderId) throws Exception {
        String status = null;
        for (int i = 0; i < 50; i++) {
            List<String> rows = read("/channel/wx-order?size=50&shopId=" + shop + "&keyword=" + orderId, "$.data.records[*].erpStatus");
            status = rows.isEmpty() ? null : rows.get(0);
            if ("ORDERED".equals(status)) {
                return status;
            }
            Thread.sleep(100);
        }
        return status;
    }

    // ---------------------------------------------------------------- 数据权限、脱敏、店铺规则

    @Test
    void storeScopeAndSensitiveData() throws Exception {
        Ctx a = prepare(5, Map.of());
        Ctx b = prepare(5, Map.of());
        String oa = mock.addOrder(a.shopId(), line(a, 1), BigDecimal.ZERO, BigDecimal.ZERO, true);
        String ob = mock.addOrder(b.shopId(), line(b, 1), BigDecimal.ZERO, BigDecimal.ZERO, true);
        sync(a.shopId(), "ORDER");
        sync(b.shopId(), "ORDER");
        long idB = id(order(b.shopId(), ob));

        // 管理员看到完整的收货信息
        Map<String, Object> full = order(a.shopId(), oa);
        assertThat(full.get("receiverPhone")).isEqualTo("13800138000");
        assertThat(full.get("sensitiveVisible")).isEqualTo(true);

        // 只管门店 A 的用户（总经理角色，有查看收货信息权限）：只能看到门店 A 的订单
        String userA = createUser("wxa" + uid().toLowerCase(), List.of(3), a.storeId(), "ASSIGNED");
        List<Map<String, Object>> rows = JsonPath.read(body(getJson("/channel/wx-order?size=500", userA)), "$.data.records");
        assertThat(rows).isNotEmpty().allSatisfy(r -> assertThat(((Number) r.get("storeId")).longValue()).isEqualTo(a.storeId()));
        assertThat(rows).extracting(r -> r.get("orderId")).contains(oa).doesNotContain(ob);
        // 列表不返回收货信息
        assertThat(rows.get(0).get("receiverPhone")).isNull();
        assertThat(rows.get(0).get("receiverAddress")).isNull();
        fail(getJson("/channel/wx-order/" + idB, userA));
        List<Map<String, Object>> shops = JsonPath.read(body(getJson("/channel/wx-shop?size=500", userA)), "$.data.records");
        assertThat(shops).extracting(s -> ((Number) s.get("id")).longValue()).contains(a.shopId()).doesNotContain(b.shopId());
        fail(send(post("/channel/wx-shop/" + b.shopId() + "/sync?type=ORDER"), userA, Map.of()));

        // 普通用户（没有查看收货信息权限）：详情脱敏
        String viewer = createUser("wxv" + uid().toLowerCase(), List.of(2), null, "ALL");
        Map<String, Object> masked = JsonPath.read(body(getJson("/channel/wx-order/" + id(full), viewer)), "$.data");
        assertThat(masked.get("sensitiveVisible")).isEqualTo(false);
        assertThat((String) masked.get("receiverPhone")).isEqualTo("138****8000");
        assertThat((String) masked.get("receiverName")).endsWith("**");
        // 没有发货权限
        fail(send(post("/channel/wx-order/" + id(full) + "/ship"), viewer, Map.of("companyId", "SF", "waybillNo", "X")));

        // 店铺规则：密钥不返回；发货仓库必须属于绑定门店；有订单的店铺不能删除、不能改绑门店
        Map<String, Object> shop = read("/channel/wx-shop/" + a.shopId(), "$.data");
        assertThat(shop).doesNotContainKey("pushToken").doesNotContainKey("appSecret");
        assertThat(shop.get("pushTokenSet")).isEqualTo(true);
        assertThat((String) shop.get("callbackPath")).endsWith("/callback/" + a.shopId());
        fail(send(post("/channel/wx-shop"), admin, Map.of("name", "错仓库" + uid(), "mode", "MOCK", "storeId", a.storeId(),
                "warehouseId", b.warehouseId())));
        fail(send(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/channel/wx-shop/" + a.shopId()), admin, Map.of()));
        // 正式对接必须填写 AppID、AppSecret
        fail(send(post("/channel/wx-shop"), admin, Map.of("name", "正式" + uid(), "mode", "LIVE", "storeId", a.storeId(),
                "warehouseId", a.warehouseId())));
        // 生成模拟订单只能用于模拟店铺：正式店铺拒绝
        long live = ((Number) ok(send(post("/channel/wx-shop"), admin, Map.of("name", "正式" + uid(), "mode", "LIVE", "storeId", a.storeId(),
                "warehouseId", a.warehouseId(), "appId", "wx0000", "appSecret", "secret")))).longValue();
        fail(send(post("/channel/wx-shop/" + live + "/mock/orders"), admin, Map.of("count", 1)));
        // 修改时密钥留空保持不变
        ok(send(put("/channel/wx-shop/" + live), admin, Map.of("name", "正式改" + uid(), "mode", "LIVE", "storeId", a.storeId(),
                "warehouseId", a.warehouseId(), "appId", "wx0000")));
        assertThat(this.<Boolean>read("/channel/wx-shop/" + live, "$.data.appSecretSet")).isTrue();
        ok(send(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/channel/wx-shop/" + live), admin, Map.of()));
    }

    private String createUser(String username, List<Integer> roles, Long storeId, String scope) throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of("username", username, "password", "abc12345", "nickname", username,
                "roleIds", roles, "storeScope", scope));
        if (storeId != null) {
            body.put("storeId", storeId);
        }
        ok(send(post("/system/user"), admin, body));
        String res = body(send(post("/auth/login"), null, Map.of("username", username, "password", "abc12345", "tenantName", PLATFORM_TENANT)));
        return "Bearer " + JsonPath.read(res, "$.data.token");
    }
}
