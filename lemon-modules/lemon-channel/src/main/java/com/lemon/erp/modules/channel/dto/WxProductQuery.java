package com.lemon.erp.modules.channel.dto;

/**
 * 商品映射列表查询条件
 *
 * @param mapped  true 只看已映射，false 只看未映射，为空全部
 * @param keyword 商品标题、规格、商家编码、SKU 编码、物料编码 / 名称，模糊匹配
 * @param removed 是否包含线上已删除的 SKU（默认不含）
 */
public record WxProductQuery(Long shopId, Boolean mapped, String keyword, Boolean removed) {
}
