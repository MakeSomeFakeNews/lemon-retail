package com.lemon.erp.modules.pos.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 挂单：暂存的购物车（JSON），取单后删除
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pos_held_sale")
public class PosHeldSale extends BaseEntity {

    private Long storeId;

    private Long terminalId;

    private Long memberId;

    private Integer itemCount;

    private BigDecimal amount;

    private String remark;

    /** 列表不返回 */
    private String content;

    private Long createBy;

    @TableField(exist = false)
    private String terminalName;

    @TableField(exist = false)
    private String cashierName;

    @TableField(exist = false)
    private String memberName;
}
