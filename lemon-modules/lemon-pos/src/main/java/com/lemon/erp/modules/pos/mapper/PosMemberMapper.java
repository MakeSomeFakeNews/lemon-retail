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

    /** 增加积分（可为负数：退货扣回、调整减少时允许为负，见 deductPoints） */
    int addPoints(@Param("id") Long id, @Param("points") BigDecimal points);

    /** 扣减积分：积分不足时不更新（返回 0） */
    int deductPoints(@Param("id") Long id, @Param("points") BigDecimal points);

    /** 增加储值余额 */
    int addBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /** 扣减储值余额：余额不足时不更新（返回 0），保证余额不为负 */
    int deductBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /** 当前积分、余额（本事务内更新之后读取变动后的值） */
    PosMember selectAccount(@Param("id") Long id);
}
