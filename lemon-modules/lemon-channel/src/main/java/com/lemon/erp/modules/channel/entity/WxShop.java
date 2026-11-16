package com.lemon.erp.modules.channel.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.channel.model.WxShopMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 微信小店店铺设置。AppSecret、推送 Token、EncodingAESKey 只写不读（返回是否已设置），修改时留空表示不变
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ch_wx_shop")
public class WxShop extends BaseEntity {

    /** 租户：消息推送免登录访问时按店铺所属租户处理 */
    @JsonIgnore
    private Long tenantId;

    @NotBlank(message = "{channel.valid.shopNameRequired}")
    @Size(max = 64, message = "{channel.valid.shopNameTooLong}")
    private String name;

    @NotNull(message = "{channel.valid.modeRequired}")
    private WxShopMode mode;

    @Size(max = 64, message = "{channel.valid.appIdTooLong}")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String appId;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(max = 128, message = "{channel.valid.secretTooLong}")
    private String appSecret;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(max = 64, message = "{channel.valid.pushTokenTooLong}")
    private String pushToken;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(max = 64, message = "{channel.valid.aesKeyTooLong}")
    private String aesKey;

    @NotNull(message = "{channel.valid.storeRequired}")
    private Long storeId;

    @NotNull(message = "{channel.valid.warehouseRequired}")
    private Long warehouseId;

    /** 为空时自动创建客户「微信小店顾客」 */
    private Long customerId;

    @Size(max = 64, message = "{channel.valid.addressIdTooLong}")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String returnAddressId;

    private Boolean stockSyncEnabled;

    @DecimalMin(value = "0", message = "{channel.valid.safetyStockNonNegative}")
    private BigDecimal safetyStock;

    @Min(value = 5, message = "{channel.valid.intervalRange}")
    @Max(value = 1440, message = "{channel.valid.intervalRange}")
    private Integer stockSyncInterval;

    private Boolean orderSyncEnabled;

    private Boolean autoCreateOrder;

    private EnableStatus status;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime stockSyncedAt;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderSyncedTo;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime aftersaleSyncedTo;

    @Size(max = 255, message = "{channel.valid.remarkTooLong}")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    // ---------------------------------------------------------------- 只用于显示

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private boolean appSecretSet;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private boolean pushTokenSet;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private boolean aesKeySet;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String storeName;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String warehouseName;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String customerName;

    /** 消息推送地址（相对接口根路径，前端拼上访问地址后在小店后台登记） */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String callbackPath;

    /** 清空已保存的推送 Token / EncodingAESKey（为空的输入表示不修改，需要单独标记清空） */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @TableField(exist = false)
    private boolean clearPushSecrets;
}
