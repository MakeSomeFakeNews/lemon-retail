package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.data.DetailLines;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.workflow.ApprovalFlow;
import com.lemon.erp.framework.workflow.ApprovalSupport;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.modules.scm.dto.PriceAdjustRequest;
import com.lemon.erp.modules.scm.entity.ScmPriceAdjust;
import com.lemon.erp.modules.scm.entity.ScmPriceAdjustItem;
import com.lemon.erp.modules.scm.entity.ScmPriceAdjustScheme;
import com.lemon.erp.modules.scm.entity.ScmPriceHistory;
import com.lemon.erp.modules.scm.entity.ScmPriceScheme;
import com.lemon.erp.modules.scm.mapper.ScmPriceAdjustItemMapper;
import com.lemon.erp.modules.scm.mapper.ScmPriceAdjustMapper;
import com.lemon.erp.modules.scm.mapper.ScmPriceAdjustSchemeMapper;
import com.lemon.erp.modules.scm.mapper.ScmPriceHistoryMapper;
import com.lemon.erp.modules.scm.model.PriceAdjustStatus;
import com.lemon.erp.modules.scm.model.PriceAdjustTarget;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.model.PriceLevels;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import com.lemon.erp.modules.wms.service.WmsPriceLevelService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 调价单：调整总部零售价（物料的零售价，经仓库模块的商品价格维护写入）或一个 / 多个门店价格方案的方案价。
 * <p>草稿 → 提交（发布了「调价单审批」流程时审批中，否则直接审核）→ 待生效 → 已生效；生效时间为空或已到时审核后立即生效，
 * 否则由定时任务 {@link ScmPriceAdjustJob} 到时生效（每张调价单一个事务）。生效时按当时的价格记录调价历史（调整前 → 调整后）。
 * 待生效的调价单可以作废，审批中的可以撤回为草稿
 */
@Service
@RequiredArgsConstructor
public class ScmPriceAdjustService {

    public static final String PROCESS_KEY = "scm_price_adjust";
    private static final ApprovalFlow FLOW = new ApprovalFlow(PROCESS_KEY, "scm_price_adjust:");

    /** 调价历史单页上限 */
    private static final long HISTORY_MAX_SIZE = 200;

    private final ScmPriceAdjustMapper adjustMapper;
    private final ScmPriceAdjustItemMapper itemMapper;
    private final ScmPriceAdjustSchemeMapper adjustSchemeMapper;
    private final ScmPriceHistoryMapper historyMapper;
    private final ScmPriceSchemeService schemeService;
    private final WmsMaterialService materialService;
    private final WmsPriceLevelService priceLevelService;
    private final DocNoService docNoService;
    private final ApprovalSupport approvalSupport;

    public PageResult<ScmPriceAdjust> page(long current, long size, String keyword, PriceAdjustStatus status,
                                           PriceAdjustTarget target, Long schemeId) {
        Page<ScmPriceAdjust> page = Page.of(current, size);
        adjustMapper.selectPageList(page, StringUtils.hasText(keyword) ? keyword.trim() : null, status, target, schemeId);
        fillSchemes(page.getRecords());
        return PageResult.of(page);
    }

    public ScmPriceAdjust get(Long id) {
        ScmPriceAdjust a = id == null ? null : adjustMapper.selectById(id);
        if (a == null) {
            throw new BizException("scm.error.priceAdjustNotFound");
        }
        return a;
    }

    /** 详情：含明细、价格方案 */
    public ScmPriceAdjust detail(Long id) {
        ScmPriceAdjust a = adjustMapper.selectDetail(id);
        if (a == null) {
            throw new BizException("scm.error.priceAdjustNotFound");
        }
        a.setItems(adjustMapper.selectItems(id));
        a.setItemCount(a.getItems().size());
        fillSchemes(List.of(a));
        return a;
    }

    /**
     * 当前价格（录入调价单时对照显示）：schemeId 为空取总部零售价，否则取方案价（方案中没有的物料不返回）
     *
     * @return 物料 ID → { retailPrice, memberPrice }
     */
    public Map<Long, Map<String, BigDecimal>> currentPrices(Long schemeId, List<Long> materialIds) {
        Map<Long, Map<String, BigDecimal>> result = new HashMap<>();
        if (materialIds == null || materialIds.isEmpty()) {
            return result;
        }
        if (schemeId == null) {
            materialService.mapByIds(materialIds).forEach((id, m) -> result.put(id, prices(m.getRetailPrice(), null)));
        } else {
            schemeService.pricesOf(schemeId, materialIds).forEach((id, i) -> result.put(id, prices(i.getRetailPrice(), i.getMemberPrice())));
        }
        return result;
    }

    private static Map<String, BigDecimal> prices(BigDecimal retail, BigDecimal member) {
        Map<String, BigDecimal> m = new HashMap<>();
        m.put("retailPrice", retail);
        m.put("memberPrice", member);
        return m;
    }

    /** 调价历史：按物料（及方案、对象、调价单）筛选，按生效时间倒序 */
    public PageResult<ScmPriceHistory> history(long current, long size, Long materialId, Long schemeId, PriceAdjustTarget target, Long adjustId) {
        Page<ScmPriceHistory> page = Page.of(current, Math.min(size, HISTORY_MAX_SIZE));
        adjustMapper.selectHistory(page, materialId, schemeId, target, adjustId);
        return PageResult.of(page);
    }

    @Transactional
    public Long create(PriceAdjustRequest req) {
        Normalized n = normalize(req);
        ScmPriceAdjust a = new ScmPriceAdjust();
        a.setAdjustNo(docNoService.next(ScmDocNos.PRICE_ADJUST));
        a.setTarget(req.target());
        a.setEffectiveTime(req.effectiveTime());
        a.setReason(req.reason());
        a.setStatus(PriceAdjustStatus.DRAFT);
        adjustMapper.insert(a);
        saveLines(a.getId(), n);
        return a.getId();
    }

    @Transactional
    public void update(Long id, PriceAdjustRequest req) {
        ScmPriceAdjust a = get(id);
        checkDraft(a);
        Normalized n = normalize(req);
        StatusTransitions.change(adjustMapper, ScmPriceAdjust::getId, id, ScmPriceAdjust::getStatus,
                PriceAdjustStatus.DRAFT, PriceAdjustStatus.DRAFT, w -> w
                        .set(ScmPriceAdjust::getTarget, req.target())
                        .set(ScmPriceAdjust::getEffectiveTime, req.effectiveTime())
                        .set(ScmPriceAdjust::getReason, req.reason())
                        .set(ScmPriceAdjust::getUpdateTime, LocalDateTime.now()),
                "scm.error.priceAdjustOnlyDraftEditable");
        saveLines(id, n);
    }

    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            ScmPriceAdjust a = get(id);
            checkDraft(a);
            itemMapper.delete(Wrappers.<ScmPriceAdjustItem>lambdaQuery().eq(ScmPriceAdjustItem::getAdjustId, id));
            adjustSchemeMapper.delete(Wrappers.<ScmPriceAdjustScheme>lambdaQuery().eq(ScmPriceAdjustScheme::getAdjustId, id));
            adjustMapper.deleteById(id);
        }
    }

    /**
     * 提交：发布了调价单审批流程时发起审批，否则直接审核；审核后生效时间为空或已到的立即生效
     *
     * @return 提交后的状态
     */
    @Transactional
    public PriceAdjustStatus submit(Long id) {
        ScmPriceAdjust a = get(id);
        checkDraft(a);
        // 按当前的明细、方案再校验一次（方案可能已停用、物料已删除）
        List<ScmPriceAdjustItem> items = itemMapper.selectList(Wrappers.<ScmPriceAdjustItem>lambdaQuery()
                .eq(ScmPriceAdjustItem::getAdjustId, id));
        if (items.isEmpty()) {
            throw new BizException("scm.valid.priceAdjustItemsRequired");
        }
        List<Long> schemeIds = schemeIdsOf(id);
        checkTarget(a.getTarget(), schemeIds);
        approvalSupport.submit(FLOW, id, () -> Map.of("target", a.getTarget().name(), "itemCount", items.size(), "adjustNo", a.getAdjustNo()),
                () -> approve(id, PriceAdjustStatus.DRAFT),
                () -> StatusTransitions.change(adjustMapper, ScmPriceAdjust::getId, id, ScmPriceAdjust::getStatus,
                        PriceAdjustStatus.DRAFT, PriceAdjustStatus.APPROVING, "scm.error.priceAdjustStatusChanged"),
                instanceId -> adjustMapper.update(Wrappers.<ScmPriceAdjust>lambdaUpdate()
                        .set(ScmPriceAdjust::getFlowInstanceId, instanceId).eq(ScmPriceAdjust::getId, id)));
        return adjustMapper.selectById(id).getStatus();
    }

    /** 撤回审批中的调价单，回到草稿 */
    @Transactional
    public void withdraw(Long id) {
        ScmPriceAdjust a = get(id);
        if (a.getStatus() != PriceAdjustStatus.APPROVING) {
            throw new BizException("scm.error.priceAdjustNotApproving");
        }
        // 撤销流程会触发审批结束事件（回到草稿）；流程已不在运行时直接改回草稿
        approvalSupport.cancelIfRunning(a.getFlowInstanceId(), null);
        StatusTransitions.tryChange(adjustMapper, ScmPriceAdjust::getId, id, ScmPriceAdjust::getStatus,
                PriceAdjustStatus.APPROVING, PriceAdjustStatus.DRAFT);
    }

    /** 作废待生效的调价单 */
    @Transactional
    public void voidAdjust(Long id) {
        get(id);
        adjustMapper.selectStatusForUpdate(id);
        StatusTransitions.change(adjustMapper, ScmPriceAdjust::getId, id, ScmPriceAdjust::getStatus,
                PriceAdjustStatus.APPROVED, PriceAdjustStatus.VOIDED, w -> w.set(ScmPriceAdjust::getUpdateTime, LocalDateTime.now()),
                "scm.error.priceAdjustOnlyApprovedVoidable");
    }

    @EventListener
    public void onFlowFinished(FlowFinishedEvent e) {
        FLOW.onFinished(e, (id, approved) -> {
            if (approved) {
                approve(id, PriceAdjustStatus.APPROVING);
            } else {
                StatusTransitions.tryChange(adjustMapper, ScmPriceAdjust::getId, id, ScmPriceAdjust::getStatus,
                        PriceAdjustStatus.APPROVING, PriceAdjustStatus.DRAFT);
            }
        });
    }

    /** 到达生效时间的待生效调价单（定时任务逐张生效） */
    public List<Long> dueIds(LocalDateTime now) {
        return adjustMapper.selectDueIds(PriceAdjustStatus.APPROVED, now);
    }

    /**
     * 待生效的调价单生效（定时任务在每张调价单自己的事务中调用）
     *
     * @return 是否生效（状态已变化、未到生效时间时返回 false）
     */
    @Transactional
    public boolean applyDue(Long id, LocalDateTime now) {
        ScmPriceAdjust a = adjustMapper.selectById(id);
        if (a == null || a.getEffectiveTime() != null && a.getEffectiveTime().isAfter(now)) {
            return false;
        }
        return apply(id);
    }

    /** 审核：改为待生效，生效时间为空或已到时立即生效 */
    private void approve(Long id, PriceAdjustStatus from) {
        if (!StatusTransitions.tryChange(adjustMapper, ScmPriceAdjust::getId, id, ScmPriceAdjust::getStatus, from, PriceAdjustStatus.APPROVED,
                w -> w.set(ScmPriceAdjust::getApproveTime, LocalDateTime.now()))) {
            throw new BizException("scm.error.priceAdjustStatusChanged");
        }
        ScmPriceAdjust a = adjustMapper.selectById(id);
        if (a.getEffectiveTime() == null || !a.getEffectiveTime().isAfter(LocalDateTime.now())) {
            apply(id);
        }
    }

    /** 生效：写入新价格、记录调价历史；待生效 → 已生效 */
    private boolean apply(Long id) {
        // 行锁：与作废串行
        adjustMapper.selectStatusForUpdate(id);
        LocalDateTime now = LocalDateTime.now();
        if (!StatusTransitions.tryChange(adjustMapper, ScmPriceAdjust::getId, id, ScmPriceAdjust::getStatus,
                PriceAdjustStatus.APPROVED, PriceAdjustStatus.EFFECTIVE, w -> w.set(ScmPriceAdjust::getEffectTime, now))) {
            return false;
        }
        ScmPriceAdjust a = adjustMapper.selectById(id);
        List<ScmPriceAdjustItem> items = itemMapper.selectList(Wrappers.<ScmPriceAdjustItem>lambdaQuery()
                .eq(ScmPriceAdjustItem::getAdjustId, id).orderByAsc(ScmPriceAdjustItem::getId));
        List<ScmPriceHistory> history = new ArrayList<>();
        if (a.getTarget() == PriceAdjustTarget.HQ) {
            history.addAll(applyHq(items));
        } else {
            for (Long schemeId : schemeIdsOf(id)) {
                history.addAll(schemeService.applyAdjust(schemeId, items));
            }
        }
        history.forEach(h -> {
            h.setAdjustId(id);
            h.setEffectTime(now);
        });
        if (!history.isEmpty()) {
            historyMapper.insertBatch(history);
        }
        return true;
    }

    /** 总部价：经仓库模块的商品价格维护修改物料的零售价 */
    private List<ScmPriceHistory> applyHq(List<ScmPriceAdjustItem> items) {
        Map<Long, WmsMaterial> materials = materialService.mapByIds(items.stream().map(ScmPriceAdjustItem::getMaterialId).toList());
        List<Map<String, Object>> rows = new ArrayList<>();
        List<ScmPriceHistory> history = new ArrayList<>();
        for (ScmPriceAdjustItem item : items) {
            WmsMaterial m = materials.get(item.getMaterialId());
            if (m == null || item.getRetailPrice() == null) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("id", m.getId());
            row.put(PriceLevels.FIELDS.get(PriceLevels.RETAIL), item.getRetailPrice());
            rows.add(row);
            ScmPriceHistory h = new ScmPriceHistory();
            h.setMaterialId(m.getId());
            h.setTarget(PriceAdjustTarget.HQ);
            h.setOldRetailPrice(m.getRetailPrice());
            h.setNewRetailPrice(item.getRetailPrice());
            history.add(h);
        }
        materialService.savePrices(rows);
        return history;
    }

    // ---------------------------------------------------------------- 内部

    private record Normalized(List<Long> schemeIds, List<ScmPriceAdjustItem> items) {
    }

    private Normalized normalize(PriceAdjustRequest req) {
        List<Long> schemeIds = req.target() == PriceAdjustTarget.SCHEME && req.schemeIds() != null
                ? new ArrayList<>(new LinkedHashSet<>(req.schemeIds())) : List.of();
        checkTarget(req.target(), schemeIds);
        boolean hq = req.target() == PriceAdjustTarget.HQ;
        Map<Long, WmsMaterial> materials = materialService.mapByIds(req.items().stream().map(PriceAdjustRequest.Line::materialId).toList());
        Set<Long> seen = new HashSet<>();
        List<ScmPriceAdjustItem> items = new ArrayList<>(req.items().size());
        for (int i = 0; i < req.items().size(); i++) {
            PriceAdjustRequest.Line l = req.items().get(i);
            if (!materials.containsKey(l.materialId())) {
                throw new BizException("scm.error.priceLineMaterialNotFound", i + 1);
            }
            if (!seen.add(l.materialId())) {
                throw new BizException("scm.error.priceLineMaterialDuplicate", i + 1);
            }
            if (hq ? l.retailPrice() == null : l.retailPrice() == null && l.memberPrice() == null) {
                throw new BizException(hq ? "scm.error.priceAdjustHqRetailRequired" : "scm.error.priceLinePriceRequired", i + 1);
            }
            ScmPriceAdjustItem item = new ScmPriceAdjustItem();
            item.setMaterialId(l.materialId());
            item.setRetailPrice(ScmPriceSchemeService.price(l.retailPrice()));
            // 总部没有会员价列，只调整零售价
            item.setMemberPrice(hq ? null : ScmPriceSchemeService.price(l.memberPrice()));
            item.setRemark(StringUtils.hasText(l.remark()) ? l.remark().trim() : null);
            items.add(item);
        }
        return new Normalized(schemeIds, items);
    }

    /** 调价对象：总部价要求零售价列启用；价格方案至少一个，且都存在、启用 */
    private void checkTarget(PriceAdjustTarget target, List<Long> schemeIds) {
        if (target == PriceAdjustTarget.HQ) {
            if (!priceLevelService.enabledLevels().contains(PriceLevels.RETAIL)) {
                throw new BizException("scm.error.priceAdjustRetailLevelDisabled");
            }
            return;
        }
        if (schemeIds.isEmpty()) {
            throw new BizException("scm.valid.priceAdjustSchemeRequired");
        }
        schemeIds.forEach(schemeService::requireEnabled);
    }

    private void saveLines(Long id, Normalized n) {
        DetailLines.replace(itemMapper, ScmPriceAdjustItem::getAdjustId, id, n.items(), ScmPriceAdjustItem::setAdjustId,
                itemMapper::insertBatch);
        List<ScmPriceAdjustScheme> schemes = n.schemeIds().stream().map(sid -> {
            ScmPriceAdjustScheme s = new ScmPriceAdjustScheme();
            s.setSchemeId(sid);
            return s;
        }).toList();
        DetailLines.replace(adjustSchemeMapper, ScmPriceAdjustScheme::getAdjustId, id, schemes, ScmPriceAdjustScheme::setAdjustId,
                adjustSchemeMapper::insertBatch);
    }

    private List<Long> schemeIdsOf(Long adjustId) {
        return adjustSchemeMapper.selectList(Wrappers.<ScmPriceAdjustScheme>lambdaQuery()
                        .eq(ScmPriceAdjustScheme::getAdjustId, adjustId).orderByAsc(ScmPriceAdjustScheme::getId))
                .stream().map(ScmPriceAdjustScheme::getSchemeId).toList();
    }

    /** 填充价格方案 ID、名称（一次查询全部调价单的方案） */
    private void fillSchemes(Collection<ScmPriceAdjust> list) {
        if (list.isEmpty()) {
            return;
        }
        List<ScmPriceAdjustScheme> rows = adjustSchemeMapper.selectList(Wrappers.<ScmPriceAdjustScheme>lambdaQuery()
                .in(ScmPriceAdjustScheme::getAdjustId, list.stream().map(ScmPriceAdjust::getId).toList())
                .orderByAsc(ScmPriceAdjustScheme::getId));
        Map<Long, ScmPriceScheme> schemes = schemeService.mapByIds(rows.stream().map(ScmPriceAdjustScheme::getSchemeId).collect(Collectors.toSet()));
        Map<Long, List<Long>> byAdjust = rows.stream().collect(Collectors.groupingBy(ScmPriceAdjustScheme::getAdjustId,
                Collectors.mapping(ScmPriceAdjustScheme::getSchemeId, Collectors.toList())));
        for (ScmPriceAdjust a : list) {
            List<Long> ids = byAdjust.getOrDefault(a.getId(), List.of());
            a.setSchemeIds(ids);
            a.setSchemeNames(ids.stream().map(schemes::get).filter(s -> s != null).map(ScmPriceScheme::getName).toList());
        }
    }

    private static void checkDraft(ScmPriceAdjust a) {
        if (a.getStatus() != PriceAdjustStatus.DRAFT) {
            throw new BizException("scm.error.priceAdjustOnlyDraftEditable");
        }
    }
}
