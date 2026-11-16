package com.lemon.erp.modules.pos.pay;

import com.lemon.erp.modules.pos.entity.PosPayConfig;
import com.lemon.erp.modules.pos.model.PayMethod;
import com.lemon.erp.modules.pos.model.PayProviderType;
import com.lemon.erp.modules.pos.model.PaymentStatus;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 聚合支付通道（可插拔）：每种通道一个 Bean，按租户的支付通道设置（{@code pos_pay_config.provider}）选用。
 * <p>
 * 调用方保证：所有方法都在数据库事务之外调用（外部 HTTP 调用不放进事务）；商户订单号 outTradeNo 是幂等键，
 * 同一订单号重复调用 {@link #pay} 时通道应返回同一笔交易的结果。接入真实服务商（如收钱吧、拉卡拉、微信 / 支付宝服务商）时：
 * 新增 {@link PayProviderType} 取值、实现本接口；异步通知的接口需要在 SecurityConfig 中放行，并按 outTradeNo 找到租户后
 * 用 {@link #verifyCallback} 验签，再按查询结果更新收款记录
 */
public interface PayProvider {

    PayProviderType type();

    /**
     * 收款：authCode 不为空时为付款码支付（收银员扫顾客手机），为空时为扫码支付（返回二维码由顾客扫）
     *
     * @return 成功 / 处理中（顾客输入密码、等待扫码或收银员确认）/ 失败
     */
    PayResult pay(PosPayConfig config, PayCommand command);

    /** 查询收款结果（处理中的交易轮询） */
    PayResult query(PosPayConfig config, String outTradeNo, BigDecimal amount);

    /**
     * 退款（原路退回）：refundNo 为退款单号（幂等键）
     *
     * @param totalAmount 原收款金额
     */
    PayResult refund(PosPayConfig config, String outTradeNo, String refundNo, BigDecimal amount, BigDecimal totalAmount);

    /** 关闭未付款的交易 */
    void close(PosPayConfig config, String outTradeNo);

    /** 校验异步通知的签名 */
    boolean verifyCallback(PosPayConfig config, Map<String, String> headers, String body);

    /**
     * @param outTradeNo 商户订单号
     * @param subject    订单标题（显示在顾客的支付凭证上）
     */
    record PayCommand(String outTradeNo, PayMethod payMethod, BigDecimal amount, String authCode, String subject) {
    }

    /**
     * @param tradeNo 通道交易号
     * @param qrCode  扫码支付的二维码内容
     * @param message 失败原因
     */
    record PayResult(PaymentStatus status, String tradeNo, String qrCode, String message) {

        public static PayResult success(String tradeNo) {
            return new PayResult(PaymentStatus.SUCCESS, tradeNo, null, null);
        }

        public static PayResult pending(String qrCode) {
            return new PayResult(PaymentStatus.PENDING, null, qrCode, null);
        }

        public static PayResult failed(String message) {
            return new PayResult(PaymentStatus.FAILED, null, null, message);
        }
    }
}
