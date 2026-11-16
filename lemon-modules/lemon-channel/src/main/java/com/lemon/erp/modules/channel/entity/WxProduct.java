package com.lemon.erp.modules.channel.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 小店 SKU 与物料的映射：商品同步时写入，自动按商家编码 / SKU 编码匹配物料编码、条码、包装条码，也可以手工映射
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ch_wx_product")
public class WxProduct extends BaseEntity {

    private Long shopId;
    private String productId;
    private String skuId;
    private String title;
    private String skuAttrs;
    private String outSkuId;
    private String skuCode;
    private BigDecimal price;
    private Long platformStock;
    private Integer productStatus;
    /** 映射的物料，为空表示未映射 */
    private Long materialId;
    /** 1 件 SKU = 多少基本单位 */
    private BigDecimal unitRatio;
    /** 手工映射的不被自动匹配覆盖 */
    private Boolean matchManual;
    private Long pushedStock;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime stockPushTime;
    private String stockError;
    /** 线上已删除 */
    private Boolean removed;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime syncTime;

    // ---------------------------------------------------------------- 只用于显示（mapper XML 关联）

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String shopName;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String materialCode;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String materialName;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String materialSpec;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String materialUnit;

    /** 物料的零售价（对比小店售价） */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private BigDecimal retailPrice;
}
