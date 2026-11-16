package com.lemon.erp.modules.pos.pay;

import com.lemon.erp.modules.pos.entity.PosPayConfig;
import com.lemon.erp.modules.pos.model.PayProviderType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 线下登记：没有接入服务商时使用。顾客用门店的收款码或其他设备付款，收款记录为处理中，收银员确认到账（可登记流水号）后为成功；
 * 退款由收银员在收款设备上办理，系统直接记为成功
 */
@Component
public class OfflinePayProvider implements PayProvider {

    @Override
    public PayProviderType type() {
        return PayProviderType.OFFLINE;
    }

    @Override
    public PayResult pay(PosPayConfig config, PayCommand command) {
        return PayResult.pending(null);
    }

    /** 没有远程交易，结果由收银员确认 */
    @Override
    public PayResult query(PosPayConfig config, String outTradeNo, BigDecimal amount) {
        return PayResult.pending(null);
    }

    @Override
    public PayResult refund(PosPayConfig config, String outTradeNo, String refundNo, BigDecimal amount, BigDecimal totalAmount) {
        return PayResult.success(null);
    }

    @Override
    public void close(PosPayConfig config, String outTradeNo) {
        // 没有远程交易
    }

    @Override
    public boolean verifyCallback(PosPayConfig config, Map<String, String> headers, String body) {
        return false;
    }
}
