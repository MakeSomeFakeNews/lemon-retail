import { useDictStore } from '@/store/dict'
import { findDict } from '@/utils/dict'
import { tt } from '@/locales'

/** 零售前台页面共用的字典 */
export const POS_DICTS = ['pos_pay_method', 'pos_sale_kind', 'pos_sale_status', 'pos_shift_status', 'pos_payment_status', 'pos_price_source', 'pos_promotion_kind']

/** 字典标签（已加载时取缓存，未加载时按语言包的译文，都没有时显示值） */
export function dictLabel(type, value) {
  if (value === null || value === undefined) return ''
  const cached = findDict(useDictStore().cache[type], value)
  return cached?.label || tt(`dict.${type}.${value}`, String(value))
}

/** 新的商户订单号（幂等键）：前端生成，网络重试沿用同一个 */
export function newTradeNo() {
  const rand = Math.random().toString(36).slice(2, 10).toUpperCase()
  return `W${Date.now()}${rand}`
}
