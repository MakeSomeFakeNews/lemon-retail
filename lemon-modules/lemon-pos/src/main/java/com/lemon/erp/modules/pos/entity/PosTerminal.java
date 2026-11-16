package com.lemon.erp.modules.pos.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.pos.model.ReceiptPaper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 收银台：属于一个门店，出库仓库、现金账户必须是本门店的（或共用的账户）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pos_terminal")
public class PosTerminal extends BaseEntity {

    /** 留空时按编码规则（SYT）自动生成 */
    @Pattern(regexp = "^[A-Za-z0-9_-]{0,32}$", message = "{pos.valid.codeFormat}")
    private String code;

    @NotBlank(message = "{pos.valid.terminalNameRequired}")
    @Size(max = 64, message = "{pos.valid.nameTooLong}")
    private String name;

    @NotNull(message = "{pos.valid.storeRequired}")
    private Long storeId;

    /** 为空时取门店的默认仓库 */
    private Long warehouseId;

    /** 为空时取门店的默认结算账户 */
    private Long cashAccountId;

    private ReceiptPaper receiptPaper;

    private EnableStatus status;

    private Integer sort;

    @Size(max = 255, message = "{pos.valid.remarkTooLong}")
    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String storeName;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String warehouseName;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String cashAccountName;

    /** 当班的班次（列表显示） */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String openShiftNo;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String openCashierName;
}
