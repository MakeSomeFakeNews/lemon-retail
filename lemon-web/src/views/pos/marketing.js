import { round2 } from '@/utils/number'

/** 充值赠送：达到的最高档位的赠送金额，没有档位时为 0（与后端 PosMemberRuleService.bonusFor 一致） */
export function bonusFor(tiers, amount) {
  const a = Number(amount || 0)
  let best = null
  for (const t of tiers || []) {
    if (a >= Number(t.minAmount) && (!best || Number(t.minAmount) > Number(best.minAmount))) best = t
  }
  return best ? Number(best.bonusAmount) : 0
}

/**
 * 积分抵现的上限（元）：应收 × 抵现上限%（向下取到分）、会员积分可抵的金额、还需支付，三者取小
 *
 * @param rule 积分与储值规则 { redeemPoints, redeemMaxRate }
 */
export function maxRedeemAmount(rule, payable, memberPoints, remaining) {
  if (!rule) return 0
  const byRate = Math.floor(Number(payable) * Number(rule.redeemMaxRate) + 1e-6) / 100
  const byPoints = Math.floor((Number(memberPoints || 0) / Number(rule.redeemPoints)) * 100 + 1e-6) / 100
  return Math.max(0, round2(Math.min(byRate, byPoints, Number(remaining))))
}

/** 商品范围的一行：商品（materialId）或分类编码（categoryCode，含下级分类） */
export const emptyGoods = (category = false) => ({ key: `${Date.now()}-${Math.random()}`, category, materialId: undefined, materialLabel: undefined, categoryCode: undefined })

/** 后端返回的商品范围（不积分设置、满减活动的参与商品）→ 编辑行 */
export function toGoodsRow(g, category) {
  return {
    key: g.id ?? `${Date.now()}-${Math.random()}`,
    category: category ?? !g.materialId,
    materialId: g.materialId || undefined,
    materialLabel: g.materialId ? `${g.materialCode || ''} ${g.materialName || ''}`.trim() : undefined,
    categoryCode: g.categoryCode || undefined
  }
}

/** 编辑行 → 请求体（去掉空行） */
export function goodsPayload(rows) {
  return rows
    .filter((r) => (r.category ? r.categoryCode : r.materialId))
    .map((r) => (r.category ? { categoryCode: r.categoryCode } : { materialId: r.materialId }))
}
