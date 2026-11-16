package com.lemon.erp.modules.store.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.framework.store.StoreStatus;
import com.lemon.erp.framework.store.StoreType;
import com.lemon.erp.modules.store.model.FranchiseSettleMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 门店：总部 / 直营店 / 加盟店 / 配送中心。仓库、结算账户、员工、单据通过 store_id 归属到门店
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("store_info")
public class StoreInfo extends BaseEntity {

    /** 留空时按编码规则（MD）自动生成 */
    @Pattern(regexp = "^[A-Za-z0-9_-]{0,32}$", message = "{store.valid.codeFormat}")
    private String code;

    @NotBlank(message = "{store.valid.storeNameRequired}")
    @Size(max = 64, message = "{store.valid.nameTooLong}")
    private String name;

    @Size(max = 32, message = "{store.valid.shortNameTooLong}")
    private String shortName;

    /** 字典 store_type，总部每个租户唯一 */
    @NotNull(message = "{store.valid.typeRequired}")
    private StoreType type;

    /** 所属区域 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long regionId;

    /** 上级配送门店：要货默认由它供货（总部或配送中心） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long supplyStoreId;

    /** 零售价方案（scm_price_scheme），为空按总部价格 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long priceSchemeId;

    /** 字典 store_status：停业、闭店不能开单；通过「修改状态」接口变更 */
    private StoreStatus status;

    @Size(max = 64, message = "{store.valid.contactTooLong}")
    private String contact;

    @Size(max = 32, message = "{store.valid.phoneTooLong}")
    private String phone;

    @Size(max = 255, message = "{store.valid.addressTooLong}")
    private String address;

    private BigDecimal longitude;

    private BigDecimal latitude;

    @Size(max = 64, message = "{store.valid.businessHoursTooLong}")
    private String businessHours;

    /** 营业面积（平方米），用于坪效 */
    @DecimalMin(value = "0", message = "{store.valid.areaNonNegative}")
    private BigDecimal area;

    /** 默认仓库（必须属于本门店），开单默认值 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long defaultWarehouseId;

    /** 默认结算账户（必须属于本门店），收款默认值 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long defaultAccountId;

    /** 对应的部门（可选）：门店员工一般建在该部门下 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long deptId;

    // ---------------- 加盟信息（加盟店）

    @Size(max = 64, message = "{store.valid.franchiseeTooLong}")
    private String franchisee;

    private LocalDate contractStart;

    private LocalDate contractEnd;

    @DecimalMin(value = "0", message = "{store.valid.depositNonNegative}")
    private BigDecimal deposit;

    /** 加盟结算方式，字典 store_settle_mode */
    private FranchiseSettleMode settleMode;

    /** 配送价方案（加盟店按配送价向总部进货），为空按价格列「配送价」 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long deliverySchemeId;

    private Integer sort;

    @Size(max = 255, message = "{store.valid.remarkTooLong}")
    private String remark;

    // ---------------- 只用于新增、显示

    /** 新增时同时创建同名仓库，作为默认仓库 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @TableField(exist = false)
    private Boolean createWarehouse;

    /** 新增时同时创建现金账户，作为默认结算账户 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @TableField(exist = false)
    private Boolean createAccount;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String regionName;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String supplyStoreName;
}
