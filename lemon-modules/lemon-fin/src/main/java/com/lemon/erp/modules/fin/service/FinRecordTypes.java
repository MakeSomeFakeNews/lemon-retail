package com.lemon.erp.modules.fin.service;

import com.lemon.erp.framework.record.RecordType;
import com.lemon.erp.framework.record.RecordTypeProvider;
import com.lemon.erp.modules.fin.mapper.FinInvoiceMapper;
import com.lemon.erp.modules.fin.mapper.FinVoucherMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 财务业务记录类型（附件、变更历史）：凭证（原始凭证、发票扫描件）、发票（版式文件）
 */
@Component
@RequiredArgsConstructor
public class FinRecordTypes implements RecordTypeProvider {

    private final FinVoucherMapper voucherMapper;
    private final FinInvoiceMapper invoiceMapper;

    @Override
    public List<RecordType> recordTypes() {
        return List.of(new RecordType("fin_voucher", "fin.label.voucher", "fin:voucher:list", "fin:voucher:edit",
                        null, voucherMapper::selectById),
                // 发票的版式文件（PDF / OFD）、XML
                new RecordType("fin_invoice", "fin.label.invoice", "fin:invoice:list", "fin:invoice:edit", null, invoiceMapper::selectById));
    }

}
