import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 销售价目表 */
export const pagePriceList = (params) => request.get('/scm/price-list', { params })
export const addPriceList = (data) => request.post('/scm/price-list', data)
export const updatePriceList = (id, data) => request.put(`/scm/price-list/${id}`, data)
export const deletePriceList = (id) => request.delete(`/scm/price-list/${ids(id)}`)
/** 取价：{ materialId, customerId, qty, date }，返回 { price, taxRate, ... }，没有匹配的价格时为空 */
export const quotePrice = (params) => request.get('/scm/price-list/quote', { params })
/** 价格控制：最低销售价、最高采购价 */
export const pagePriceControl = (params) => request.get('/scm/price-list/control', { params })
export const priceControlsOf = (materialIds) => request.get('/scm/price-list/control/of', { params: { materialIds: materialIds.join(',') } })
export const addPriceControl = (data) => request.post('/scm/price-list/control', data)
export const updatePriceControl = (id, data) => request.put(`/scm/price-list/control/${id}`, data)
export const deletePriceControl = (id) => request.delete(`/scm/price-list/control/${ids(id)}`)
/** 历史成交价：{ kind: sales | purchase, partnerId, materialId, limit } → [{ orderNo, orderDate, price, taxRate, qty, currency }] */
export const priceHistory = (params) => request.get('/scm/price-list/history', { params })

/** 促销活动 */
export const pagePromotion = (params) => request.get('/scm/promotion', { params })
export const getPromotion = (id) => request.get(`/scm/promotion/${id}`)
export const promotionUsages = (id) => request.get(`/scm/promotion/${id}/usages`)
export const addPromotion = (data) => request.post('/scm/promotion', data)
export const updatePromotion = (id, data) => request.put(`/scm/promotion/${id}`, data)
export const changePromotionStatus = (id, status) => request.put(`/scm/promotion/${id}/status`, { status })
export const deletePromotion = (id) => request.delete(`/scm/promotion/${id}`)
/** 下单前预览：data { customerId, orderDate, items: [{ materialId, qty, price }] } → { lines, gifts, discountAmount, notes } */
export const previewPromotion = (data) => request.post('/scm/promotion/preview', data)
