package com.lemon.erp.modules.pos.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.pos.dto.PosQueries;
import com.lemon.erp.modules.pos.entity.PosMember;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

public interface PosMemberMapper extends BaseMapper<PosMember> {

    IPage<PosMember> selectPageList(IPage<PosMember> page, @Param("q") PosQueries.Member q);

    /** 详情（含等级名称、会员折扣、开卡门店） */
    PosMember selectDetail(@Param("id") Long id);

    /** 按手机号或会员号精确查找（含等级折扣） */
    PosMember selectByCode(@Param("code") String code);

    /** 累加消费金额、次数（退货为负金额、次数 0），记录最近消费时间 */
    int addStats(@Param("id") Long id, @Param("amount") BigDecimal amount, @Param("visits") int visits);
}
