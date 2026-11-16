package com.lemon.erp.modules.pos.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.pos.entity.PosMemberLevel;

import java.util.List;

public interface PosMemberLevelMapper extends BaseMapper<PosMemberLevel> {

    /** 全部等级（含会员数），按排序 */
    List<PosMemberLevel> selectWithCount();
}
