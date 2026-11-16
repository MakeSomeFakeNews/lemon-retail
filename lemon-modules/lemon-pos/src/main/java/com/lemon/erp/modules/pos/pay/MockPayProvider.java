package com.lemon.erp.modules.pos.pay;

import com.lemon.erp.modules.pos.entity.PosPayConfig;
import com.lemon.erp.modules.pos.model.PayProviderType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * 模拟通道（只用于测试，lemon.pos.mock-pay=true 时加载）：按金额的分位确定结果，便于测试各种情况——
 * 尾数 .44 失败；尾数 .55 处理中（查询时成功）；尾数 .66 退款失败；其他成功
 */
@Component
@ConditionalOnProperty(name = "lemon.pos.mock-pay", havingValue = "true")
public class MockPayProvider implements PayProvider {

    private static final int FAIL = 44;
    private static final int PENDING = 55;
    private static final int REFUND_FAIL = 66;

    @Override
    public PayProviderType type() {
        return PayProviderType.MOCK;
    }

    @Override
    public PayResult pay(PosPayConfig config, PayCommand command) {
        return switch (cents(command.amount())) {
            case FAIL -> PayResult.failed("MOCK: insufficient balance"); // i18n-ignore 模拟通道返回的原因
            case PENDING -> PayResult.pending(command.authCode() == null ? "mock://qr/" + command.outTradeNo() : null);
            default -> PayResult.success("MOCK" + command.outTradeNo());
        };
    }

    @Override
    public PayResult query(PosPayConfig config, String outTradeNo, BigDecimal amount) {
        return cents(amount) == FAIL ? PayResult.failed("MOCK: insufficient balance") : PayResult.success("MOCK" + outTradeNo); // i18n-ignore
    }

    @Override
    public PayResult refund(PosPayConfig config, String outTradeNo, String refundNo, BigDecimal amount, BigDecimal totalAmount) {
        return cents(totalAmount) == REFUND_FAIL ? PayResult.failed("MOCK: refund rejected") : PayResult.success("MOCKR" + refundNo); // i18n-ignore
    }

    @Override
    public void close(PosPayConfig config, String outTradeNo) {
        // 模拟通道没有远程交易
    }

    @Override
    public boolean verifyCallback(PosPayConfig config, Map<String, String> headers, String body) {
        return "mock-signature".equals(headers.get("X-Mock-Signature"));
    }

    private static int cents(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).remainder(BigDecimal.ONE).movePointRight(2).abs().intValue();
    }
}
