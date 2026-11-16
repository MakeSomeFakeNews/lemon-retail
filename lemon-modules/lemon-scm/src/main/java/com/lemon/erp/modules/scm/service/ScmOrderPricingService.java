package com.lemon.erp.modules.scm.service;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.scm.dto.OrderRequest;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;
import com.lemon.erp.modules.scm.model.OrderKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

import static com.lemon.erp.modules.scm.model.ScmConstants.amount;
import static com.lemon.erp.modules.scm.model.ScmConstants.tax;

/**
 * 订单价格：采购订单按采购合同价下单并校验合同占用，销售订单应用促销和返利抵扣，保存前按限价校验。
 * 在调用方（{@link ScmOrderService}）的事务中执行
 */
@Service
@RequiredArgsConstructor
public class ScmOrderPricingService {

    private final ScmPromotionService promotionService;
    private final ScmPriceControlService priceControl;
    private final ScmCustomerService customerService;
    private final ScmTaxRates taxRates;

    /** 应用促销，并按调整后的单价重算金额 */
    public ScmPromotionService.Applied promote(OrderRequest req, List<ScmOrderItem> items) {
        ScmPromotionService.Applied applied = promotionService.apply(customerService.get(req.partnerId()), req.orderDate(), items,
                taxRates.fallback(req.invoiceKind()));
        for (ScmOrderItem item : applied.items()) {
            item.setAmount(amount(item.getQty(), item.getPrice()));
            item.setTaxAmount(tax(item.getAmount(), item.getTaxRate()));
        }
        return applied;
    }

    /** 整单优惠（在促销之后）：按行金额分摊到各行，返回订单记录的优惠金额 */
    public static BigDecimal applyWholeDiscount(OrderRequest req, List<ScmOrderItem> items) {
        WholeDiscounts.apply(items, req.wholeDiscount());
        return Decimals.money(req.wholeDiscount());
    }

    /** 限价：控制方式为禁止保存的物料，销售单价不能低于最低销售价、采购单价不能高于最高采购价 */
    public void checkPriceLimit(OrderKind kind, List<ScmOrderItem> items, BigDecimal exchangeRate) {
        priceControl.check(kind, items, exchangeRate);
    }

    /** 订单保存后记录应用的促销（修改时先删除旧记录） */
    public void saveUsages(Long orderId, ScmPromotionService.Applied applied) {
        promotionService.saveUsages(orderId, applied.usages());
    }

    public void deleteUsages(Long orderId) {
        promotionService.deleteUsages(orderId);
    }
}
