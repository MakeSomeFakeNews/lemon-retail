package com.lemon.erp.modules.pos.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.pos.model.PosPriceSource;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * POS 单据明细：价格均为录入单位的含税价
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pos_sale_item")
public class PosSaleItem extends BaseEntity {

    private Long saleId;

    private Integer lineNo;

    private Long materialId;

    private String unit;

    private BigDecimal unitRatio;

    private BigDecimal inputQty;

    private BigDecimal qty;

    private BigDecimal listPrice;

    private BigDecimal price;

    private PosPriceSource priceSource;

    private Long specialId;

    private BigDecimal discountRate;

    private BigDecimal salePrice;

    private BigDecimal amount;

    private BigDecimal orderShare;

    private BigDecimal finalAmount;

    private BigDecimal taxRate;

    private String serialNos;

    private Long origItemId;

    private BigDecimal returnedQty;

    private BigDecimal returnedAmount;

    private String remark;

    @TableField(exist = false)
    private String materialCode;

    @TableField(exist = false)
    private String materialName;

    @TableField(exist = false)
    private String spec;

    @TableField(exist = false)
    private String barcode;

    @TableField(exist = false)
    private String specialName;
}
