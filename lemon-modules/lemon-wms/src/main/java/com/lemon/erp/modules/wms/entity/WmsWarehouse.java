package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.wms.model.WarehouseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_warehouse")
public class WmsWarehouse extends BaseEntity {

    @NotBlank(message = "{wms.valid.warehouseCodeRequired}")
    @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "{wms.valid.warehouseCodeMayOnlyContainLetters}")
    private String code;

    @NotBlank(message = "{wms.valid.warehouseNameRequired}")
    @Size(max = 64, message = "{wms.valid.warehouseNameCannotExceedCharacters}")
    private String name;

    /** 所属门店：每个仓库属于一个门店，单据的门店就是其仓库的门店；新增时为空则属于总部，已有库存或单据后不能修改 */
    private Long storeId;

    /** 所属门店名称，仅展示用 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String storeName;

    /** 仓库类型，字典 wms_warehouse_type：NORMAL / RAW / FINISHED / DEFECT（不良品仓不计入可用库存） */
    private WarehouseType type;

    /** 寄售仓的客户 / 供应商 */
    private Long partnerId;

    @Size(max = 255, message = "{wms.valid.addressCannotExceedCharacters}")
    private String address;

    @Size(max = 64, message = "{wms.valid.contactCannotExceedCharacters}")
    private String contact;

    @Size(max = 32, message = "{wms.valid.contactPhoneCannotExceedCharacters}")
    private String phone;

    private Integer sort;

    /** 1 启用 0 停用；停用后不能在新单据中使用 */
    private EnableStatus status;

    @Size(max = 255, message = "{wms.valid.remarkCannotExceedCharacters}")
    private String remark;
}
