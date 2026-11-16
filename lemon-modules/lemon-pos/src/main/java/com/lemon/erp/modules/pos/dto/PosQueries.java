package com.lemon.erp.modules.pos.dto;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.pos.model.DailyCloseStatus;
import com.lemon.erp.modules.pos.model.PosReportDimension;
import com.lemon.erp.modules.pos.model.SaleKind;
import com.lemon.erp.modules.pos.model.SaleStatus;
import com.lemon.erp.modules.pos.model.ShiftStatus;
import com.lemon.erp.modules.pos.model.SpecialStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 零售前台的查询条件（GET 参数绑定）
 */
public final class PosQueries {

    private PosQueries() {
    }

    /**
     * POS 单据 / 销售明细
     *
     * @param keyword 单号、会员手机号或会员号
     */
    public record Sale(String keyword, SaleKind kind, SaleStatus status, Long storeId, Long terminalId, Long cashierId, Long memberId,
                       Long materialId, Long shiftId,
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
    }

    public record Shift(Long storeId, Long terminalId, Long cashierId, ShiftStatus status,
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
    }

    /** @param keyword 姓名、手机号或会员号 */
    public record Member(String keyword, Long levelId, Long storeId, EnableStatus status) {
    }

    public record Special(String keyword, SpecialStatus status, Long materialId) {
    }

    public record DailyClose(Long storeId, DailyCloseStatus status,
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
    }

    /** 收银日报：按营业日期范围、门店 / 收银台 / 收银员筛选，按维度汇总 */
    public record CashierReport(PosReportDimension groupBy, Long storeId, Long terminalId, Long cashierId,
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
    }
}
