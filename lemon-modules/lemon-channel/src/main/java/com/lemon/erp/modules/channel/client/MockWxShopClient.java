package com.lemon.erp.modules.channel.client;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.channel.model.WxShopMode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * 模拟店铺（MOCK）：内存中的微信小店，每个店铺（按本系统店铺 ID）各自一份商品、订单、售后单，重启后清空。
 * 未开通小店时用于演示完整流程，也用于自动化测试。除了 {@link WxShopClient} 的接口，还提供构造数据、模拟买家与小店后台操作的方法
 * （新增商品 / 订单、在小店后台发货、取消订单、申请售后、买家寄回退货）。
 * <p>
 * 状态流转与真实接口一致：订单 10 待付款 → 20 待发货 → 30 已发货 → 100 已完成，取消为 250（未发货全部退款为 200）；
 * 售后 MERCHANT_PROCESSING → 仅退款同意即 MERCHANT_REFUND_SUCCESS，退货退款同意后 USER_WAIT_RETURN → 买家寄回 MERCHANT_WAIT_RECEIPT
 * → 确认收货 MERCHANT_REFUND_SUCCESS；拒绝为 MERCHANT_REJECT_REFUND / MERCHANT_REJECT_RETURN
 */
@Component
public class MockWxShopClient implements WxShopClient {

    public static final int STATUS_UNPAID = 10;
    public static final int STATUS_TO_SHIP = 20;
    public static final int STATUS_SHIPPED = 30;
    public static final int STATUS_COMPLETED = 100;
    public static final int STATUS_REFUNDED_CANCEL = 200;
    public static final int STATUS_CANCELLED = 250;

    static final String AS_PROCESSING = "MERCHANT_PROCESSING";
    static final String AS_WAIT_RETURN = "USER_WAIT_RETURN";
    static final String AS_WAIT_RECEIPT = "MERCHANT_WAIT_RECEIPT";
    static final String AS_REFUND_SUCCESS = "MERCHANT_REFUND_SUCCESS";
    static final String AS_REJECT_REFUND = "MERCHANT_REJECT_REFUND";
    static final String AS_REJECT_RETURN = "MERCHANT_REJECT_RETURN";
    static final String TYPE_RETURN = "RETURN";
    static final String TYPE_REFUND = "REFUND";

    /** 模拟的快递公司：编码 → 名称的消息键 */
    private static final Map<String, String> COMPANIES = new LinkedHashMap<>();

    static {
        COMPANIES.put("SF", "channel.text.mockCompanySf");
        COMPANIES.put("ZTO", "channel.text.mockCompanyZto");
        COMPANIES.put("YTO", "channel.text.mockCompanyYto");
        COMPANIES.put("YD", "channel.text.mockCompanyYd");
        COMPANIES.put("JD", "channel.text.mockCompanyJd");
        COMPANIES.put("EMS", "channel.text.mockCompanyEms");
    }

    private final Map<Long, Shop> shops = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(System.currentTimeMillis() % 100_000_000L * 100);

    @Override
    public WxShopMode mode() {
        return WxShopMode.MOCK;
    }

    @Override
    public int test(Credentials c) {
        return shop(c.shopId()).products.size();
    }

    @Override
    public Page<String> listProductIds(Credentials c, String nextKey) {
        Shop shop = shop(c.shopId());
        synchronized (shop) {
            return page(new ArrayList<>(shop.products.keySet()), nextKey);
        }
    }

    @Override
    public Product getProduct(Credentials c, String productId) {
        Shop shop = shop(c.shopId());
        synchronized (shop) {
            MockProduct p = shop.products.get(productId);
            if (p == null) {
                throw new BizException("channel.error.mockNotFound", productId);
            }
            return p.toProduct();
        }
    }

    @Override
    public void setStock(Credentials c, String productId, String skuId, long qty) {
        Shop shop = shop(c.shopId());
        synchronized (shop) {
            sku(shop, productId, skuId).stock = qty;
        }
    }

    @Override
    public Page<String> listOrderIds(Credentials c, LocalDateTime from, LocalDateTime to, String nextKey) {
        Shop shop = shop(c.shopId());
        synchronized (shop) {
            return page(shop.orders.values().stream().filter(inRange(o -> o.updateTime, from, to)).map(o -> o.orderId).toList(), nextKey);
        }
    }

    @Override
    public Order getOrder(Credentials c, String orderId) {
        Shop shop = shop(c.shopId());
        synchronized (shop) {
            return order(shop, orderId).toOrder();
        }
    }

    @Override
    public List<DeliveryCompany> deliveryCompanies(Credentials c) {
        return COMPANIES.entrySet().stream().map(e -> new DeliveryCompany(e.getKey(), Messages.get(e.getValue()))).toList();
    }

    @Override
    public void sendDelivery(Credentials c, DeliveryRequest req) {
        Shop shop = shop(c.shopId());
        synchronized (shop) {
            MockOrder o = order(shop, req.orderId());
            if (o.status != STATUS_TO_SHIP) {
                throw new BizException("channel.error.mockInvalidState", req.orderId(), o.status);
            }
            if (!COMPANIES.containsKey(req.companyId())) {
                throw new BizException("channel.error.mockNotFound", req.companyId());
            }
            shop.deliveries.add(req);
            o.shipment = new Shipment(req.companyId(), Messages.get(COMPANIES.get(req.companyId())), req.waybillNo(), LocalDateTime.now());
            o.status = STATUS_SHIPPED;
            o.touch();
        }
    }

    @Override
    public Page<String> listAftersaleIds(Credentials c, LocalDateTime from, LocalDateTime to, String nextKey) {
        Shop shop = shop(c.shopId());
        synchronized (shop) {
            return page(shop.aftersales.values().stream().filter(inRange(a -> a.createTime, from, to)).map(a -> a.aftersaleId).toList(), nextKey);
        }
    }

    @Override
    public Aftersale getAftersale(Credentials c, String aftersaleId) {
        Shop shop = shop(c.shopId());
        synchronized (shop) {
            return aftersale(shop, aftersaleId).toAftersale();
        }
    }

    @Override
    public void acceptAftersale(Credentials c, String aftersaleId) {
        Shop shop = shop(c.shopId());
        synchronized (shop) {
            MockAftersale a = aftersale(shop, aftersaleId);
            requireStatus(a, AS_PROCESSING);
            if (TYPE_RETURN.equals(a.type)) {
                a.change(AS_WAIT_RETURN);
            } else {
                refund(shop, a);
            }
        }
    }

    @Override
    public void rejectAftersale(Credentials c, String aftersaleId, String reason) {
        Shop shop = shop(c.shopId());
        synchronized (shop) {
            MockAftersale a = aftersale(shop, aftersaleId);
            requireStatus(a, AS_PROCESSING);
            a.change(TYPE_RETURN.equals(a.type) ? AS_REJECT_RETURN : AS_REJECT_REFUND);
        }
    }

    @Override
    public void confirmReturnReceived(Credentials c, String aftersaleId) {
        Shop shop = shop(c.shopId());
        synchronized (shop) {
            MockAftersale a = aftersale(shop, aftersaleId);
            requireStatus(a, AS_WAIT_RECEIPT);
            refund(shop, a);
        }
    }

    // ---------------------------------------------------------------- 构造数据、模拟买家与小店后台操作

    /**
     * 新增商品
     *
     * @return 商品（含生成的商品 ID、SKU ID）
     */
    public Product addProduct(Long shopId, String title, List<SkuSpec> skus) {
        Shop shop = shop(shopId);
        synchronized (shop) {
            MockProduct p = new MockProduct();
            p.productId = String.valueOf(seq.incrementAndGet());
            p.title = title;
            for (SkuSpec s : skus) {
                MockSku sku = new MockSku();
                sku.skuId = String.valueOf(seq.incrementAndGet());
                sku.outSkuId = s.outSkuId();
                sku.skuCode = s.skuCode();
                sku.attrs = s.attrs();
                sku.price = s.price().setScale(2, RoundingMode.HALF_UP);
                sku.stock = 0;
                p.skus.add(sku);
            }
            shop.products.put(p.productId, p);
            return p.toProduct();
        }
    }

    /** 店铺中的商品数 */
    public int productCount(Long shopId) {
        Shop shop = shop(shopId);
        synchronized (shop) {
            return shop.products.size();
        }
    }

    /** SKU 当前的线上库存 */
    public long stockOf(Long shopId, String productId, String skuId) {
        Shop shop = shop(shopId);
        synchronized (shop) {
            return sku(shop, productId, skuId).stock;
        }
    }

    /**
     * 买家下单
     *
     * @param paid 是否已付款（待发货），否则为待付款
     * @return 订单号
     */
    public String addOrder(Long shopId, List<LineSpec> lines, BigDecimal freight, BigDecimal discount, boolean paid) {
        Shop shop = shop(shopId);
        synchronized (shop) {
            MockOrder o = new MockOrder();
            o.orderId = "37" + seq.incrementAndGet();
            o.createTime = LocalDateTime.now();
            o.buyer = "mock_" + Long.toString(seq.incrementAndGet(), 36);
            o.receiver = new Receiver(Messages.get("channel.text.mockReceiver"), "13800138000", Messages.get("channel.text.mockAddress"));
            o.freight = freight == null ? BigDecimal.ZERO : freight;
            o.discount = discount == null ? BigDecimal.ZERO : discount;
            for (LineSpec l : lines) {
                MockProduct p = shop.products.get(l.productId());
                MockSku sku = sku(shop, l.productId(), l.skuId());
                o.lines.add(new OrderLine(p.productId, sku.skuId, p.title, sku.attrs, sku.outSkuId, sku.skuCode, l.qty(), sku.price, null));
            }
            o.status = paid ? STATUS_TO_SHIP : STATUS_UNPAID;
            o.payTime = paid ? o.createTime : null;
            o.touch();
            shop.orders.put(o.orderId, o);
            return o.orderId;
        }
    }

    /** 买家付款 */
    public void pay(Long shopId, String orderId) {
        Shop shop = shop(shopId);
        synchronized (shop) {
            MockOrder o = order(shop, orderId);
            if (o.status != STATUS_UNPAID) {
                throw new BizException("channel.error.mockInvalidState", orderId, o.status);
            }
            o.status = STATUS_TO_SHIP;
            o.payTime = LocalDateTime.now();
            o.touch();
        }
    }

    /** 在小店后台发货（不经过 ERP） */
    public void shipOnPlatform(Long shopId, String orderId, String companyId, String waybillNo) {
        Shop shop = shop(shopId);
        synchronized (shop) {
            MockOrder o = order(shop, orderId);
            if (o.status != STATUS_TO_SHIP) {
                throw new BizException("channel.error.mockInvalidState", orderId, o.status);
            }
            o.shipment = new Shipment(companyId, Messages.get(COMPANIES.getOrDefault(companyId, "channel.text.mockCompanySf")), waybillNo,
                    LocalDateTime.now());
            o.status = STATUS_SHIPPED;
            o.touch();
        }
    }

    /** 取消订单（未发货） */
    public void cancel(Long shopId, String orderId) {
        Shop shop = shop(shopId);
        synchronized (shop) {
            MockOrder o = order(shop, orderId);
            if (o.status != STATUS_UNPAID && o.status != STATUS_TO_SHIP) {
                throw new BizException("channel.error.mockInvalidState", orderId, o.status);
            }
            o.status = STATUS_CANCELLED;
            o.touch();
        }
    }

    /** 当前订单状态码 */
    public int orderStatus(Long shopId, String orderId) {
        Shop shop = shop(shopId);
        synchronized (shop) {
            return order(shop, orderId).status;
        }
    }

    /** 店铺收到的发货回传（按时间顺序） */
    public List<DeliveryRequest> deliveries(Long shopId) {
        Shop shop = shop(shopId);
        synchronized (shop) {
            return List.copyOf(shop.deliveries);
        }
    }

    /**
     * 买家申请售后
     *
     * @param returns 退货退款，否则仅退款
     * @return 售后单号
     */
    public String addAftersale(Long shopId, String orderId, boolean returns, String skuId, int qty, BigDecimal amount, String reason) {
        Shop shop = shop(shopId);
        synchronized (shop) {
            MockOrder o = order(shop, orderId);
            OrderLine line = o.lines.stream().filter(l -> l.skuId().equals(skuId)).findFirst()
                    .orElseThrow(() -> new BizException("channel.error.mockNotFound", skuId));
            MockAftersale a = new MockAftersale();
            a.aftersaleId = "41" + seq.incrementAndGet();
            a.orderId = orderId;
            a.type = returns ? TYPE_RETURN : TYPE_REFUND;
            a.productId = line.productId();
            a.skuId = skuId;
            a.qty = qty;
            a.amount = amount;
            a.reason = reason;
            a.createTime = LocalDateTime.now();
            a.change(AS_PROCESSING);
            shop.aftersales.put(a.aftersaleId, a);
            return a.aftersaleId;
        }
    }

    /** 买家寄回退货 */
    public void buyerReturned(Long shopId, String aftersaleId) {
        Shop shop = shop(shopId);
        synchronized (shop) {
            MockAftersale a = aftersale(shop, aftersaleId);
            requireStatus(a, AS_WAIT_RETURN);
            a.change(AS_WAIT_RECEIPT);
        }
    }

    /** 当前售后状态 */
    public String aftersaleStatus(Long shopId, String aftersaleId) {
        Shop shop = shop(shopId);
        synchronized (shop) {
            return aftersale(shop, aftersaleId).status;
        }
    }

    /**
     * 新增商品的 SKU
     *
     * @param outSkuId 商家编码（自动匹配时与物料编码 / 条码比较）
     */
    public record SkuSpec(String outSkuId, String skuCode, String attrs, BigDecimal price) {
    }

    public record LineSpec(String productId, String skuId, int qty) {
    }

    // ---------------------------------------------------------------- 内部

    private Shop shop(Long shopId) {
        return shops.computeIfAbsent(Objects.requireNonNull(shopId), k -> new Shop());
    }

    private static MockSku sku(Shop shop, String productId, String skuId) {
        MockProduct p = shop.products.get(productId);
        MockSku sku = p == null ? null : p.skus.stream().filter(s -> s.skuId.equals(skuId)).findFirst().orElse(null);
        if (sku == null) {
            throw new BizException("channel.error.mockNotFound", productId + "/" + skuId);
        }
        return sku;
    }

    private static MockOrder order(Shop shop, String orderId) {
        MockOrder o = shop.orders.get(orderId);
        if (o == null) {
            throw new BizException("channel.error.mockNotFound", orderId);
        }
        return o;
    }

    private static MockAftersale aftersale(Shop shop, String aftersaleId) {
        MockAftersale a = shop.aftersales.get(aftersaleId);
        if (a == null) {
            throw new BizException("channel.error.mockNotFound", aftersaleId);
        }
        return a;
    }

    private static void requireStatus(MockAftersale a, String status) {
        if (!status.equals(a.status)) {
            throw new BizException("channel.error.mockInvalidState", a.aftersaleId, a.status);
        }
    }

    /** 退款完成；未发货的订单全部商品都退款后订单取消（200） */
    private static void refund(Shop shop, MockAftersale a) {
        a.change(AS_REFUND_SUCCESS);
        MockOrder o = shop.orders.get(a.orderId);
        if (o != null && o.status == STATUS_TO_SHIP) {
            boolean all = o.lines.stream().allMatch(l -> shop.aftersales.values().stream()
                    .filter(x -> x.orderId.equals(o.orderId) && x.skuId.equals(l.skuId()) && AS_REFUND_SUCCESS.equals(x.status))
                    .mapToInt(x -> x.qty).sum() >= l.qty());
            if (all) {
                o.status = STATUS_REFUNDED_CANCEL;
                o.touch();
            }
        }
    }

    private static <T> Predicate<T> inRange(java.util.function.Function<T, LocalDateTime> time, LocalDateTime from, LocalDateTime to) {
        return x -> {
            LocalDateTime t = time.apply(x);
            return !t.isBefore(from) && t.isBefore(to);
        };
    }

    /** 每页 20 条，nextKey 为下一页的起始序号 */
    private static Page<String> page(List<String> all, String nextKey) {
        int size = 20;
        int start = nextKey == null || nextKey.isBlank() ? 0 : Integer.parseInt(nextKey);
        int end = Math.min(all.size(), start + size);
        boolean more = end < all.size();
        return new Page<>(start >= all.size() ? List.of() : List.copyOf(all.subList(start, end)), more ? String.valueOf(end) : null, more,
                all.size());
    }

    private static final class Shop {
        final Map<String, MockProduct> products = new LinkedHashMap<>();
        final Map<String, MockOrder> orders = new LinkedHashMap<>();
        final Map<String, MockAftersale> aftersales = new LinkedHashMap<>();
        final List<DeliveryRequest> deliveries = new ArrayList<>();
    }

    private static final class MockProduct {
        String productId;
        String title;
        final List<MockSku> skus = new ArrayList<>();

        Product toProduct() {
            return new Product(productId, title, 5, skus.stream()
                    .map(s -> new Sku(s.skuId, s.outSkuId, s.skuCode, s.attrs, s.price, s.stock)).toList());
        }
    }

    private static final class MockSku {
        String skuId;
        String outSkuId;
        String skuCode;
        String attrs;
        BigDecimal price;
        long stock;
    }

    private static final class MockOrder {
        String orderId;
        int status;
        LocalDateTime createTime;
        LocalDateTime updateTime;
        LocalDateTime payTime;
        String buyer;
        Receiver receiver;
        BigDecimal freight;
        BigDecimal discount;
        final List<OrderLine> lines = new ArrayList<>();
        Shipment shipment;

        void touch() {
            updateTime = LocalDateTime.now();
        }

        Order toOrder() {
            BigDecimal goods = lines.stream().map(l -> l.salePrice().multiply(BigDecimal.valueOf(l.qty()))).reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal paid = goods.add(freight).subtract(discount).max(BigDecimal.ZERO);
            return new Order(orderId, status, createTime, updateTime, payTime, buyer, receiver, goods, freight, discount, paid,
                    List.copyOf(lines), shipment);
        }
    }

    private static final class MockAftersale {
        String aftersaleId;
        String orderId;
        String type;
        String status;
        String productId;
        String skuId;
        int qty;
        BigDecimal amount;
        String reason;
        LocalDateTime createTime;
        LocalDateTime updateTime;

        void change(String to) {
            status = to;
            updateTime = LocalDateTime.now();
        }

        Aftersale toAftersale() {
            return new Aftersale(aftersaleId, orderId, type, status, productId, skuId, qty, amount, reason, createTime, updateTime);
        }
    }
}
