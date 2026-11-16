package com.lemon.erp.modules.pos.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.pos.dto.PosRequests;
import com.lemon.erp.modules.pos.dto.PosViews;
import com.lemon.erp.modules.pos.entity.PosHeldSale;
import com.lemon.erp.modules.pos.entity.PosMember;
import com.lemon.erp.modules.pos.entity.PosPayment;
import com.lemon.erp.modules.pos.entity.PosSale;
import com.lemon.erp.modules.pos.entity.PosTopup;
import com.lemon.erp.modules.pos.service.PosCashierService;
import com.lemon.erp.modules.pos.service.PosHeldSaleService;
import com.lemon.erp.modules.pos.service.PosMemberService;
import com.lemon.erp.modules.pos.service.PosPayService;
import com.lemon.erp.modules.pos.service.PosSaleService;
import com.lemon.erp.modules.pos.service.PosTerminalService;
import com.lemon.erp.modules.pos.service.PosTopupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 收银台（前台收银）：上下文、扫码、试算、结算、退货、挂单取单、聚合支付、会员识别与快速登记。
 * 退货、作废产生的聚合支付退款在业务事务提交之后再向通道办理（外部调用不放在事务中）
 */
@RestController
@RequestMapping("/pos")
@RequiredArgsConstructor
public class PosCashierController {

    private final PosCashierService cashierService;
    private final PosSaleService saleService;
    private final PosHeldSaleService heldService;
    private final PosPayService payService;
    private final PosMemberService memberService;
    private final PosTerminalService terminalService;
    private final PosTopupService topupService;

    @GetMapping("/context")
    @PreAuthorize("@ss.hasPerm('pos:cashier:list')")
    public R<PosViews.Context> context(@RequestParam Long terminalId) {
        return R.ok(cashierService.context(terminalId));
    }

    /** 扫码 / 输入编码：物料 + 单位 + 价格 + 本收银台仓库的库存 */
    @GetMapping("/lookup")
    @PreAuthorize("@ss.hasPerm('pos:cashier:list')")
    public R<PosViews.Lookup> lookup(@RequestParam Long terminalId, @RequestParam String code, @RequestParam(required = false) Long memberId) {
        return R.ok(cashierService.lookup(terminalId, code, memberId));
    }

    @PostMapping("/sale/preview")
    @PreAuthorize("@ss.hasPerm('pos:cashier:list')")
    public R<PosViews.Preview> preview(@Valid @RequestBody PosRequests.Sale req) {
        return R.ok(saleService.preview(req));
    }

    @PostMapping("/sale/checkout")
    @PreAuthorize("@ss.hasPerm('pos:cashier:list')")
    @OperLog(title = "POS 收银", type = OperType.INSERT) // i18n-ignore
    public R<PosSale> checkout(@Valid @RequestBody PosRequests.Sale req) {
        PosSaleService.Result r = saleService.checkout(req);
        return R.ok(saleService.detail(r.saleId()));
    }

    /** 退货：原单退货需要 pos:sale:return，无原单退货需要 pos:sale:noReceiptReturn（服务中校验） */
    @PostMapping("/sale/return")
    @PreAuthorize("@ss.hasPerm('pos:cashier:list')")
    @OperLog(title = "POS 退货", type = OperType.INSERT) // i18n-ignore
    public R<PosSale> returnSale(@Valid @RequestBody PosRequests.Return req) {
        PosSaleService.Result r = saleService.returnSale(req);
        payService.processRefunds(r.refundIds());
        return R.ok(saleService.detail(r.saleId()));
    }

    @PostMapping("/sale/{id}/void")
    @PreAuthorize("@ss.hasPerm('pos:sale:void')")
    @OperLog(title = "POS 作废", type = OperType.UPDATE) // i18n-ignore
    public R<PosSale> voidSale(@PathVariable Long id, @Valid @RequestBody(required = false) PosRequests.VoidSale req) {
        PosSaleService.Result r = saleService.voidSale(id, req);
        payService.processRefunds(r.refundIds());
        return R.ok(saleService.detail(id));
    }

    /** 按小票号查找原单（原单退货） */
    @GetMapping("/sale/by-no")
    @PreAuthorize("@ss.hasAnyPerm('pos:cashier:list', 'pos:sale:list')")
    public R<PosSale> byNo(@RequestParam String saleNo) {
        return R.ok(saleService.findByNo(saleNo));
    }

    /** 收银台储值充值（pos:sale:topup，服务中校验），返回充值单 */
    @PostMapping("/cashier/topup")
    @PreAuthorize("@ss.hasPerm('pos:cashier:list')")
    @OperLog(title = "收银充值", type = OperType.INSERT) // i18n-ignore
    public R<PosTopup> topup(@Valid @RequestBody PosRequests.Topup req) {
        return R.ok(topupService.detail(topupService.topupAtPos(req)));
    }

    // ---------------------------------------------------------------- 挂单

    @PostMapping("/held")
    @PreAuthorize("@ss.hasPerm('pos:cashier:list')")
    public R<Long> hold(@Valid @RequestBody PosRequests.Held req) {
        return R.ok(heldService.hold(req));
    }

    @GetMapping("/held")
    @PreAuthorize("@ss.hasPerm('pos:cashier:list')")
    public R<List<PosHeldSale>> heldList(@RequestParam Long terminalId) {
        return R.ok(heldService.list(terminalId));
    }

    @PostMapping("/held/{id}/resume")
    @PreAuthorize("@ss.hasPerm('pos:cashier:list')")
    public R<PosHeldSale> resume(@PathVariable Long id, @RequestParam Long terminalId) {
        return R.ok(heldService.resume(id, terminalId));
    }

    @DeleteMapping("/held/{id}")
    @PreAuthorize("@ss.hasPerm('pos:cashier:list')")
    public R<Void> deleteHeld(@PathVariable Long id, @RequestParam Long terminalId) {
        heldService.delete(id, terminalId);
        return R.ok();
    }

    // ---------------------------------------------------------------- 聚合支付

    /** 微信 / 支付宝收款（结算前）：付款码或扫码，返回收款记录（成功 / 处理中 / 失败） */
    @PostMapping("/pay")
    @PreAuthorize("@ss.hasPerm('pos:cashier:list')")
    public R<PosPayment> pay(@Valid @RequestBody PosRequests.Pay req) {
        return R.ok(payService.pay(req));
    }

    @GetMapping("/pay/{outTradeNo}")
    @PreAuthorize("@ss.hasPerm('pos:cashier:list')")
    public R<PosPayment> query(@PathVariable String outTradeNo) {
        return R.ok(payService.query(outTradeNo));
    }

    /** 线下登记：确认已到账 */
    @PostMapping("/pay/{outTradeNo}/confirm")
    @PreAuthorize("@ss.hasPerm('pos:cashier:list')")
    public R<PosPayment> confirm(@PathVariable String outTradeNo, @Valid @RequestBody(required = false) PosRequests.PayConfirm req) {
        return R.ok(payService.confirmOffline(outTradeNo, req == null ? null : req.refNo()));
    }

    /** 撤销结算前的收款（未付款的关闭，已付款的原路退回） */
    @PostMapping("/pay/{outTradeNo}/cancel")
    @PreAuthorize("@ss.hasPerm('pos:cashier:list')")
    public R<PosPayment> cancel(@PathVariable String outTradeNo) {
        return R.ok(payService.cancel(outTradeNo));
    }

    /** 重试失败的聚合支付退款 */
    @PostMapping("/payment/{id}/retry-refund")
    @PreAuthorize("@ss.hasAnyPerm('pos:cashier:list', 'pos:sale:list')")
    public R<PosPayment> retryRefund(@PathVariable Long id) {
        return R.ok(payService.retryRefund(id));
    }

    // ---------------------------------------------------------------- 会员

    /** 按手机号或会员号识别会员 */
    @GetMapping("/member/identify")
    @PreAuthorize("@ss.hasAnyPerm('pos:cashier:list', 'pos:member:list')")
    public R<PosMember> identify(@RequestParam String code) {
        return R.ok(memberService.identify(code));
    }

    /** 收银时快速登记会员 */
    @PostMapping("/member/quick")
    @PreAuthorize("@ss.hasPerm('pos:cashier:list')")
    @OperLog(title = "会员登记", type = OperType.INSERT) // i18n-ignore
    public R<PosMember> quickRegister(@Valid @RequestBody PosRequests.QuickMember req) {
        return R.ok(memberService.quickRegister(req, terminalService.requireForCashier(req.terminalId())));
    }
}
