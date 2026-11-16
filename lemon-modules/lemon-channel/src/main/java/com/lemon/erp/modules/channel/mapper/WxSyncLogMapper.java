package com.lemon.erp.modules.channel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.channel.entity.WxSyncLog;
import com.lemon.erp.modules.channel.model.WxSyncType;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface WxSyncLogMapper extends BaseMapper<WxSyncLog> {

    /** 列表：含店铺名称，按时间倒序 */
    IPage<WxSyncLog> selectPageList(IPage<WxSyncLog> page, @Param("shopId") Long shopId, @Param("syncType") WxSyncType syncType,
                                    @Param("success") Boolean success, @Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);
}
