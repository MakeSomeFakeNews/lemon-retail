package com.lemon.erp.modules.store.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.TreeEntity;
import com.lemon.erp.common.enums.EnableStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 区域（多级）：门店归属到区域，报表、营销活动可按区域（含下级区域）选择门店
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("store_region")
public class StoreRegion extends TreeEntity<StoreRegion> {

    @NotBlank(message = "{store.valid.regionCodeRequired}")
    @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "{store.valid.codeFormat}")
    private String code;

    @NotBlank(message = "{store.valid.regionNameRequired}")
    @Size(max = 64, message = "{store.valid.nameTooLong}")
    private String name;

    private EnableStatus status;

    @Size(max = 255, message = "{store.valid.remarkTooLong}")
    private String remark;
}
