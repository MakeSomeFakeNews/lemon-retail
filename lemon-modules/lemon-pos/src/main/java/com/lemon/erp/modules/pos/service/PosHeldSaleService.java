package com.lemon.erp.modules.pos.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.pos.dto.PosRequests;
import com.lemon.erp.modules.pos.entity.PosHeldSale;
import com.lemon.erp.modules.pos.entity.PosTerminal;
import com.lemon.erp.modules.pos.mapper.PosHeldSaleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 挂单 / 取单：购物车内容由前端序列化保存，同一门店的收银台都可以取单，取单时删除挂单
 */
@Service
@RequiredArgsConstructor
public class PosHeldSaleService {

    private final PosHeldSaleMapper heldMapper;
    private final PosTerminalService terminalService;

    public Long hold(PosRequests.Held req) {
        PosTerminal terminal = terminalService.requireForCashier(req.terminalId());
        PosHeldSale h = new PosHeldSale();
        h.setStoreId(terminal.getStoreId());
        h.setTerminalId(terminal.getId());
        h.setMemberId(req.memberId());
        h.setItemCount(req.itemCount() == null ? 0 : req.itemCount());
        h.setAmount(req.amount());
        h.setRemark(req.remark());
        h.setContent(req.content());
        heldMapper.insert(h);
        return h.getId();
    }

    /** 收银台所在门店的挂单（不含购物车内容） */
    public List<PosHeldSale> list(Long terminalId) {
        PosTerminal terminal = terminalService.requireForCashier(terminalId);
        return heldMapper.selectByStore(terminal.getStoreId());
    }

    /** 取单：返回购物车内容并删除挂单 */
    @Transactional
    public PosHeldSale resume(Long id, Long terminalId) {
        PosTerminal terminal = terminalService.requireForCashier(terminalId);
        PosHeldSale h = require(id, terminal.getStoreId());
        if (heldMapper.deleteById(id) == 0) {
            throw new BizException("pos.error.heldNotFound");
        }
        return h;
    }

    public void delete(Long id, Long terminalId) {
        PosTerminal terminal = terminalService.requireForCashier(terminalId);
        require(id, terminal.getStoreId());
        heldMapper.deleteById(id);
    }

    /** 结算后删除对应的挂单（已被取走的忽略） */
    void deleteInStore(Long id, Long storeId) {
        heldMapper.delete(Wrappers.<PosHeldSale>lambdaQuery().eq(PosHeldSale::getId, id).eq(PosHeldSale::getStoreId, storeId));
    }

    private PosHeldSale require(Long id, Long storeId) {
        PosHeldSale h = id == null ? null : heldMapper.selectById(id);
        if (h == null || !Objects.equals(h.getStoreId(), storeId)) {
            throw new BizException("pos.error.heldNotFound");
        }
        return h;
    }
}
