package com.lemon.erp.modules.channel.client;

import com.lemon.erp.modules.channel.model.WxShopMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 微信小店服务端接口的抽象：业务代码只使用这里的模型，接口地址、字段名、单位（分、秒）的转换只在实现类中。
 * 两个实现：{@link HttpWxShopClient} 调用真实接口（LIVE），{@link MockWxShopClient} 是内存中的模拟店铺（MOCK，演示与测试）。
 * 金额一律为元（两位小数），时间为本地时间
 */
public interface WxShopClient {

    /** 实现对应的对接方式 */
    WxShopMode mode();

    /** 测试连接：获取接口调用凭证并读取第一页商品，返回商品总数（接口不返回时为 -1） */
    int test(Credentials c);

    /** 商品 ID 列表（分页，nextKey 为空表示第一页） */
    Page<String> listProductIds(Credentials c, String nextKey);

    /** 商品详情（线上数据，含 SKU） */
    Product getProduct(Credentials c, String productId);

    /** 把 SKU 的库存设置为 qty（全量覆盖） */
    void setStock(Credentials c, String productId, String skuId, long qty);

    /** 按更新时间范围 [from, to) 查询订单 ID（分页） */
    Page<String> listOrderIds(Credentials c, LocalDateTime from, LocalDateTime to, String nextKey);

    Order getOrder(Credentials c, String orderId);

    /** 快递公司列表 */
    List<DeliveryCompany> deliveryCompanies(Credentials c);

    /** 订单发货：回传快递公司和运单号 */
    void sendDelivery(Credentials c, DeliveryRequest req);

    /** 按时间范围 [from, to) 查询售后单 ID（分页） */
    Page<String> listAftersaleIds(Credentials c, LocalDateTime from, LocalDateTime to, String nextKey);

    Aftersale getAftersale(Credentials c, String aftersaleId);

    /** 同意售后：仅退款即退款；退货退款为同意退货（使用店铺设置的退货地址） */
    void acceptAftersale(Credentials c, String aftersaleId);

    /** 拒绝售后 */
    void rejectAftersale(Credentials c, String aftersaleId, String reason);

    /** 退货退款：确认收到退货并退款 */
    void confirmReturnReceived(Credentials c, String aftersaleId);

    /** 店铺配置修改后清理接口调用凭证 */
    default void evictToken(Long shopId) {
    }

    // ---------------------------------------------------------------- 模型

    /**
     * 调用接口所需的店铺配置
     *
     * @param shopId          本系统的店铺 ID（模拟店铺按它区分，凭证缓存的 key）
     * @param returnAddressId 退货地址 ID（同意退货时使用），可为空
     */
    record Credentials(Long shopId, WxShopMode mode, String appId, String appSecret, String returnAddressId) {
    }

    /**
     * 分页结果
     *
     * @param nextKey 下一页的游标，没有更多时为空
     */
    record Page<T>(List<T> items, String nextKey, boolean hasMore, int total) {
    }

    /**
     * 商品
     *
     * @param status 商品状态码（线上原值，5 为上架，按官方文档，待联调确认）
     */
    record Product(String productId, String title, Integer status, List<Sku> skus) {
    }

    /**
     * SKU
     *
     * @param outSkuId 商家自定义 SKU 编码
     * @param skuCode  SKU 条码 / 编码
     * @param attrs    规格，如「颜色:红;尺码:L」
     * @param price    售价（元）
     * @param stock    线上库存
     */
    record Sku(String skuId, String outSkuId, String skuCode, String attrs, BigDecimal price, Long stock) {
    }

    /**
     * 订单
     *
     * @param status      线上订单状态码（见 {@link com.lemon.erp.modules.channel.model.WxOrderStatus#ofPlatform}）
     * @param buyer       买家标识（openid 等，入库前脱敏）
     * @param goodsAmount 商品总价（元）
     * @param discount    优惠金额（元）
     * @param paidAmount  实付金额（元，含运费）
     * @param shipment    线上的发货信息（在小店后台发货时有），没有时为 null
     */
    record Order(String orderId, int status, LocalDateTime createTime, LocalDateTime updateTime, LocalDateTime payTime,
                 String buyer, Receiver receiver, BigDecimal goodsAmount, BigDecimal freight, BigDecimal discount, BigDecimal paidAmount,
                 List<OrderLine> lines, Shipment shipment) {
    }

    /**
     * 订单商品
     *
     * @param salePrice  售价（元）
     * @param paidAmount 本行实付金额（元），接口不返回时为 null（按售价比例分摊订单实付）
     */
    record OrderLine(String productId, String skuId, String title, String attrs, String outSkuId, String skuCode, int qty,
                     BigDecimal salePrice, BigDecimal paidAmount) {
    }

    /** 收货信息（敏感：只在详情中按权限显示） */
    record Receiver(String name, String phone, String address) {
    }

    record Shipment(String companyId, String companyName, String waybillNo, LocalDateTime shipTime) {
    }

    record DeliveryCompany(String id, String name) {
    }

    record DeliveryRequest(String orderId, String companyId, String waybillNo, List<DeliveryItem> items) {
    }

    record DeliveryItem(String productId, String skuId, int qty) {
    }

    /**
     * 售后单
     *
     * @param type   线上售后类型（REFUND 仅退款 / RETURN 退货退款）
     * @param status 线上售后状态（见 {@link com.lemon.erp.modules.channel.model.WxAftersaleStatus#ofPlatform}）
     */
    record Aftersale(String aftersaleId, String orderId, String type, String status, String productId, String skuId, int qty,
                     BigDecimal refundAmount, String reason, LocalDateTime createTime, LocalDateTime updateTime) {
    }
}
