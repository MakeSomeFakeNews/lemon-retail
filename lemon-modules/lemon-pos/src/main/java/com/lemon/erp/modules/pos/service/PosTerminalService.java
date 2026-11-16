package com.lemon.erp.modules.pos.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.store.StoreBrief;
import com.lemon.erp.framework.store.StoreLookup;
import com.lemon.erp.modules.fin.service.FinAccountService;
import com.lemon.erp.modules.pos.dto.PosRequests;
import com.lemon.erp.modules.pos.entity.PosPayAccount;
import com.lemon.erp.modules.pos.entity.PosSale;
import com.lemon.erp.modules.pos.entity.PosShift;
import com.lemon.erp.modules.pos.entity.PosTerminal;
import com.lemon.erp.modules.pos.mapper.PosPayAccountMapper;
import com.lemon.erp.modules.pos.mapper.PosSaleMapper;
import com.lemon.erp.modules.pos.mapper.PosShiftMapper;
import com.lemon.erp.modules.pos.mapper.PosTerminalMapper;
import com.lemon.erp.modules.pos.model.PayMethod;
import com.lemon.erp.modules.pos.model.ReceiptPaper;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 收银台与门店收款账户。收银台属于一个门店：出库仓库为本门店的仓库（默认门店的默认仓库），现金账户为本门店或共用的结算账户；
 * 其他支付方式的结算账户在「门店收款账户」中设置，未设置时取门店的默认结算账户，再没有时取收银台的现金账户
 */
@Service
@RequiredArgsConstructor
public class PosTerminalService {

    private final PosTerminalMapper terminalMapper;
    private final PosPayAccountMapper payAccountMapper;
    private final PosShiftMapper shiftMapper;
    private final PosSaleMapper saleMapper;
    private final StoreLookup storeLookup;
    private final WmsWarehouseService warehouseService;
    private final FinAccountService accountService;
    private final DocNoService docNoService;

    @DataPermission(table = "pos_terminal", deptColumn = "", userColumn = "", storeColumn = "store_id")
    public PageResult<PosTerminal> page(long current, long size, String keyword, Long storeId, EnableStatus status) {
        Page<PosTerminal> page = Page.of(current, size);
        terminalMapper.selectPageList(page, keyword, storeId, status);
        return PageResult.of(page);
    }

    /** 可管辖门店中启用的收银台（收银台页面选择） */
    @DataPermission(table = "pos_terminal", deptColumn = "", userColumn = "", storeColumn = "store_id")
    public List<PosTerminal> options() {
        Page<PosTerminal> page = Page.of(1, 500);
        terminalMapper.selectPageList(page, null, null, EnableStatus.ENABLED);
        return page.getRecords();
    }

    public PosTerminal get(Long id) {
        PosTerminal t = id == null ? null : terminalMapper.selectDetail(id);
        if (t == null) {
            throw new BizException("pos.error.terminalNotFound");
        }
        return t;
    }

    /** 详情：需要可管辖收银台所属的门店 */
    public PosTerminal detail(Long id) {
        PosTerminal t = get(id);
        checkStore(t.getStoreId());
        return t;
    }

    /**
     * 收银使用的收银台：启用、可管辖其门店、门店可以开单
     */
    public PosTerminal requireForCashier(Long id) {
        PosTerminal t = get(id);
        if (t.getStatus() != EnableStatus.ENABLED) {
            throw new BizException("pos.error.terminalDisabled", t.getName());
        }
        checkStore(t.getStoreId());
        storeLookup.requireTrading(t.getStoreId());
        return t;
    }

    @Transactional
    public Long create(PosTerminal t) {
        t.setId(null);
        if (!StringUtils.hasText(t.getCode())) {
            t.setCode(docNoService.next(PosDocNos.TERMINAL));
        }
        checkCodeUnique(t.getCode(), null);
        fillAndCheck(t);
        terminalMapper.insert(t);
        return t.getId();
    }

    public void update(Long id, PosTerminal t) {
        PosTerminal old = detail(id);
        t.setId(id);
        if (!StringUtils.hasText(t.getCode())) {
            t.setCode(old.getCode());
        }
        checkCodeUnique(t.getCode(), id);
        // 有单据后不能改门店（单据的门店 = 收银台的门店）
        if (!Objects.equals(old.getStoreId(), t.getStoreId()) && hasBusiness(id)) {
            throw new BizException("pos.error.terminalStoreLocked", old.getName());
        }
        fillAndCheck(t);
        terminalMapper.updateById(t);
    }

    public void delete(List<Long> ids) {
        for (Long id : ids) {
            PosTerminal t = detail(id);
            if (hasBusiness(id)) {
                throw new BizException("pos.error.terminalInUse", t.getName());
            }
        }
        terminalMapper.deleteByIds(ids);
    }

    // ---------------------------------------------------------------- 门店收款账户

    /** 门店各支付方式的结算账户（未设置的不在结果中） */
    public Map<PayMethod, Long> payAccounts(Long storeId) {
        checkStore(storeId);
        Map<PayMethod, Long> map = new EnumMap<>(PayMethod.class);
        payAccountMapper.selectList(Wrappers.<PosPayAccount>lambdaQuery().eq(PosPayAccount::getStoreId, storeId))
                .forEach(a -> map.put(a.getPayMethod(), a.getAccountId()));
        return map;
    }

    /** 保存门店收款账户：账户为空的方式删除设置；现金不在此设置 */
    @Transactional
    public void savePayAccounts(PosRequests.PayAccounts req) {
        storeLookup.require(req.storeId());
        checkStore(req.storeId());
        Set<PayMethod> seen = new HashSet<>();
        payAccountMapper.delete(Wrappers.<PosPayAccount>lambdaQuery().eq(PosPayAccount::getStoreId, req.storeId()));
        for (PosRequests.PayAccountItem item : req.items()) {
            if (item.payMethod() == PayMethod.CASH || item.accountId() == null || !seen.add(item.payMethod())) {
                continue;
            }
            accountService.checkStoreAccount(req.storeId(), item.accountId());
            PosPayAccount a = new PosPayAccount();
            a.setStoreId(req.storeId());
            a.setPayMethod(item.payMethod());
            a.setAccountId(item.accountId());
            payAccountMapper.insert(a);
        }
    }

    /**
     * 收款记入的结算账户：现金为收银台的现金账户；其他方式取门店收款账户，未设置时为门店默认结算账户，再没有时为收银台的现金账户
     */
    public Long accountFor(PosTerminal terminal, PayMethod method) {
        if (method == PayMethod.CASH) {
            return terminal.getCashAccountId();
        }
        PosPayAccount a = payAccountMapper.selectOne(Wrappers.<PosPayAccount>lambdaQuery()
                .eq(PosPayAccount::getStoreId, terminal.getStoreId()).eq(PosPayAccount::getPayMethod, method));
        if (a != null) {
            return a.getAccountId();
        }
        Long storeDefault = storeLookup.find(terminal.getStoreId()).map(StoreBrief::defaultAccountId).orElse(null);
        return storeDefault != null ? storeDefault : terminal.getCashAccountId();
    }

    /** 锁定收银台（开班互斥） */
    public void lock(Long id) {
        terminalMapper.lockById(id);
    }

    // ---------------------------------------------------------------- 内部

    private void fillAndCheck(PosTerminal t) {
        StoreBrief store = storeLookup.require(t.getStoreId());
        checkStore(store.id());
        if (t.getWarehouseId() == null) {
            t.setWarehouseId(store.defaultWarehouseId());
        }
        if (t.getWarehouseId() == null) {
            throw new BizException("pos.error.terminalWarehouseRequired");
        }
        warehouseService.checkStoreWarehouse(store.id(), t.getWarehouseId());
        if (t.getCashAccountId() == null) {
            t.setCashAccountId(store.defaultAccountId());
        }
        if (t.getCashAccountId() == null) {
            throw new BizException("pos.error.terminalAccountRequired");
        }
        accountService.checkStoreAccount(store.id(), t.getCashAccountId());
        if (t.getReceiptPaper() == null) t.setReceiptPaper(ReceiptPaper.MM80);
        if (t.getStatus() == null) t.setStatus(EnableStatus.ENABLED);
        if (t.getSort() == null) t.setSort(0);
    }

    private boolean hasBusiness(Long terminalId) {
        return shiftMapper.exists(Wrappers.<PosShift>lambdaQuery().eq(PosShift::getTerminalId, terminalId))
                || saleMapper.exists(Wrappers.<PosSale>lambdaQuery().eq(PosSale::getTerminalId, terminalId));
    }

    private void checkCodeUnique(String code, Long id) {
        PosTerminal exist = terminalMapper.selectOne(Wrappers.<PosTerminal>lambdaQuery().eq(PosTerminal::getCode, code));
        if (exist != null && !Objects.equals(exist.getId(), id)) {
            throw new BizException("pos.error.terminalCodeExists", code);
        }
    }

    static void checkStore(Long storeId) {
        if (!SecurityUtils.getDataScope().canAccessStore(storeId)) {
            throw new BizException("framework.error.storeNotAccessible");
        }
    }
}
