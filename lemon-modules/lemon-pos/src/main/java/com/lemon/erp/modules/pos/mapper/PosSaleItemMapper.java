package com.lemon.erp.modules.pos.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.pos.entity.PosSaleItem;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PosSaleItemMapper extends BaseMapper<PosSaleItem> {

    void insertBatch(@Param("list") List<PosSaleItem> items);

    /** 明细（含物料编码、名称、规格、条码、特价活动名称），按行号 */
    List<PosSaleItem> selectBySaleId(@Param("saleId") Long saleId);

    /** 原单退货：累加已退数量和金额，可退数量不足时不更新（返回 0） */
    int addReturned(@Param("id") Long id, @Param("qty") BigDecimal qty, @Param("amount") BigDecimal amount);
}
