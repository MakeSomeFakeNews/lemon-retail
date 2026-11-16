package com.lemon.erp.modules.pos.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.rule.BizRules;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataPermissionRule;
import com.lemon.erp.framework.security.datascope.DataScopes;
import com.lemon.erp.framework.store.StoreBrief;
import com.lemon.erp.framework.store.StoreLookup;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.dto.PosRequests;
import com.lemon.erp.modules.pos.dto.PosViews;
import com.lemon.erp.modules.pos.entity.PosMember;
import com.lemon.erp.modules.pos.entity.PosMemberRule;
import com.lemon.erp.modules.pos.entity.PosPayment;
import com.lemon.erp.modules.pos.entity.PosSale;
import com.lemon.erp.modules.pos.entity.PosSaleItem;
import com.lemon.erp.modules.pos.entity.PosShift;
import com.lemon.erp.modules.pos.entity.PosTerminal;
import com.lemon.erp.modules.pos.mapper.PosPaymentMapper;
import com.lemon.erp.modules.pos.mapper.PosSaleItemMapper;
import com.lemon.erp.modules.pos.mapper.PosSaleMapper;
import com.lemon.erp.modules.pos.model.PayMethod;
import com.lemon.erp.modules.pos.model.PaymentStatus;
import com.lemon.erp.modules.pos.model.PointsType;
import com.lemon.erp.modules.pos.model.PosPriceSource;
import com.lemon.erp.modules.pos.model.SaleKind;
import com.lemon.erp.modules.pos.model.SaleStatus;
import com.lemon.erp.modules.pos.model.StoredValueType;
import com.lemon.erp.modules.scm.service.ScmBizRules;
import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import com.lemon.erp.modules.wms.dto.StockDocRequest;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.model.MovingAverage;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.model.StockInType;
import com.lemon.erp.modules.wms.model.StockOutType;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.StockCostService;
import com.lemon.erp.modules.wms.service.WmsStockDocService;
import com.lemon.erp.modules.wms.service.WmsStockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * POS 销售、退货、作废。结算（含退货、作废）在一个事务中完成：生成并确认仓库出入库单（零售出库 / 零售退货，实时成本：出库按当前均价，
 * 退货、作废按原零售出库的实际成本入库）、写入收款 / 退款记录、更新会员消费。散客零售不产生应收；收款按支付方式记入门店收款账户，
 * 门店日结时按结算账户汇总生成其他收入 / 费用支出单（见 {@link PosDailyCloseService}）。
 * <p>
 * 日期锁定：门店当天已日结时不能收银、退货、作废；作废只能作废当天的销售单。聚合支付的退款在事务提交后向通道办理（调用方负责）
 */
@Service
@RequiredArgsConstructor
public class PosSaleService {

    /** 单据的数据权限：收银员部门 + 收银员，且按门店 */
    static final DataPermissionRule RULE = DataPermissionRule.withStore("pos_sale", "dept_id", "create_by");
    static final String RETURN_PERM = "pos:sale:return";
    static final String NO_RECEIPT_PERM = "pos:sale:noReceiptReturn";
    static final String VOID_PERM = "pos:sale:void";

    private final PosSaleMapper saleMapper;
    private final PosSaleItemMapper itemMapper;
    private final PosPaymentMapper paymentMapper;
    private final PosTerminalService terminalService;
    private final PosShiftService shiftService;
    private final PosDailyCloseService dailyCloseService;
    private final PosMemberService memberService;
    private final PosHeldSaleService heldService;
    private final PosCartCalculator calculator;
    private final PosTenderService tenderService;
    private final PosMemberRuleService ruleService;
    private final PosPointsService pointsService;
    private final PosStoredValueService storedValueService;
    private final StoreLookup storeLookup;
    private final WmsStockDocService stockDocService;
    private final WmsStockQueryService stockQueryService;
    private final StockCostService costService;
    private final DocNoService docNoService;
    private final PermissionService permissionService;
    private final BizRules bizRules;

    /** 结算 / 退货 / 作废的结果：refundIds 为需要在事务提交后向通道办理的退款 */
    public record Result(Long saleId, List<Long> refundIds) {
    }

    // ---------------------------------------------------------------- 查询

    @DataPermission(table = "pos_sale", deptColumn = "dept_id", userColumn = "create_by", storeColumn = "store_id")
    public PageResult<PosSale> page(long current, long size, PosQueries.Sale q) {
        Page<PosSale> page = Page.of(current, size);
        saleMapper.selectPageList(page, q);
        List<PosSale> sales = page.getRecords();
        if (!sales.isEmpty()) {
            Map<Long, String> methods = paymentMapper.selectList(Wrappers.<PosPayment>lambdaQuery()
                            .select(PosPayment::getSaleId, PosPayment::getPayMethod)
                            .in(PosPayment::getSaleId, sales.stream().map(PosSale::getId).toList())
                            .eq(PosPayment::getStatus, PaymentStatus.SUCCESS)).stream()
                    .collect(Collectors.groupingBy(PosPayment::getSaleId, Collectors.mapping(p -> p.getPayMethod().name(),
                            Collectors.collectingAndThen(Collectors.toCollection(java.util.LinkedHashSet::new), s -> String.join(",", s)))));
            boolean sensitive = permissionService.hasPerm(PosMemberService.SENSITIVE_PERM);
            sales.forEach(s -> {
                s.setPayMethods(methods.get(s.getId()));
                if (!sensitive) s.setMemberPhone(PosMemberService.maskPhone(s.getMemberPhone()));
            });
        }
        return PageResult.of(page);
    }

    /** 详情：明细、收款 / 退款；按数据权限 */
    public PosSale detail(Long id) {
        PosSale s = DataScopes.require(RULE, () -> id == null ? null : saleMapper.selectDetail(id), "pos.error.saleNotFound");
        return fill(s);
    }

    /** 按单号查找（原单退货扫小票） */
    public PosSale findByNo(String saleNo) {
        Long id = saleMapper.selectIdByNo(saleNo == null ? null : saleNo.trim());
        if (id == null) {
            throw new BizException("pos.error.saleNotFound");
        }
        return detail(id);
    }

    private PosSale fill(PosSale s) {
        s.setItems(itemMapper.selectBySaleId(s.getId()));
        s.setPayments(paymentMapper.selectBySaleId(s.getId()));
        if (!permissionService.hasPerm(PosMemberService.SENSITIVE_PERM)) {
            s.setMemberPhone(PosMemberService.maskPhone(s.getMemberPhone()));
        }
        return s;
    }

    // ---------------------------------------------------------------- 收银

    /** 试算：取价、优惠、抹零、最低售价（与结算的计算相同，不保存） */
    public PosViews.Preview preview(PosRequests.Sale req) {
        PosTerminal terminal = terminalService.requireForCashier(req.terminalId());
        PosMember member = req.memberId() == null ? null : memberService.requireActive(req.memberId());
        PosViews.Preview p = calculator.compute(storeLookup.require(terminal.getStoreId()), member, req.lines(), req.orderDiscount()).preview();
        if (member != null && !permissionService.hasPerm(PosMemberService.SENSITIVE_PERM)) {
            member.setPhone(PosMemberService.maskPhone(member.getPhone()));
        }
        return p;
    }

    /**
     * 结算：校验班次、日结锁定、付款（现金找零、聚合支付关联已收款的记录），生成零售出库单、收款记录，更新会员消费
     */
    @Transactional
    public Result checkout(PosRequests.Sale req) {
        PosTerminal terminal = terminalService.requireForCashier(req.terminalId());
        PosShift shift = shiftService.requireOwnOpen(terminal);
        LocalDate today = LocalDate.now();
        dailyCloseService.checkNotClosed(terminal.getStoreId(), today);
        StoreBrief store = storeLookup.require(terminal.getStoreId());
        PosMember member = req.memberId() == null ? null : memberService.requireActive(req.memberId());
        PosCartCalculator.Cart cart = calculator.compute(store, member, req.lines(), req.orderDiscount());
        PosViews.Preview pv = cart.preview();
        PosMemberRule rule = ruleService.get();
        PosTenderService.Tenders tenders = tenderService.validate(req.tenders(), pv.payable(), terminal, member, rule);

        String saleNo = docNoService.next(PosDocNos.SALE);
        BigDecimal taxRate = bizRules.decimal(ScmBizRules.DEFAULT_TAX_RATE);
        List<StockDocItemRequest> stockItems = allocateOut(terminal.getWarehouseId(), pv.lines(), cart.materials(), taxRate);
        WmsStockDoc stockDoc = stockDocService.createConfirmed(StockBizType.OUT,
                new StockDocRequest(StockOutType.RETAIL, terminal.getWarehouseId(), null, today, req.remark(), stockItems), saleNo);

        LoginUser user = SecurityUtils.getLoginUser();
        PosSale s = new PosSale();
        s.setSaleNo(saleNo);
        s.setKind(SaleKind.SALE);
        s.setStatus(SaleStatus.COMPLETED);
        s.setStoreId(terminal.getStoreId());
        s.setTerminalId(terminal.getId());
        s.setShiftId(shift.getId());
        s.setWarehouseId(terminal.getWarehouseId());
        s.setMemberId(member == null ? null : member.getId());
        s.setBusinessDate(today);
        s.setSaleTime(LocalDateTime.now());
        s.setTotalQty(pv.totalQty());
        s.setListAmount(pv.listAmount());
        s.setAmount(pv.amount());
        s.setOrderDiscount(pv.orderDiscount());
        s.setRoundOff(pv.roundOff());
        s.setPayableAmount(pv.payable());
        s.setPaidAmount(tenders.paid());
        s.setChangeAmount(tenders.change());
        s.setPromotionId(pv.promotionId());
        s.setPromoAmount(pv.promoAmount());
        BigDecimal earned = member == null ? BigDecimal.ZERO
                : PosPointsService.earned(rule, member, pointsBase(pv, tenders.pointsAmount(), cart.materials()));
        s.setPointsEarned(earned);
        s.setWmsDocId(stockDoc.getId());
        s.setWmsDocNo(stockDoc.getDocNo());
        s.setRemark(req.remark());
        s.setDeptId(user.deptId());
        saleMapper.insert(s);
        itemMapper.insertBatch(toItems(s.getId(), pv.lines(), taxRate));
        tenderService.save(s.getId(), null, s.getStoreId(), shift, terminal, tenders);
        if (member != null) {
            // 储值、积分抵现：条件扣减（余额 / 积分不足时整单回滚）；之后记入获得的积分
            if (tenders.storedValue().signum() > 0) {
                storedValueService.change(member.getId(), s.getStoreId(), StoredValueType.CONSUME, tenders.storedValue().negate(), today,
                        s.getId(), null, saleNo, null);
            }
            if (tenders.points().signum() > 0) {
                pointsService.change(member.getId(), s.getStoreId(), PointsType.REDEEM, tenders.points().negate(), s.getId(), saleNo, null, false);
            }
            if (earned.signum() > 0) {
                pointsService.change(member.getId(), s.getStoreId(), PointsType.EARN, earned, s.getId(), saleNo, null, true);
            }
            memberService.addStats(member.getId(), pv.payable(), 1);
        }
        if (req.heldId() != null) {
            heldService.deleteInStore(req.heldId(), terminal.getStoreId());
        }
        return new Result(s.getId(), List.of());
    }

    /**
     * 可积分金额：不积分商品、赠品以外各行的实收合计 × (应收 − 积分抵现) ÷ 应收（积分抵现的部分不再积分）
     */
    private BigDecimal pointsBase(PosViews.Preview pv, BigDecimal pointsAmount, Map<Long, WmsMaterial> materials) {
        if (pv.payable().signum() <= 0) {
            return BigDecimal.ZERO;
        }
        Set<Long> excluded = ruleService.excludedMaterials(materials.values());
        BigDecimal base = pv.lines().stream().filter(l -> !l.isGift() && !excluded.contains(l.getMaterialId()))
                .map(PosViews.PricedLine::getFinalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return base.multiply(pv.payable().subtract(pointsAmount)).divide(pv.payable(), 2, RoundingMode.DOWN);
    }

    // ---------------------------------------------------------------- 退货

    /**
     * 退货：原单退货（{@code pos:sale:return}）按原销售行退，可部分退，退款按原支付方式原路退回（聚合支付优先，其次银行卡、现金）；
     * 无原单退货（{@code pos:sale:noReceiptReturn}）按当前取价或填写的单价，退现金或银行卡。按原零售出库的实际成本入库（无原单按当前均价）
     */
    @Transactional
    public Result returnSale(PosRequests.Return req) {
        PosTerminal terminal = terminalService.requireForCashier(req.terminalId());
        PosShift shift = shiftService.requireOwnOpen(terminal);
        LocalDate today = LocalDate.now();
        dailyCloseService.checkNotClosed(terminal.getStoreId(), today);
        boolean withReceipt = req.origSaleId() != null;
        if (!permissionService.hasPerm(withReceipt ? RETURN_PERM : NO_RECEIPT_PERM)) {
            throw new BizException(withReceipt ? "pos.error.noReturnPerm" : "pos.error.noReceiptReturnPerm");
        }
        String returnNo = docNoService.next(PosDocNos.RETURN);
        LoginUser user = SecurityUtils.getLoginUser();
        PosSale r = new PosSale();
        r.setSaleNo(returnNo);
        r.setKind(SaleKind.RETURN);
        r.setStatus(SaleStatus.COMPLETED);
        r.setStoreId(terminal.getStoreId());
        r.setTerminalId(terminal.getId());
        r.setShiftId(shift.getId());
        r.setWarehouseId(terminal.getWarehouseId());
        r.setBusinessDate(today);
        r.setSaleTime(LocalDateTime.now());
        r.setRemark(req.remark());
        r.setDeptId(user.deptId());
        r.setOrderDiscount(BigDecimal.ZERO);
        r.setRoundOff(BigDecimal.ZERO);
        r.setChangeAmount(BigDecimal.ZERO);

        List<PosSaleItem> items;
        List<StockDocItemRequest> stockItems;
        PosSale orig = null;
        if (withReceipt) {
            orig = saleMapper.selectById(req.origSaleId());
            if (orig == null || orig.getKind() != SaleKind.SALE) {
                throw new BizException("pos.error.saleNotFound");
            }
            if (orig.getStatus() != SaleStatus.COMPLETED) {
                throw new BizException("pos.error.saleVoided", orig.getSaleNo());
            }
            if (!Objects.equals(orig.getStoreId(), terminal.getStoreId())) {
                throw new BizException("pos.error.returnOtherStore", orig.getSaleNo());
            }
            r.setOrigSaleId(orig.getId());
            r.setMemberId(orig.getMemberId());
            items = receiptReturnItems(orig, req.lines());
            stockItems = returnStockItems(orig, items, terminal.getWarehouseId());
        } else {
            if (req.memberId() != null) {
                r.setMemberId(memberService.requireActive(req.memberId()).getId());
            }
            items = noReceiptItems(terminal, req.lines());
            stockItems = items.stream().map(i -> new StockDocItemRequest(i.getMaterialId(), null, null, null, null, null, i.getQty(),
                    i.getSerialNos(), null, null, null, null)).toList();
        }
        BigDecimal refund = Decimals.sum(items, PosSaleItem::getFinalAmount);
        r.setTotalQty(Decimals.sum(items, PosSaleItem::getInputQty));
        r.setListAmount(items.stream().map(i -> i.getListPrice() == null ? BigDecimal.ZERO : i.getListPrice().multiply(i.getInputQty()))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP));
        r.setAmount(Decimals.sum(items, PosSaleItem::getAmount));
        r.setOrderDiscount(Decimals.sum(items, PosSaleItem::getOrderShare));
        r.setPayableAmount(refund);
        r.setPaidAmount(refund);

        WmsStockDoc stockDoc = stockDocService.createConfirmed(StockBizType.IN,
                new StockDocRequest(StockInType.RETAIL_RETURN, terminal.getWarehouseId(), null, today, req.remark(), stockItems), returnNo);
        r.setWmsDocId(stockDoc.getId());
        r.setWmsDocNo(stockDoc.getDocNo());
        saleMapper.insert(r);
        items.forEach(i -> i.setSaleId(r.getId()));
        itemMapper.insertBatch(items);

        List<Long> refundIds;
        if (withReceipt) {
            refundIds = refundToOriginal(r, orig, refund, shift, terminal);
        } else {
            PayMethod method = req.refundMethod() == null ? PayMethod.CASH : req.refundMethod();
            if (method != PayMethod.CASH && method != PayMethod.BANK_CARD) {
                throw new BizException("pos.error.noReceiptRefundMethod");
            }
            PosPayment p = newPayment(r, shift, method, terminalService.accountFor(terminal, method));
            p.setAmount(refund.negate());
            p.setStatus(PaymentStatus.SUCCESS);
            p.setPayTime(LocalDateTime.now());
            paymentMapper.insert(p);
            refundIds = List.of();
        }
        if (r.getMemberId() != null) {
            if (orig != null) {
                deductEarnedPoints(r, orig, refund);
            }
            memberService.addStats(r.getMemberId(), refund.negate(), 0);
        }
        return new Result(r.getId(), refundIds);
    }

    /**
     * 退货扣回原单获得的积分：按退款占原单应收的比例（四舍五入取整），原单全部退完时扣回剩余；会员已用掉时积分可为负
     */
    private void deductEarnedPoints(PosSale r, PosSale orig, BigDecimal refund) {
        BigDecimal earned = orig.getPointsEarned() == null ? BigDecimal.ZERO : orig.getPointsEarned();
        if (earned.signum() <= 0) {
            return;
        }
        BigDecimal left = earned.add(saleMapper.sumReturnedPoints(orig.getId()));
        BigDecimal x = saleMapper.fullyReturned(orig.getId()) || orig.getPayableAmount().signum() == 0 ? left
                : earned.multiply(refund).divide(orig.getPayableAmount(), 0, RoundingMode.HALF_UP).min(left);
        if (x.signum() <= 0) {
            return;
        }
        pointsService.change(r.getMemberId(), r.getStoreId(), PointsType.RETURN, x.negate(), r.getId(), r.getSaleNo(), null, true);
        saleMapper.update(Wrappers.<PosSale>lambdaUpdate().set(PosSale::getPointsEarned, x.negate()).eq(PosSale::getId, r.getId()));
    }

    /** 原单退货的明细：可退数量按原行（条件更新防止并发超退），退款 = 原实收按数量比例，最后一次退完时取剩余金额 */
    private List<PosSaleItem> receiptReturnItems(PosSale orig, List<PosRequests.ReturnLine> lines) {
        Map<Long, PosSaleItem> origItems = itemMapper.selectBySaleId(orig.getId()).stream()
                .collect(Collectors.toMap(PosSaleItem::getId, Function.identity()));
        List<PosSaleItem> items = new ArrayList<>();
        int lineNo = 1;
        for (PosRequests.ReturnLine l : lines) {
            PosSaleItem o = l.origItemId() == null ? null : origItems.get(l.origItemId());
            if (o == null) {
                throw new BizException("pos.error.returnLineInvalid", lineNo);
            }
            BigDecimal qty = l.qty().setScale(4, RoundingMode.HALF_UP);
            BigDecimal remaining = o.getInputQty().subtract(o.getReturnedQty());
            boolean last = qty.compareTo(remaining) == 0;
            BigDecimal refund = last ? o.getFinalAmount().subtract(o.getReturnedAmount())
                    : o.getFinalAmount().multiply(qty).divide(o.getInputQty(), 2, RoundingMode.HALF_UP);
            if (itemMapper.addReturned(o.getId(), qty, refund) == 0) {
                throw new BizException("pos.error.returnQtyExceeded", o.getMaterialCode(), o.getMaterialName(),
                        remaining.stripTrailingZeros().toPlainString());
            }
            BigDecimal amount = o.getAmount().multiply(qty).divide(o.getInputQty(), 2, RoundingMode.HALF_UP);
            PosSaleItem i = new PosSaleItem();
            i.setLineNo(lineNo++);
            i.setMaterialId(o.getMaterialId());
            i.setMaterialCode(o.getMaterialCode());
            i.setUnit(o.getUnit());
            i.setUnitRatio(o.getUnitRatio());
            i.setInputQty(qty);
            i.setQty(qty.multiply(o.getUnitRatio()).setScale(4, RoundingMode.HALF_UP));
            i.setListPrice(o.getListPrice());
            i.setPrice(o.getPrice());
            i.setPriceSource(o.getPriceSource());
            i.setSpecialId(o.getSpecialId());
            i.setDiscountRate(o.getDiscountRate());
            i.setSalePrice(o.getSalePrice());
            i.setAmount(amount);
            i.setPromoShare(BigDecimal.ZERO);
            i.setOrderShare(amount.subtract(refund));
            i.setFinalAmount(refund);
            i.setGift(o.getGift());
            i.setTaxRate(o.getTaxRate());
            i.setSerialNos(l.serialNos() != null ? l.serialNos() : last && o.getReturnedQty().signum() == 0 ? o.getSerialNos() : null);
            i.setOrigItemId(o.getId());
            items.add(i);
        }
        return items;
    }

    /** 无原单退货的明细：单价为填写的单价，没有填写时按当前取价（不含会员价、特价以外的手工优惠） */
    private List<PosSaleItem> noReceiptItems(PosTerminal terminal, List<PosRequests.ReturnLine> lines) {
        List<PosRequests.SaleLine> saleLines = new ArrayList<>();
        for (int n = 0; n < lines.size(); n++) {
            PosRequests.ReturnLine l = lines.get(n);
            if (l.materialId() == null) {
                throw new BizException("pos.error.returnLineInvalid", n + 1);
            }
            saleLines.add(new PosRequests.SaleLine(l.materialId(), l.unit(), l.qty(), null, null, l.serialNos(), null));
        }
        PosCartCalculator.Cart cart = calculatorForReturn(terminal, saleLines);
        BigDecimal taxRate = bizRules.decimal(ScmBizRules.DEFAULT_TAX_RATE);
        List<PosSaleItem> items = toItems(null, cart.preview().lines(), taxRate);
        for (int n = 0; n < items.size(); n++) {
            BigDecimal price = lines.get(n).price();
            if (price != null) {
                PosSaleItem i = items.get(n);
                BigDecimal amount = i.getInputQty().multiply(price).setScale(2, RoundingMode.HALF_UP);
                i.setPrice(price);
                i.setPriceSource(PosPriceSource.MANUAL);
                i.setSalePrice(price);
                i.setAmount(amount);
                i.setOrderShare(BigDecimal.ZERO);
                i.setFinalAmount(amount);
            }
        }
        return items;
    }

    /** 无原单退货按当前取价（不做折扣上限、最低售价校验之外的处理） */
    private PosCartCalculator.Cart calculatorForReturn(PosTerminal terminal, List<PosRequests.SaleLine> lines) {
        return calculator.compute(storeLookup.require(terminal.getStoreId()), null, lines, null);
    }

    /**
     * 退货入库的明细：按原零售出库的库位、批次入库，成本取原出库的实际成本单价
     */
    private List<StockDocItemRequest> returnStockItems(PosSale orig, List<PosSaleItem> items, Long warehouseId) {
        Map<Long, BigDecimal> costs = costService.docUnitCosts(StockBizType.OUT, List.of(orig.getWmsDocId()));
        Map<Long, List<Map<String, Object>>> outLines = saleMapper.selectStockLines(orig.getWmsDocId()).stream()
                .collect(Collectors.groupingBy(m -> ((Number) m.get("materialId")).longValue(), LinkedHashMap::new, Collectors.toList()));
        List<StockDocItemRequest> result = new ArrayList<>();
        for (PosSaleItem i : items) {
            BigDecimal left = i.getQty();
            BigDecimal cost = costs.get(i.getMaterialId());
            BigDecimal bizPrice = bizPrice(i.getFinalAmount(), i.getQty(), i.getTaxRate());
            List<Map<String, Object>> slots = outLines.getOrDefault(i.getMaterialId(), List.of());
            boolean serialsUsed = false;
            for (Map<String, Object> slot : slots) {
                if (left.signum() <= 0) {
                    break;
                }
                BigDecimal avail = new BigDecimal(slot.get("qty").toString());
                BigDecimal take = left.min(avail);
                if (take.signum() <= 0) {
                    continue;
                }
                slot.put("qty", avail.subtract(take));
                left = left.subtract(take);
                Long location = ((Number) slot.get("locationId")).longValue();
                result.add(new StockDocItemRequest(i.getMaterialId(), location == 0 ? null : location, null, (String) slot.get("batchNo"),
                        null, null, take, serialsUsed ? null : i.getSerialNos(), null, warehouseId, cost, bizPrice));
                serialsUsed = true;
            }
            if (left.signum() > 0) {
                result.add(new StockDocItemRequest(i.getMaterialId(), null, null, null, null, null, left,
                        serialsUsed ? null : i.getSerialNos(), null, warehouseId, cost, bizPrice));
            }
        }
        return result;
    }

    /**
     * 原路退款：按原单的收款依次退（聚合支付、银行卡、现金），每笔不超过其剩余可退金额；现金、银行卡立即成功，聚合支付为处理中，
     * 事务提交后向通道办理
     *
     * @return 需要向通道办理的退款记录
     */
    private List<Long> refundToOriginal(PosSale r, PosSale orig, BigDecimal total, PosShift shift, PosTerminal terminal) {
        List<PosPayment> pays = paymentMapper.selectBySaleId(orig.getId()).stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS && p.getAmount().signum() > 0)
                .sorted(Comparator.comparingInt(PosSaleService::refundOrder).thenComparing(PosPayment::getId))
                .toList();
        BigDecimal left = total;
        List<Long> pending = new ArrayList<>();
        for (PosPayment p : pays) {
            if (left.signum() <= 0) {
                break;
            }
            BigDecimal x = left.min(p.getAmount().subtract(p.getRefundedAmount()));
            if (x.signum() <= 0) {
                continue;
            }
            if (paymentMapper.addRefunded(p.getId(), x) == 0) {
                throw new BizException("pos.error.refundExceeded", orig.getSaleNo());
            }
            boolean full = x.compareTo(p.getAmount().subtract(p.getRefundedAmount())) == 0;
            left = left.subtract(x);
            PosPayment refund = newPayment(r, shift, p.getPayMethod(), p.getAccountId());
            refund.setAmount(x.negate());
            refund.setProvider(p.getProvider());
            refund.setOrigPaymentId(p.getId());
            if (p.getPayMethod() == PayMethod.POINTS) {
                // 退回抵现积分：按金额比例，退完时取剩余
                BigDecimal used = p.getPoints() == null ? BigDecimal.ZERO : p.getPoints();
                BigDecimal back = full ? used.subtract(paymentMapper.sumRefundedPoints(p.getId()))
                        : used.multiply(x).divide(p.getAmount(), 0, RoundingMode.HALF_UP);
                refund.setPoints(back.negate());
                pointsService.change(orig.getMemberId(), r.getStoreId(), PointsType.REFUND, back, r.getId(), r.getSaleNo(), null, true);
            } else if (p.getPayMethod() == PayMethod.STORED_VALUE) {
                storedValueService.change(orig.getMemberId(), r.getStoreId(), StoredValueType.REFUND, x, LocalDate.now(), r.getId(), null,
                        r.getSaleNo(), null);
            }
            boolean electronic = p.getPayMethod().electronic();
            refund.setStatus(electronic ? PaymentStatus.PENDING : PaymentStatus.SUCCESS);
            refund.setPayTime(electronic ? null : LocalDateTime.now());
            paymentMapper.insert(refund);
            if (electronic) {
                pending.add(refund.getId());
            }
        }
        if (left.signum() > 0) {
            // 原单收款已退完（理论上不会出现），差额退现金
            PosPayment cash = newPayment(r, shift, PayMethod.CASH, terminal.getCashAccountId());
            cash.setAmount(left.negate());
            cash.setStatus(PaymentStatus.SUCCESS);
            cash.setPayTime(LocalDateTime.now());
            paymentMapper.insert(cash);
        }
        return pending;
    }

    /** 原路退款的顺序：积分抵现 → 聚合支付 → 储值 → 银行卡 → 现金 */
    private static int refundOrder(PosPayment p) {
        return switch (p.getPayMethod()) {
            case POINTS -> 0;
            case WECHAT, ALIPAY -> 1;
            case STORED_VALUE -> 2;
            case BANK_CARD -> 3;
            case CASH -> 4;
        };
    }

    // ---------------------------------------------------------------- 作废

    /**
     * 作废当天的销售单（{@code pos:sale:void}）：门店当天未日结、没有退货；库存按原出库的实际成本退回，全部收款原路退回
     * （记入原收银台当班的班次），会员消费扣回
     */
    @Transactional
    public Result voidSale(Long id, PosRequests.VoidSale req) {
        PosSale s = DataScopes.require(RULE, () -> saleMapper.selectById(id), "pos.error.saleNotFound");
        if (s.getKind() != SaleKind.SALE || s.getStatus() != SaleStatus.COMPLETED) {
            throw new BizException("pos.error.saleNotVoidable", s.getSaleNo());
        }
        LocalDate today = LocalDate.now();
        if (!today.equals(s.getBusinessDate())) {
            throw new BizException("pos.error.voidOnlyToday", s.getSaleNo());
        }
        dailyCloseService.checkNotClosed(s.getStoreId(), s.getBusinessDate());
        if (saleMapper.existsReturn(s.getId())) {
            throw new BizException("pos.error.saleHasReturn", s.getSaleNo());
        }
        PosTerminal terminal = terminalService.get(s.getTerminalId());
        PosShift shift = shiftService.current(terminal.getId());
        if (shift == null) {
            throw new BizException("pos.error.voidNeedsShift", terminal.getName());
        }
        List<PosSaleItem> items = itemMapper.selectBySaleId(s.getId());
        List<StockDocItemRequest> stockItems = returnStockItems(s, items, s.getWarehouseId());
        WmsStockDoc doc = stockDocService.createConfirmed(StockBizType.IN,
                new StockDocRequest(StockInType.RETAIL_RETURN, s.getWarehouseId(), null, today,
                        Messages.get("pos.text.voidRemark", s.getSaleNo()), stockItems), s.getSaleNo());
        List<Long> pending = refundToOriginal(s, s, s.getPayableAmount(), shift, terminal);
        if (s.getMemberId() != null) {
            if (s.getPointsEarned() != null && s.getPointsEarned().signum() > 0) {
                pointsService.change(s.getMemberId(), s.getStoreId(), PointsType.RETURN, s.getPointsEarned().negate(), s.getId(), s.getSaleNo(),
                        Messages.get("pos.text.voidRemark", s.getSaleNo()), true);
            }
            memberService.addStats(s.getMemberId(), s.getPayableAmount().negate(), -1);
        }
        StatusTransitions.change(saleMapper, PosSale::getId, id, PosSale::getStatus, SaleStatus.COMPLETED, SaleStatus.VOIDED,
                w -> w.set(PosSale::getVoidBy, SecurityUtils.getUserId()).set(PosSale::getVoidTime, LocalDateTime.now())
                        .set(PosSale::getVoidReason, req == null ? null : req.reason()).set(PosSale::getVoidDocId, doc.getId()),
                "pos.error.saleNotVoidable", s.getSaleNo());
        return new Result(id, pending);
    }

    // ---------------------------------------------------------------- 内部

    private PosPayment newPayment(PosSale s, PosShift shift, PayMethod method, Long accountId) {
        return tenderService.newPayment(s.getId(), s.getStoreId(), shift, method, accountId);
    }

    private static List<PosSaleItem> toItems(Long saleId, List<PosViews.PricedLine> lines, BigDecimal taxRate) {
        return lines.stream().map(l -> {
            PosSaleItem i = new PosSaleItem();
            i.setSaleId(saleId);
            i.setLineNo(l.getLineNo());
            i.setMaterialId(l.getMaterialId());
            i.setMaterialCode(l.getMaterialCode());
            i.setUnit(l.getUnit());
            i.setUnitRatio(l.getUnitRatio());
            i.setInputQty(l.getQty());
            i.setQty(l.getBaseQty());
            i.setListPrice(l.getListPrice());
            i.setPrice(l.getPrice());
            i.setPriceSource(l.getPriceSource());
            i.setSpecialId(l.getSpecialId());
            i.setDiscountRate(l.getDiscountRate());
            i.setSalePrice(l.getSalePrice());
            i.setAmount(l.getAmount());
            i.setPromoShare(l.getPromoShare());
            i.setOrderShare(l.getOrderShare());
            i.setFinalAmount(l.getFinalAmount());
            i.setGift(l.isGift() ? 1 : 0);
            i.setTaxRate(taxRate);
            i.setSerialNos(l.getSerialNos());
            i.setRemark(l.getRemark());
            return i;
        }).toList();
    }

    /**
     * 零售出库的明细：按先到期先出（已过期批次除外）在收银台仓库的库存位置中分配库位、批次；库存不足时由仓库报错整单回滚。
     * 业务单价 = 实收（含税）/ (1 + 税率) / 基本单位数量
     */
    private List<StockDocItemRequest> allocateOut(Long warehouseId, List<PosViews.PricedLine> lines, Map<Long, WmsMaterial> materials,
                                                  BigDecimal taxRate) {
        Map<Long, List<Map<String, Object>>> candidates = new HashMap<>();
        List<StockDocItemRequest> result = new ArrayList<>();
        for (PosViews.PricedLine l : lines) {
            WmsMaterial m = materials.get(l.getMaterialId());
            BigDecimal bizPrice = bizPrice(l.getFinalAmount(), l.getBaseQty(), taxRate);
            List<Map<String, Object>> slots = candidates.computeIfAbsent(m.getId(), id -> new ArrayList<>(stockQueryService.pickCandidates(warehouseId, id)));
            boolean serial = m.getSerialEnabled() != null && m.getSerialEnabled() == 1;
            if (serial) {
                // 序列号物料：序列号所在的库位由仓库按库存位置校验；只有一个库位时带上它
                Long location = slots.stream().map(s -> ((Number) s.get("locationId")).longValue()).distinct().count() == 1
                        ? ((Number) slots.get(0).get("locationId")).longValue() : null;
                result.add(new StockDocItemRequest(m.getId(), location == null || location == 0 ? null : location, null,
                        slots.isEmpty() ? null : (String) slots.get(0).get("batchNo"), null, null, l.getBaseQty(), l.getSerialNos(), null,
                        null, null, bizPrice));
                continue;
            }
            BigDecimal left = l.getBaseQty();
            for (Map<String, Object> slot : slots) {
                if (left.signum() <= 0) {
                    break;
                }
                BigDecimal avail = new BigDecimal(slot.get("qty").toString());
                BigDecimal take = left.min(avail);
                if (take.signum() <= 0) {
                    continue;
                }
                slot.put("qty", avail.subtract(take));
                left = left.subtract(take);
                long location = ((Number) slot.get("locationId")).longValue();
                result.add(new StockDocItemRequest(m.getId(), location == 0 ? null : location, null, (String) slot.get("batchNo"), null, null,
                        take, null, null, null, null, bizPrice));
            }
            if (left.signum() > 0) {
                // 库存不足：由仓库按「库存不足」报错
                result.add(new StockDocItemRequest(m.getId(), null, null, null, null, null, left, null, null, null, null, bizPrice));
            }
        }
        return result;
    }

    /** 不含税业务单价（基本单位）= 含税金额 / (1 + 税率%) / 数量 */
    private static BigDecimal bizPrice(BigDecimal amount, BigDecimal qty, BigDecimal taxRate) {
        if (qty == null || qty.signum() == 0 || amount == null) {
            return null;
        }
        BigDecimal rate = taxRate == null ? BigDecimal.ZERO : taxRate;
        return amount.multiply(Decimals.HUNDRED).divide(Decimals.HUNDRED.add(rate), 6, RoundingMode.HALF_UP)
                .divide(qty, MovingAverage.PRICE_SCALE, RoundingMode.HALF_UP);
    }
}
