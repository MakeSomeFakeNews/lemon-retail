package com.lemon.erp.modules.pos.dto;

import com.lemon.erp.modules.pos.entity.PosMemberRule;
import com.lemon.erp.modules.pos.model.PayMethod;
import com.lemon.erp.modules.pos.model.PayProviderType;
import com.lemon.erp.modules.pos.model.PromotionKind;
import com.lemon.erp.modules.pos.model.PromotionRange;
import com.lemon.erp.modules.pos.model.PromotionThreshold;
import com.lemon.erp.modules.pos.model.SaleChannel;
import com.lemon.erp.modules.pos.model.SpecialScope;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 零售前台的请求体
 */
public final class PosRequests {

    private PosRequests() {
    }

    /** 门店收款账户：支付方式 → 结算账户（现金取收银台的现金账户，不在此设置） */
    public record PayAccounts(@NotNull(message = "{pos.valid.storeRequired}") Long storeId,
                              @NotNull(message = "{pos.valid.payAccountsRequired}") List<@Valid PayAccountItem> items) {
    }

    public record PayAccountItem(@NotNull(message = "{pos.valid.payMethodRequired}") PayMethod payMethod, Long accountId) {
    }

    /** 开班：备用金 */
    public record ShiftOpen(@NotNull(message = "{pos.valid.terminalRequired}") Long terminalId,
                            @NotNull(message = "{pos.valid.openingCashRequired}")
                            @DecimalMin(value = "0", message = "{pos.valid.amountNonNegative}")
                            @Digits(integer = 14, fraction = 2, message = "{pos.valid.amountScale}") BigDecimal openingCash) {
    }

    /** 交班：实点现金 */
    public record ShiftClose(@NotNull(message = "{pos.valid.countedCashRequired}")
                             @DecimalMin(value = "0", message = "{pos.valid.amountNonNegative}")
                             @Digits(integer = 14, fraction = 2, message = "{pos.valid.amountScale}") BigDecimal countedCash,
                             @Size(max = 255, message = "{pos.valid.remarkTooLong}") String remark) {
    }

    /**
     * 收银明细行
     *
     * @param unit         录入单位，为空为基本单位（扫包装条码时为辅助单位）
     * @param qty          数量（录入单位）
     * @param price        手工改价（录入单位），为空按取价；与取价不同时需要改价权限
     * @param discountRate 单行折扣（%），为空或 100 不打折；需要折扣权限
     * @param serialNos    序列号（启用序列号管理的物料）
     */
    public record SaleLine(@NotNull(message = "{pos.valid.materialRequired}") Long materialId,
                           String unit,
                           @NotNull(message = "{pos.valid.qtyRequired}")
                           @DecimalMin(value = "0", inclusive = false, message = "{pos.valid.qtyPositive}")
                           @Digits(integer = 14, fraction = 4, message = "{pos.valid.qtyScale}") BigDecimal qty,
                           @DecimalMin(value = "0", message = "{pos.valid.amountNonNegative}")
                           @Digits(integer = 14, fraction = 4, message = "{pos.valid.priceScale}") BigDecimal price,
                           @DecimalMin(value = "0", message = "{pos.valid.discountRateRange}")
                           @DecimalMax(value = "100", message = "{pos.valid.discountRateRange}") BigDecimal discountRate,
                           @Size(max = 2000, message = "{pos.valid.serialTooLong}") String serialNos,
                           @Size(max = 255, message = "{pos.valid.remarkTooLong}") String remark) {
    }

    /**
     * 付款方式
     *
     * @param amount     金额：现金为顾客付款（找零 = 付款 - 现金应收），其他方式为本次收款金额
     * @param outTradeNo 微信、支付宝：已收款成功的商户订单号（先 POST /pos/pay 收款）
     * @param refNo      银行卡：刷卡凭证号（可空）
     */
    public record Tender(@NotNull(message = "{pos.valid.payMethodRequired}") PayMethod payMethod,
                         @NotNull(message = "{pos.valid.amountRequired}")
                         @DecimalMin(value = "0", message = "{pos.valid.amountNonNegative}")
                         @Digits(integer = 14, fraction = 2, message = "{pos.valid.amountScale}") BigDecimal amount,
                         @Size(max = 64, message = "{pos.valid.tradeNoTooLong}") String outTradeNo,
                         @Size(max = 64, message = "{pos.valid.refNoTooLong}") String refNo) {
    }

    /**
     * 收银结算 / 试算
     *
     * @param orderDiscount 整单优惠金额（需要折扣权限）
     * @param heldId        从挂单取出的，结算后删除挂单
     */
    public record Sale(@NotNull(message = "{pos.valid.terminalRequired}") Long terminalId,
                       Long memberId,
                       @NotEmpty(message = "{pos.valid.linesRequired}") @Size(max = 300, message = "{pos.valid.linesTooMany}") List<@Valid @NotNull SaleLine> lines,
                       @DecimalMin(value = "0", message = "{pos.valid.amountNonNegative}")
                       @Digits(integer = 14, fraction = 2, message = "{pos.valid.amountScale}") BigDecimal orderDiscount,
                       @Size(max = 10, message = "{pos.valid.tendersTooMany}") List<@Valid @NotNull Tender> tenders,
                       @Size(max = 255, message = "{pos.valid.remarkTooLong}") String remark,
                       Long heldId) {
    }

    /**
     * 退货行：原单退货填原销售行，无原单退货填物料、单价
     *
     * @param qty   退货数量（录入单位）
     * @param price 无原单退货的退款单价（录入单位），为空按当前取价
     */
    public record ReturnLine(Long origItemId,
                             Long materialId,
                             String unit,
                             @NotNull(message = "{pos.valid.qtyRequired}")
                             @DecimalMin(value = "0", inclusive = false, message = "{pos.valid.qtyPositive}")
                             @Digits(integer = 14, fraction = 4, message = "{pos.valid.qtyScale}") BigDecimal qty,
                             @DecimalMin(value = "0", message = "{pos.valid.amountNonNegative}")
                             @Digits(integer = 14, fraction = 4, message = "{pos.valid.priceScale}") BigDecimal price,
                             @Size(max = 2000, message = "{pos.valid.serialTooLong}") String serialNos) {
    }

    /**
     * 退货：原单退货（origSaleId）按原支付方式原路退款；无原单退货需要权限，按 refundMethod（现金 / 银行卡）退款
     */
    public record Return(@NotNull(message = "{pos.valid.terminalRequired}") Long terminalId,
                         Long origSaleId,
                         Long memberId,
                         @NotEmpty(message = "{pos.valid.linesRequired}") @Size(max = 300, message = "{pos.valid.linesTooMany}") List<@Valid @NotNull ReturnLine> lines,
                         PayMethod refundMethod,
                         @Size(max = 255, message = "{pos.valid.remarkTooLong}") String remark) {
    }

    public record VoidSale(@Size(max = 255, message = "{pos.valid.remarkTooLong}") String reason) {
    }

    /** 挂单：购物车内容由前端保存为 JSON，取单时原样返回 */
    public record Held(@NotNull(message = "{pos.valid.terminalRequired}") Long terminalId,
                       Long memberId,
                       @NotNull(message = "{pos.valid.amountRequired}") BigDecimal amount,
                       Integer itemCount,
                       @Size(max = 255, message = "{pos.valid.remarkTooLong}") String remark,
                       @NotBlank(message = "{pos.valid.linesRequired}") @Size(max = 200000, message = "{pos.valid.heldTooLarge}") String content) {
    }

    /**
     * 聚合支付收款：authCode 为顾客付款码（扫码枪扫顾客手机），为空时生成二维码由顾客扫码
     *
     * @param outTradeNo 商户订单号（幂等）：前端生成，网络超时后重试用同一个号码不会重复收款；为空时由后端生成
     */
    public record Pay(@NotNull(message = "{pos.valid.terminalRequired}") Long terminalId,
                      @NotNull(message = "{pos.valid.payMethodRequired}") PayMethod payMethod,
                      @NotNull(message = "{pos.valid.amountRequired}")
                      @DecimalMin(value = "0.01", message = "{pos.valid.amountPositive}")
                      @Digits(integer = 14, fraction = 2, message = "{pos.valid.amountScale}") BigDecimal amount,
                      @Size(max = 64, message = "{pos.valid.authCodeTooLong}") String authCode,
                      @Pattern(regexp = "^[A-Za-z0-9_-]{8,48}$", message = "{pos.valid.outTradeNoFormat}") String outTradeNo) {
    }

    /** 线下登记的收款：收银员确认已到账，登记参考号 */
    public record PayConfirm(@Size(max = 64, message = "{pos.valid.refNoTooLong}") String refNo) {
    }

    /** 收银时快速登记会员 */
    public record QuickMember(@NotNull(message = "{pos.valid.terminalRequired}") Long terminalId,
                              @NotBlank(message = "{pos.valid.phoneRequired}")
                              @Pattern(regexp = "^[0-9+-]{5,20}$", message = "{pos.valid.phoneFormat}") String phone,
                              @Size(max = 64, message = "{pos.valid.nameTooLong}") String name) {
    }

    /**
     * 特价活动
     *
     * @param targetIds 适用门店（STORES）或区域（REGIONS）
     */
    public record Special(@NotBlank(message = "{pos.valid.specialNameRequired}") @Size(max = 64, message = "{pos.valid.nameTooLong}") String name,
                          @NotNull(message = "{pos.valid.beginTimeRequired}") LocalDateTime beginTime,
                          @NotNull(message = "{pos.valid.endTimeRequired}") LocalDateTime endTime,
                          SaleChannel channel,
                          @NotNull(message = "{pos.valid.storeScopeRequired}") SpecialScope storeScope,
                          List<Long> targetIds,
                          Boolean memberOnly,
                          @Size(max = 255, message = "{pos.valid.remarkTooLong}") String remark,
                          @NotEmpty(message = "{pos.valid.linesRequired}") @Size(max = 1000, message = "{pos.valid.linesTooMany}") List<@Valid @NotNull SpecialItem> items) {
    }

    public record SpecialItem(@NotNull(message = "{pos.valid.materialRequired}") Long materialId,
                              @DecimalMin(value = "0", message = "{pos.valid.amountNonNegative}")
                              @Digits(integer = 14, fraction = 4, message = "{pos.valid.priceScale}") BigDecimal specialPrice,
                              @DecimalMin(value = "1", message = "{pos.valid.discountRateRange}")
                              @DecimalMax(value = "100", message = "{pos.valid.discountRateRange}") BigDecimal discountRate,
                              @DecimalMin(value = "0", inclusive = false, message = "{pos.valid.qtyPositive}")
                              @Digits(integer = 14, fraction = 4, message = "{pos.valid.qtyScale}") BigDecimal limitQty) {
    }

    /** 门店日结 */
    public record DailyClose(@NotNull(message = "{pos.valid.storeRequired}") Long storeId,
                             @NotNull(message = "{pos.valid.businessDateRequired}") LocalDate businessDate,
                             @Size(max = 255, message = "{pos.valid.remarkTooLong}") String remark) {
    }

    /** 支付通道设置：密钥为空时保持原值 */
    public record PayConfig(@NotNull(message = "{pos.valid.providerRequired}") PayProviderType provider,
                            @Size(max = 64, message = "{pos.valid.appIdTooLong}") String appId,
                            @Size(max = 64, message = "{pos.valid.merchantNoTooLong}") String merchantNo,
                            @Size(max = 512, message = "{pos.valid.secretTooLong}") String apiKey,
                            @Size(max = 2000, message = "{pos.valid.secretTooLong}") String apiSecret,
                            @Size(max = 255, message = "{pos.valid.notifyUrlTooLong}") String notifyUrl,
                            @Size(max = 255, message = "{pos.valid.remarkTooLong}") String remark) {
    }

    // ---------------------------------------------------------------- 会员营销（M4）

    /**
     * 积分与储值规则：规则、不积分的商品 / 分类、充值赠送档位（整体保存）
     */
    public record MemberRuleSave(@NotNull(message = "{pos.valid.ruleRequired}") @Valid PosMemberRule rule,
                                 @Size(max = 1000, message = "{pos.valid.linesTooMany}") List<@Valid @NotNull GoodsItem> excludes,
                                 @Size(max = 50, message = "{pos.valid.linesTooMany}") List<@Valid @NotNull TopupTier> tiers) {
    }

    /** 商品范围：商品（materialId）或分类编码（categoryCode，含下级分类）二选一 */
    public record GoodsItem(Long materialId, @Size(max = 32, message = "{pos.valid.codeFormat}") String categoryCode) {
    }

    /** 充值赠送档位：充值满 minAmount 送 bonusAmount */
    public record TopupTier(@NotNull(message = "{pos.valid.amountRequired}")
                            @DecimalMin(value = "0.01", message = "{pos.valid.amountPositive}")
                            @Digits(integer = 14, fraction = 2, message = "{pos.valid.amountScale}") BigDecimal minAmount,
                            @NotNull(message = "{pos.valid.amountRequired}")
                            @DecimalMin(value = "0", message = "{pos.valid.amountNonNegative}")
                            @Digits(integer = 14, fraction = 2, message = "{pos.valid.amountScale}") BigDecimal bonusAmount) {
    }

    /** 积分调整（pos:member:points）：points 增加为正、减少为负（整数） */
    public record PointsAdjust(@NotNull(message = "{pos.valid.memberRequired}") Long memberId,
                               @NotNull(message = "{pos.valid.pointsRequired}")
                               @Digits(integer = 12, fraction = 0, message = "{pos.valid.pointsInteger}") BigDecimal points,
                               @NotBlank(message = "{pos.valid.reasonRequired}") @Size(max = 255, message = "{pos.valid.remarkTooLong}") String reason) {
    }

    /**
     * 积分清零（pos:member:points）：清除 before 之前获得、至今还没有用掉的积分（先获得的先用），before 为空时清除全部积分
     */
    public record PointsClear(LocalDate before,
                              @NotBlank(message = "{pos.valid.reasonRequired}") @Size(max = 255, message = "{pos.valid.remarkTooLong}") String reason) {
    }

    /** 储值调整（pos:member:storedValue）：amount 增加为正、减少为负；门店为空时记总部 */
    public record StoredValueAdjust(@NotNull(message = "{pos.valid.memberRequired}") Long memberId,
                                    @NotNull(message = "{pos.valid.amountRequired}")
                                    @Digits(integer = 14, fraction = 2, message = "{pos.valid.amountScale}") BigDecimal amount,
                                    Long storeId,
                                    @NotBlank(message = "{pos.valid.reasonRequired}") @Size(max = 255, message = "{pos.valid.remarkTooLong}") String reason) {
    }

    /**
     * 收银台充值（pos:sale:topup）：付款方式同收银（不能用储值、积分），赠送金额为空时按档位，与档位不同时需要「后台充值」权限
     */
    public record Topup(@NotNull(message = "{pos.valid.terminalRequired}") Long terminalId,
                        @NotNull(message = "{pos.valid.memberRequired}") Long memberId,
                        @NotNull(message = "{pos.valid.amountRequired}")
                        @DecimalMin(value = "0.01", message = "{pos.valid.amountPositive}")
                        @Digits(integer = 14, fraction = 2, message = "{pos.valid.amountScale}") BigDecimal amount,
                        @DecimalMin(value = "0", message = "{pos.valid.amountNonNegative}")
                        @Digits(integer = 14, fraction = 2, message = "{pos.valid.amountScale}") BigDecimal bonusAmount,
                        @NotEmpty(message = "{pos.valid.tendersRequired}") @Size(max = 10, message = "{pos.valid.tendersTooMany}") List<@Valid @NotNull Tender> tenders,
                        @Size(max = 255, message = "{pos.valid.remarkTooLong}") String remark) {
    }

    /**
     * 后台充值（pos:topup:add）：登记收款方式与结算账户（为空取门店默认结算账户），微信、支付宝为线下登记
     */
    public record AdminTopup(@NotNull(message = "{pos.valid.storeRequired}") Long storeId,
                             @NotNull(message = "{pos.valid.memberRequired}") Long memberId,
                             @NotNull(message = "{pos.valid.amountRequired}")
                             @DecimalMin(value = "0.01", message = "{pos.valid.amountPositive}")
                             @Digits(integer = 14, fraction = 2, message = "{pos.valid.amountScale}") BigDecimal amount,
                             @DecimalMin(value = "0", message = "{pos.valid.amountNonNegative}")
                             @Digits(integer = 14, fraction = 2, message = "{pos.valid.amountScale}") BigDecimal bonusAmount,
                             @NotNull(message = "{pos.valid.payMethodRequired}") PayMethod payMethod,
                             Long accountId,
                             @Size(max = 64, message = "{pos.valid.refNoTooLong}") String refNo,
                             @Size(max = 255, message = "{pos.valid.remarkTooLong}") String remark) {
    }

    /**
     * 满减 / 满折 / 满赠活动
     *
     * @param threshold    门槛：满 X 元（金额）或 X 件（录入数量）
     * @param reduceAmount 满减：减 Y 元
     * @param discountRate 满折：折扣（%）
     * @param stackable    每满叠加（满减、满赠）
     * @param goods        参与商品（指定商品）/ 分类（指定分类）
     * @param gifts        满赠的赠品
     */
    public record Promotion(@NotBlank(message = "{pos.valid.promotionNameRequired}") @Size(max = 64, message = "{pos.valid.nameTooLong}") String name,
                            @NotNull(message = "{pos.valid.promotionKindRequired}") PromotionKind kind,
                            PromotionThreshold thresholdType,
                            @NotNull(message = "{pos.valid.thresholdRequired}")
                            @DecimalMin(value = "0", inclusive = false, message = "{pos.valid.thresholdPositive}")
                            @Digits(integer = 14, fraction = 4, message = "{pos.valid.qtyScale}") BigDecimal threshold,
                            @DecimalMin(value = "0", inclusive = false, message = "{pos.valid.amountPositive}")
                            @Digits(integer = 14, fraction = 2, message = "{pos.valid.amountScale}") BigDecimal reduceAmount,
                            @DecimalMin(value = "1", message = "{pos.valid.discountRateRange}")
                            @DecimalMax(value = "99.99", message = "{pos.valid.discountRateRange}") BigDecimal discountRate,
                            Boolean stackable,
                            PromotionRange goodsScope,
                            Boolean excludeSpecial,
                            Integer priority,
                            @NotNull(message = "{pos.valid.beginTimeRequired}") LocalDateTime beginTime,
                            @NotNull(message = "{pos.valid.endTimeRequired}") LocalDateTime endTime,
                            SaleChannel channel,
                            @NotNull(message = "{pos.valid.storeScopeRequired}") SpecialScope storeScope,
                            List<Long> targetIds,
                            Boolean memberOnly,
                            @Size(max = 255, message = "{pos.valid.remarkTooLong}") String remark,
                            @Size(max = 1000, message = "{pos.valid.linesTooMany}") List<@Valid @NotNull GoodsItem> goods,
                            @Size(max = 20, message = "{pos.valid.linesTooMany}") List<@Valid @NotNull GiftItem> gifts) {
    }

    public record GiftItem(@NotNull(message = "{pos.valid.materialRequired}") Long materialId,
                           @NotNull(message = "{pos.valid.qtyRequired}")
                           @DecimalMin(value = "0", inclusive = false, message = "{pos.valid.qtyPositive}")
                           @Digits(integer = 14, fraction = 4, message = "{pos.valid.qtyScale}") BigDecimal qty) {
    }
}
