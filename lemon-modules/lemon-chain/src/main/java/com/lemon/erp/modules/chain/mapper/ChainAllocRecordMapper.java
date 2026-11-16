package com.lemon.erp.modules.chain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.chain.entity.ChainAllocRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ChainAllocRecordMapper extends BaseMapper<ChainAllocRecord> {

    void insertBatch(@Param("list") List<ChainAllocRecord> list);

    /** 一次分摊的结果（含门店名称） */
    List<ChainAllocRecord> selectByRun(@Param("runId") Long runId);
}
