package com.lemon.erp.modules.channel.client;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.cache.CacheService;
import com.lemon.erp.modules.channel.model.WxShopMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 微信小店服务端接口（LIVE）。接口地址、请求与响应字段都按官方文档编写，标注「待联调确认」的部分在开通小店后需要逐一核对；
 * 线上模型与 {@link WxShopClient} 模型的转换只在本类中。
 * <p>
 * 接口调用凭证用 stable_token 获取，按店铺缓存（{@value #TOKEN_KEY}{店铺ID}），提前 5 分钟过期；凭证失效的错误码自动刷新重试一次。
 * 金额单位为分（转换为元），时间为秒级时间戳（按服务器时区转换为本地时间）
 */
@Slf4j
@Component
public class HttpWxShopClient implements WxShopClient {

    /** 接口调用凭证的缓存前缀（登记在缓存监控中） */
    public static final String TOKEN_KEY = "wxshop:token:";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);
    private static final long TOKEN_EARLY_EXPIRE_SECONDS = 300;
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    /** access_token 无效或过期的错误码：刷新凭证后重试一次 */
    private static final Set<Long> TOKEN_ERRORS = Set.of(40001L, 40014L, 42001L);
    private static final int PAGE_SIZE = 100;

    // ---------------------------------------------------------------- 接口地址（按官方文档，待联调确认）

    /** 稳定版接口调用凭证（按官方文档，待联调确认） */
    static final String PATH_STABLE_TOKEN = "/cgi-bin/stable_token";
    /** 获取商品列表（按官方文档，待联调确认） */
    static final String PATH_PRODUCT_LIST = "/channels/ec/product/list/get";
    /** 获取商品详情（按官方文档，待联调确认） */
    static final String PATH_PRODUCT_GET = "/channels/ec/product/get";
    /** 更新商品库存（按官方文档，待联调确认） */
    static final String PATH_STOCK_UPDATE = "/channels/ec/product/stock/update";
    /** 获取订单列表（按官方文档，待联调确认） */
    static final String PATH_ORDER_LIST = "/channels/ec/order/list/get";
    /** 获取订单详情（按官方文档，待联调确认） */
    static final String PATH_ORDER_GET = "/channels/ec/order/get";
    /** 获取快递公司列表（按官方文档，待联调确认：新版可能为 deliverycompanylist/new/get） */
    static final String PATH_DELIVERY_COMPANIES = "/channels/ec/order/deliverycompanylist/get";
    /** 订单发货（按官方文档，待联调确认） */
    static final String PATH_DELIVERY_SEND = "/channels/ec/order/delivery/send";
    /** 获取售后单列表（按官方文档，待联调确认） */
    static final String PATH_AFTERSALE_LIST = "/channels/ec/aftersale/getaftersalelist";
    /** 获取售后单详情（按官方文档，待联调确认） */
    static final String PATH_AFTERSALE_GET = "/channels/ec/aftersale/getaftersaleorder";
    /** 同意售后（按官方文档，待联调确认：退货退款在买家寄回后再次调用即同意退款） */
    static final String PATH_AFTERSALE_ACCEPT = "/channels/ec/aftersale/acceptapply";
    /** 拒绝售后（按官方文档，待联调确认） */
    static final String PATH_AFTERSALE_REJECT = "/channels/ec/aftersale/rejectapply";

    // ---------------------------------------------------------------- 字段名与取值（按官方文档，待联调确认）

    /** 商品详情：1 获取线上数据（按官方文档，待联调确认） */
    static final int PRODUCT_DATA_ONLINE = 1;
    /** 库存修改方式：3 全量设置（1 增加、2 减少，按官方文档，待联调确认） */
    static final int STOCK_DIFF_SET = 3;
    /** 发货方式：1 自寄快递（按官方文档，待联调确认） */
    static final int DELIVER_TYPE_EXPRESS = 1;
    /** 拒绝售后的原因类型：1 其他（按官方文档，待联调确认） */
    static final int REJECT_REASON_OTHER = 1;

    private final ObjectMapper objectMapper;
    private final CacheService cache;
    private final String apiBase;
    private final RestClient client = RestClient.builder().requestFactory(requestFactory()).build();

    public HttpWxShopClient(ObjectMapper objectMapper, CacheService cache,
                            @Value("${lemon.channel.wx-shop.api-base:https://api.weixin.qq.com}") String apiBase) {
        this.objectMapper = objectMapper;
        this.cache = cache;
        this.apiBase = apiBase;
    }

    private static JdkClientHttpRequestFactory requestFactory() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }

    @Override
    public WxShopMode mode() {
        return WxShopMode.LIVE;
    }

    @Override
    public int test(Credentials c) {
        token(c, true);
        JsonNode res = call(c, PATH_PRODUCT_LIST, body("page_size", 1));
        return res.has("total_num") ? res.path("total_num").asInt() : -1;
    }

    @Override
    public Page<String> listProductIds(Credentials c, String nextKey) {
        // 请求 {page_size, next_key}，响应 {product_ids, next_key, total_num}（按官方文档，待联调确认）
        JsonNode res = call(c, PATH_PRODUCT_LIST, body("page_size", PAGE_SIZE, "next_key", nextKey));
        List<String> ids = texts(res.path("product_ids"));
        String next = text(res, "next_key");
        return new Page<>(ids, next, next != null && !ids.isEmpty(), res.path("total_num").asInt(ids.size()));
    }

    @Override
    public Product getProduct(Credentials c, String productId) {
        // 响应 {product: {product_id, title, status, skus: [{sku_id, out_sku_id, sku_code, sale_price(分), stock_num, sku_attrs: [{attr_key, attr_value}]}]}}
        JsonNode p = call(c, PATH_PRODUCT_GET, body("product_id", productId, "data_type", PRODUCT_DATA_ONLINE)).path("product");
        List<Sku> skus = new ArrayList<>();
        for (JsonNode s : p.path("skus")) {
            skus.add(new Sku(s.path("sku_id").asString(""), text(s, "out_sku_id"), text(s, "sku_code"), attrs(s.path("sku_attrs")),
                    yuan(s.path("sale_price")), s.has("stock_num") ? s.path("stock_num").asLong() : null));
        }
        return new Product(p.path("product_id").asString(productId), text(p, "title"),
                p.has("status") ? p.path("status").asInt() : null, skus);
    }

    @Override
    public void setStock(Credentials c, String productId, String skuId, long qty) {
        call(c, PATH_STOCK_UPDATE, body("product_id", productId, "sku_id", skuId, "diff_type", STOCK_DIFF_SET, "num", qty));
    }

    @Override
    public Page<String> listOrderIds(Credentials c, LocalDateTime from, LocalDateTime to, String nextKey) {
        // 请求 {update_time_range: {start_time, end_time}, page_size, next_key}，响应 {order_id_list, next_key, has_more}
        Map<String, Object> range = body("start_time", epoch(from), "end_time", epoch(to));
        JsonNode res = call(c, PATH_ORDER_LIST, body("update_time_range", range, "page_size", PAGE_SIZE, "next_key", nextKey));
        List<String> ids = texts(res.path("order_id_list"));
        return new Page<>(ids, text(res, "next_key"), res.path("has_more").asBoolean(false), ids.size());
    }

    @Override
    public Order getOrder(Credentials c, String orderId) {
        JsonNode o = call(c, PATH_ORDER_GET, body("order_id", orderId)).path("order");
        JsonNode detail = o.path("order_detail");
        JsonNode price = detail.path("price_info");
        JsonNode delivery = detail.path("delivery_info");
        JsonNode addr = delivery.path("address_info");
        List<OrderLine> lines = new ArrayList<>();
        for (JsonNode p : detail.path("product_infos")) {
            // real_price：本行实付（分），按官方文档，待联调确认是否为行合计
            lines.add(new OrderLine(p.path("product_id").asString(""), p.path("sku_id").asString(""), text(p, "title"),
                    attrs(p.path("sku_attrs")), text(p, "out_sku_id"), text(p, "sku_code"), p.path("sku_cnt").asInt(0),
                    yuan(p.path("sale_price")), p.has("real_price") ? yuan(p.path("real_price")) : null));
        }
        Receiver receiver = addr.isMissingNode() ? null : new Receiver(text(addr, "user_name"),
                StringUtils.hasText(text(addr, "virtual_order_tel_number")) ? text(addr, "virtual_order_tel_number") : text(addr, "tel_number"),
                join(text(addr, "province_name"), text(addr, "city_name"), text(addr, "county_name"), text(addr, "detail_info"),
                        text(addr, "house_number")));
        Shipment shipment = null;
        for (JsonNode d : delivery.path("delivery_product_info")) {
            shipment = new Shipment(text(d, "delivery_id"), text(d, "delivery_name"), text(d, "waybill_id"), time(d.path("delivery_time")));
        }
        BigDecimal goods = yuan(price.path("product_price"));
        BigDecimal freight = yuan(price.path("freight"));
        BigDecimal paid = yuan(price.path("order_price"));
        BigDecimal discount = goods.add(freight).subtract(paid).max(BigDecimal.ZERO);
        return new Order(o.path("order_id").asString(orderId), o.path("status").asInt(0), time(o.path("create_time")), time(o.path("update_time")),
                time(detail.path("pay_info").path("pay_time")), text(o, "openid"), receiver, goods, freight, discount, paid, lines, shipment);
    }

    @Override
    public List<DeliveryCompany> deliveryCompanies(Credentials c) {
        JsonNode res = call(c, PATH_DELIVERY_COMPANIES, Map.of());
        List<DeliveryCompany> list = new ArrayList<>();
        for (JsonNode n : res.path("company_list")) {
            list.add(new DeliveryCompany(n.path("delivery_id").asString(""), n.path("delivery_name").asString("")));
        }
        return list;
    }

    @Override
    public void sendDelivery(Credentials c, DeliveryRequest req) {
        List<Map<String, Object>> products = req.items().stream()
                .map(i -> body("product_id", i.productId(), "sku_id", i.skuId(), "product_cnt", i.qty())).toList();
        Map<String, Object> delivery = body("delivery_id", req.companyId(), "waybill_id", req.waybillNo(),
                "deliver_type", DELIVER_TYPE_EXPRESS, "product_infos", products);
        call(c, PATH_DELIVERY_SEND, body("order_id", req.orderId(), "delivery_list", List.of(delivery)));
    }

    @Override
    public Page<String> listAftersaleIds(Credentials c, LocalDateTime from, LocalDateTime to, String nextKey) {
        // 请求 {begin_create_time, end_create_time, next_key}，响应 {after_sale_order_id_list, next_key, has_more}
        JsonNode res = call(c, PATH_AFTERSALE_LIST, body("begin_create_time", epoch(from), "end_create_time", epoch(to), "next_key", nextKey));
        List<String> ids = texts(res.path("after_sale_order_id_list"));
        return new Page<>(ids, text(res, "next_key"), res.path("has_more").asBoolean(false), ids.size());
    }

    @Override
    public Aftersale getAftersale(Credentials c, String aftersaleId) {
        JsonNode a = call(c, PATH_AFTERSALE_GET, body("after_sale_order_id", aftersaleId)).path("after_sale_order");
        JsonNode product = a.path("product_info");
        String reason = text(a, "reason");
        if (reason == null) {
            reason = text(a.path("details"), "desc");
        }
        return new Aftersale(a.path("after_sale_order_id").asString(aftersaleId), text(a, "order_id"), text(a, "type"), text(a, "status"),
                text(product, "product_id"), text(product, "sku_id"), product.path("count").asInt(0),
                yuan(a.path("refund_info").path("amount")), reason, time(a.path("create_time")), time(a.path("update_time")));
    }

    @Override
    public void acceptAftersale(Credentials c, String aftersaleId) {
        call(c, PATH_AFTERSALE_ACCEPT, body("after_sale_order_id", aftersaleId, "address_id", c.returnAddressId()));
    }

    @Override
    public void rejectAftersale(Credentials c, String aftersaleId, String reason) {
        call(c, PATH_AFTERSALE_REJECT, body("after_sale_order_id", aftersaleId, "reject_reason", reason, "reject_reason_type", REJECT_REASON_OTHER));
    }

    @Override
    public void confirmReturnReceived(Credentials c, String aftersaleId) {
        call(c, PATH_AFTERSALE_ACCEPT, body("after_sale_order_id", aftersaleId));
    }

    @Override
    public void evictToken(Long shopId) {
        cache.delete(TOKEN_KEY + shopId);
    }

    // ---------------------------------------------------------------- 调用

    /** POST JSON，access_token 失效时刷新重试一次；errcode 不为 0 时抛出 */
    private JsonNode call(Credentials c, String path, Object body) {
        JsonNode res = post(uri(path, token(c, false)), body);
        long code = res.path("errcode").asLong(0);
        if (TOKEN_ERRORS.contains(code)) {
            res = post(uri(path, token(c, true)), body);
            code = res.path("errcode").asLong(0);
        }
        if (code != 0) {
            throw error(res);
        }
        return res;
    }

    /** 稳定版接口调用凭证：请求 {grant_type, appid, secret, force_refresh}，响应 {access_token, expires_in}（按官方文档，待联调确认） */
    private String token(Credentials c, boolean refresh) {
        String key = TOKEN_KEY + c.shopId();
        if (!refresh) {
            String cached = cache.get(key, String.class);
            if (cached != null) {
                return cached;
            }
        }
        if (!StringUtils.hasText(c.appId()) || !StringUtils.hasText(c.appSecret())) {
            throw new BizException("channel.error.appIdSecretRequired");
        }
        JsonNode res = post(URI.create(apiBase + PATH_STABLE_TOKEN), body("grant_type", "client_credential", "appid", c.appId(),
                "secret", c.appSecret(), "force_refresh", false));
        String token = text(res, "access_token");
        if (token == null) {
            throw error(res);
        }
        long ttl = res.path("expires_in").asLong(7200) - TOKEN_EARLY_EXPIRE_SECONDS;
        if (ttl > 0) {
            cache.set(key, token, Duration.ofSeconds(ttl));
        }
        return token;
    }

    private URI uri(String path, String token) {
        return UriComponentsBuilder.fromUriString(apiBase + path).queryParam("access_token", "{t}").encode().buildAndExpand(token).toUri();
    }

    private JsonNode post(URI uri, Object body) {
        try {
            // 微信返回的 Content-Type 可能是 text/plain，按字符串读取后再解析
            String text = client.post().uri(uri).contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(body)).retrieve().body(String.class);
            return objectMapper.readTree(text == null || text.isBlank() ? "{}" : text);
        } catch (RestClientException | JacksonException e) {
            log.warn("微信小店接口调用失败 {}", uri.getPath(), e);
            throw new BizException("channel.error.apiCallFailed", e.getMessage());
        }
    }

    private static BizException error(JsonNode res) {
        return new BizException("channel.error.apiReturnedError", res.path("errcode").asString(""), res.path("errmsg").asString(""));
    }

    // ---------------------------------------------------------------- 转换

    /** 请求体：值为空的字段不传 */
    private static Map<String, Object> body(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            Object v = kv[i + 1];
            if (v != null && !(v instanceof String s && s.isEmpty())) {
                map.put((String) kv[i], v);
            }
        }
        return map;
    }

    private static String text(JsonNode node, String field) {
        String s = node.path(field).asString("");
        return s.isBlank() ? null : s;
    }

    private static List<String> texts(JsonNode array) {
        List<String> list = new ArrayList<>();
        array.forEach(n -> list.add(n.asString("")));
        return list;
    }

    /** 规格：[{attr_key, attr_value}] → 「颜色:红;尺码:L」 */
    private static String attrs(JsonNode array) {
        List<String> parts = new ArrayList<>();
        array.forEach(a -> parts.add(a.path("attr_key").asString("") + ":" + a.path("attr_value").asString("")));
        return parts.isEmpty() ? null : String.join(";", parts);
    }

    /** 分 → 元 */
    private static BigDecimal yuan(JsonNode cents) {
        return BigDecimal.valueOf(cents.asLong(0)).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private static LocalDateTime time(JsonNode seconds) {
        long s = seconds.asLong(0);
        return s <= 0 ? null : LocalDateTime.ofInstant(Instant.ofEpochSecond(s), ZONE);
    }

    private static long epoch(LocalDateTime time) {
        return time.atZone(ZONE).toEpochSecond();
    }

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null) {
                sb.append(p);
            }
        }
        return sb.isEmpty() ? null : sb.toString();
    }
}
