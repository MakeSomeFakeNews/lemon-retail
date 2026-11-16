package com.lemon.erp.modules.pos.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.pos.model.PromotionKind;
import com.lemon.erp.modules.pos.model.PromotionRange;
import com.lemon.erp.modules.pos.model.PromotionThreshold;
import com.lemon.erp.modules.pos.model.SaleChannel;
import com.lemon.erp.modules.pos.model.SpecialScope;
import com.lemon.erp.modules.pos.model.SpecialStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 满减 / 满折 / 满赠活动（整单级，在特价之后计算，每单只应用一个）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pos_promotion")
public class PosPromotion extends BaseEntity {

    private String name;

    private PromotionKind kind;

    private PromotionThreshold thresholdType;

    private BigDecimal threshold;

    private BigDecimal reduceAmount;

    private BigDecimal discountRate;

    private Boolean stackable;

    private PromotionRange goodsScope;

    private Boolean excludeSpecial;

    private Integer priority;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private SaleChannel channel;

    private SpecialScope storeScope;

    private Boolean memberOnly;

    private SpecialStatus status;

    private String remark;

    private Long createBy;

    @TableField(exist = false)
    private String createByName;

    /** 适用的门店 / 区域 */
    @TableField(exist = false)
    private List<Long> targetIds;

    @TableField(exist = false)
    private List<String> targetNames;

    /** 参与的商品 / 分类 */
    @TableField(exist = false)
    private List<PosPromotionGoods> goods;

    @TableField(exist = false)
    private List<PosPromotionGift> gifts;
}
