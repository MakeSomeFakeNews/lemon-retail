package com.lemon.erp.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.system.entity.SysUserStore;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;

public interface SysUserStoreMapper extends BaseMapper<SysUserStore> {

    /** 批量插入用户的管辖门店，单条 INSERT ... VALUES (...), (...) */
    int insertBatch(@Param("userId") Long userId, @Param("storeIds") Collection<Long> storeIds);
}
