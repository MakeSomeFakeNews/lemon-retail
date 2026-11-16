package com.lemon.erp.modules.dist.model;

/**
 * 收货差异的处理方式（字典 dist_discrepancy_handling），由总部判定责任方后处理：
 * <ul>
 *   <li>少收 / 破损：差异数量在途（收货方 → 发货方的差异调拨），{@link #RETURN_SOURCE} 退回发货方（发货方收货入库）、
 *       {@link #SOURCE_WRITE_OFF} 发货方承担（收回后在发货仓库报损出库）、{@link #TARGET_WRITE_OFF} 收货方承担（收回后调给收货方并在收货仓库报损出库）；</li>
 *   <li>多收：{@link #ACCEPT} 收货方留用（发货仓库调给收货仓库，补记库存）、{@link #RETURN_SOURCE} 退回发货方（货物原路退回，不入账）</li>
 * </ul>
 */
public enum DiscrepancyHandling {
    /** 退回发货方 */
    RETURN_SOURCE,
    /** 发货方承担：在发货仓库报损 */
    SOURCE_WRITE_OFF,
    /** 收货方承担：在收货仓库报损 */
    TARGET_WRITE_OFF,
    /** 收货方留用（多收） */
    ACCEPT
}
