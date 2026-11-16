package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.ref.RefType;
import com.lemon.erp.framework.ref.ReferenceChecker;
import com.lemon.erp.modules.system.entity.SysUser;
import com.lemon.erp.modules.system.entity.SysUserStore;
import com.lemon.erp.modules.system.mapper.SysUserMapper;
import com.lemon.erp.modules.system.mapper.SysUserStoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 删除门店前检查：有员工属于或管辖该门店
 */
@Component
@RequiredArgsConstructor
public class SysStoreReferenceChecker implements ReferenceChecker {

    private final SysUserMapper userMapper;
    private final SysUserStoreMapper userStoreMapper;

    @Override
    public String findReference(RefType type, Long id) {
        if (type != RefType.STORE) {
            return null;
        }
        boolean used = userMapper.exists(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getStoreId, id))
                || userStoreMapper.exists(Wrappers.<SysUserStore>lambdaQuery().eq(SysUserStore::getStoreId, id));
        return used ? "system.label.storeStaff" : null;
    }
}
