package com.lemon.erp.modules.dist.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 日期锁定：store_id = 0 为全公司，其他为门店（两者都检查）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dist_date_lock")
public class DistDateLock extends BaseEntity {

    private Long storeId;
    private LocalDate lockDate;
    private String remark;
    private Long updateBy;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private String updateByName;
}
