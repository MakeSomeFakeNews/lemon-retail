package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户的管辖门店（门店范围为「指定门店」时，所属门店之外另外可管辖的门店）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user_store")
public class SysUserStore {
    private Long userId;
    private Long storeId;
}
