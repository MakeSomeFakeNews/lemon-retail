package com.lemon.erp.modules.pos.model;

import com.lemon.erp.framework.setting.OptionEnum;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 抹零规则（业务参数 pos.rounding），label 为消息键：整单优惠后的应收金额按规则处理，差额记为抹零并分摊到各行
 */
public enum PosRounding implements OptionEnum {
    /** 不抹零 */
    NONE("pos.label.roundingNone", 2, RoundingMode.HALF_UP),
    /** 抹分：舍去分，保留到角 */
    JIAO("pos.label.roundingJiao", 1, RoundingMode.DOWN),
    /** 抹角：舍去角和分，保留到元 */
    YUAN("pos.label.roundingYuan", 0, RoundingMode.DOWN),
    /** 四舍五入到角 */
    ROUND_JIAO("pos.label.roundingRoundJiao", 1, RoundingMode.HALF_UP);

    private final String label;
    private final int scale;
    private final RoundingMode mode;

    PosRounding(String label, int scale, RoundingMode mode) {
        this.label = label;
        this.scale = scale;
        this.mode = mode;
    }

    @Override
    public String label() {
        return label;
    }

    /** 按规则处理后的应收金额（2 位小数） */
    public BigDecimal apply(BigDecimal amount) {
        return amount.setScale(scale, mode).setScale(2, RoundingMode.UNNECESSARY);
    }
}
