package com.lemon.erp.modules.dist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.dist.entity.DistDateLock;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface DistDateLockMapper extends BaseMapper<DistDateLock> {

    /** 全部锁定设置（含门店、设置人名称） */
    List<DistDateLock> selectAllWithNames();

    /** 生效的锁定日期：全公司与仓库所属门店中较晚的一个；warehouseId 为空只取全公司 */
    LocalDate selectEffectiveDate(@Param("warehouseId") Long warehouseId);
}
