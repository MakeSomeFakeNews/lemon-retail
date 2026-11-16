package com.lemon.erp.modules.pos.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.entity.PosPointsLog;
import lombok.Data;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PosPointsLogMapper extends BaseMapper<PosPointsLog> {

    /** 列表：endNext 为结束日期的次日零点（按发生时间筛选） */
    IPage<PosPointsLog> selectPageList(IPage<PosPointsLog> page, @Param("q") PosQueries.PointsLog q, @Param("endNext") LocalDateTime endNext);

    /**
     * 积分清零的候选：有积分的会员，before 之前增加的积分合计、全部减少的积分合计（先获得的先用），before 为空时只取当前积分
     */
    List<Clearable> selectClearable(@Param("before") LocalDateTime before);

    void insertBatch(@Param("list") List<PosPointsLog> list);

    @Data
    class Clearable {
        private Long memberId;
        private BigDecimal points;
        private BigDecimal earnedBefore;
        private BigDecimal spent;
    }
}
