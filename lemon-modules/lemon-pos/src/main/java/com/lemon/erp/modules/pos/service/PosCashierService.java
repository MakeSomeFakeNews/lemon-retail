package com.lemon.erp.modules.pos.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.rule.BizRules;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.store.StoreBrief;
import com.lemon.erp.framework.store.StoreLookup;
import com.lemon.erp.modules.pos.dto.PosViews;
import com.lemon.erp.modules.pos.entity.PosMember;
import com.lemon.erp.modules.pos.entity.PosMemberRule;
import com.lemon.erp.modules.pos.entity.PosTerminal;
import com.lemon.erp.modules.pos.model.PayMethod;
import com.lemon.erp.modules.pos.model.PosRounding;
import com.lemon.erp.modules.scm.entity.ScmPriceControl;
import com.lemon.erp.modules.scm.model.PriceControlMode;
import com.lemon.erp.modules.scm.service.ScmPriceControlService;
import com.lemon.erp.modules.wms.dto.BarcodeMatch;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import com.lemon.erp.modules.pos.mapper.PosTerminalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 收银台页面的读取接口：进入收银的上下文、扫码识别商品。扫码到出行要求 300ms 内，一次请求返回物料、单位、价格、库存：
 * 按条码 / 编码查物料（索引）、取价（方案价、特价各一次查询）、价格控制、本仓库库存，均为单行索引查询
 */
@Service
@RequiredArgsConstructor
public class PosCashierService {

    /** 收银台上按权限显示的操作 */
    private static final List<String> CASHIER_PERMS = List.of(PosCartCalculator.PRICE_PERM, PosCartCalculator.DISCOUNT_PERM,
            PosCartCalculator.DISCOUNT_ANY_PERM, PosCartCalculator.BELOW_MIN_PERM, PosSaleService.RETURN_PERM, PosSaleService.NO_RECEIPT_PERM,
            PosSaleService.VOID_PERM, "pos:member:add", PosTopupService.TOPUP_PERM, PosTenderService.REDEEM_PERM, PosTopupService.ADMIN_TOPUP_PERM);

    private final PosTerminalService terminalService;
    private final PosTerminalMapper terminalMapper;
    private final PosShiftService shiftService;
    private final PosPayService payService;
    private final PosPricingService pricingService;
    private final PosMemberService memberService;
    private final PosMemberRuleService ruleService;
    private final WmsMaterialService materialService;
    private final ScmPriceControlService priceControlService;
    private final StoreLookup storeLookup;
    private final PermissionService permissionService;
    private final BizRules bizRules;

    public PosViews.Context context(Long terminalId) {
        PosTerminal terminal = terminalService.requireForCashier(terminalId);
        PosMemberRule rule = ruleService.get();
        // 储值、积分抵现按规则是否启用
        List<PayMethod> methods = Arrays.stream(PayMethod.values()).filter(m -> m != PayMethod.STORED_VALUE || Boolean.TRUE.equals(rule.getStoredValueEnabled()))
                .filter(m -> m != PayMethod.POINTS || Boolean.TRUE.equals(rule.getRedeemEnabled())).toList();
        return new PosViews.Context(terminal, terminal.getStoreName(), shiftService.current(terminalId), payService.currentProvider(), methods,
                bizRules.decimal(PosBizRules.MAX_DISCOUNT_RATE), bizRules.option(PosBizRules.ROUNDING, PosRounding.class),
                CASHIER_PERMS.stream().filter(permissionService::hasPerm).toList(), rule, ruleService.tiers());
    }

    /**
     * 扫码 / 输入编码识别商品
     *
     * @param memberId 已识别的会员（带上时按会员价、会员特价取价）
     */
    public PosViews.Lookup lookup(Long terminalId, String code, Long memberId) {
        PosTerminal terminal = terminalService.requireForCashier(terminalId);
        BarcodeMatch match = materialService.byBarcode(code);
        if (match == null) {
            throw new BizException("pos.error.barcodeNotFound", code == null ? "" : code.trim());
        }
        WmsMaterial m = match.material();
        PosCartCalculator.checkSellable(m);
        StoreBrief store = storeLookup.require(terminal.getStoreId());
        PosMember member = memberId == null ? null : memberService.get(memberId);
        PosPricingService.Price p = pricingService.prices(store, member, Map.of(m.getId(), m), LocalDateTime.now()).get(m.getId());
        ScmPriceControl control = priceControlService.ofMaterials(List.of(m.getId())).stream().findFirst().orElse(null);
        return new PosViews.Lookup(m.getId(), m.getCode(), m.getName(), m.getSpec(), match.unit(), m.getUnit(), match.ratio(), m.getBarcode(),
                m.getImageUrl(), m.getBatchEnabled() != null && m.getBatchEnabled() == 1, m.getSerialEnabled() != null && m.getSerialEnabled() == 1,
                PosCartCalculator.unitPrice(p.listPrice(), match.ratio()), PosCartCalculator.unitPrice(p.price(), match.ratio()), p.source(),
                p.specialId(), p.limitQty(), control == null ? null : control.getMinSalePrice(),
                control != null && control.getControlMode() == PriceControlMode.BLOCK,
                terminalMapper.selectAvailableQty(terminal.getWarehouseId(), m.getId()));
    }
}
