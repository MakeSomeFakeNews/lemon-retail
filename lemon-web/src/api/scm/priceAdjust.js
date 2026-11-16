import request from '@/utils/request'

/** 调价单：params { current, size, keyword, status, target, schemeId } */
export const pagePriceAdjust = (params) => request.get('/scm/price-adjust', { params })
/** 详情：含 schemeIds、schemeNames、items [{ materialId, materialCode, materialName, spec, unit, retailPrice, memberPrice, remark, hqRetailPrice }] */
export const getPriceAdjust = (id) => request.get(`/scm/price-adjust/${id}`)
/** { target: HQ | SCHEME, schemeIds, effectiveTime（'YYYY-MM-DD HH:mm:ss'，为空审核后立即生效）, reason, items: [{ materialId, retailPrice, memberPrice, remark }] } → id */
export const addPriceAdjust = (data) => request.post('/scm/price-adjust', data)
export const updatePriceAdjust = (id, data) => request.put(`/scm/price-adjust/${id}`, data)
export const deletePriceAdjust = (ids) => request.delete(`/scm/price-adjust/${Array.isArray(ids) ? ids.join(',') : ids}`)
/** 提交：发布了审批流程时进入审批，否则直接审核（生效时间为空或已到时立即生效）→ 提交后的状态 */
export const submitPriceAdjust = (id) => request.post(`/scm/price-adjust/${id}/submit`)
export const withdrawPriceAdjust = (id) => request.post(`/scm/price-adjust/${id}/withdraw`)
export const voidPriceAdjust = (id) => request.post(`/scm/price-adjust/${id}/void`)
/** 当前价格：schemeId 为空取总部零售价 → { 物料 ID: { retailPrice, memberPrice } } */
export const currentPrices = (schemeId, materialIds) =>
  request.get('/scm/price-adjust/current-prices', { params: { schemeId, materialIds: materialIds.join(',') } })
/** 调价历史：params { current, size, materialId, schemeId, target, adjustId } → 按生效时间倒序 */
export const priceHistory = (params) => request.get('/scm/price-adjust/history', { params })
