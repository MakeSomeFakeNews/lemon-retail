package com.lemon.erp.modules.wms.event;

import com.lemon.erp.modules.wms.model.TransferPricing;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 调拨单确认（调出）后已生成调拨价、调拨金额，或调拨单被冲销（红字单据，金额为负数）时发布，与确认 / 冲销同一事务。
 * 配送模块据此为跨门店的差价调拨记录门店往来（调出门店应收调入门店的内部结算款）；平价调拨不产生往来
 *
 * @param docId          调拨单（冲销时为红字单据）
 * @param pricing        调拨价格（同价 / 差价调拨的依据），为空按同价调拨
 * @param costAmount     调出成本金额（红字为负数），可为空
 * @param transferAmount 调拨金额（内部结算价 × 数量，红字为负数），可为空
 * @param sourceNo       来源单号（配送单等业务单据生成的调拨单），可为空
 * @param reversedId     冲销时为被冲销的原调拨单 ID，否则为空
 */
public record TransferSettledEvent(Long docId, String docNo, LocalDate bizDate, Long warehouseId, Long toWarehouseId,
                                   TransferPricing pricing, BigDecimal costAmount, BigDecimal transferAmount,
                                   String sourceNo, Long reversedId) {

    /** 差价调拨（调拨价可以与调出成本价不同） */
    public boolean differential() {
        return pricing != null && pricing.differential();
    }
}
