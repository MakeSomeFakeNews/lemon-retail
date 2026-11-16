package com.lemon.erp.modules.pos.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.entity.PosSpecial;
import com.lemon.erp.modules.pos.model.SpecialScope;
import lombok.Data;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface PosSpecialMapper extends BaseMapper<PosSpecial> {

    IPage<PosSpecial> selectPageList(IPage<PosSpecial> page, @Param("q") PosQueries.Special q);

    PosSpecial selectDetail(@Param("id") Long id);

    /** 在 now 时进行中、门店收银可用的活动中这些物料的特价（门店适用范围由调用方按 storeScope / 适用门店判断） */
    List<Candidate> selectCandidates(@Param("materialIds") Collection<Long> materialIds, @Param("now") LocalDateTime now);

    /** 取价候选：活动 × 物料 */
    @Data
    class Candidate {
        private Long specialId;
        private SpecialScope storeScope;
        private Integer memberOnly;
        private Long materialId;
        private BigDecimal specialPrice;
        private BigDecimal discountRate;
        private BigDecimal limitQty;
    }
}
