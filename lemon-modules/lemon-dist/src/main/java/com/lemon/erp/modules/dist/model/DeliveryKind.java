package com.lemon.erp.modules.dist.model;

/**
 * 配送单种类（字典 dist_delivery_kind）：配送（总部 / 配送中心 → 门店）、退仓（门店 → 总部 / 配送中心）。
 * 两者都是在途调拨：发货方调出（在途），收货方收货（入库，差异生成收货差异）
 */
public enum DeliveryKind {
    /** 配送：由要货单生成或手工开单，发货方为配送中心 */
    DELIVERY,
    /** 门店退仓：门店申请 → 总部审核 → 门店发货 → 配送中心收货 */
    RETURN
}
