import request from '@/utils/request'

/** 门店价格方案：params { current, size, keyword, status } → { records: [{ id, code, name, status, remark, itemCount }], total } */
export const pagePriceScheme = (params) => request.get('/scm/price-scheme', { params })
/** 下拉：启用的方案 [{ id, code, name, status }]；ids 为需要回显的方案（含停用的） */
export const priceSchemeOptions = (ids) =>
  request.get('/scm/price-scheme/options', { params: { ids: ids?.length ? ids.join(',') : undefined } })
/** 详情：含 items [{ materialId, materialCode, materialName, spec, unit, retailPrice, memberPrice, hqRetailPrice }] */
export const getPriceScheme = (id) => request.get(`/scm/price-scheme/${id}`)
/** { code?（留空自动生成）, name, status, remark, items: [{ materialId, retailPrice, memberPrice }] } → id */
export const addPriceScheme = (data) => request.post('/scm/price-scheme', data)
export const updatePriceScheme = (id, data) => request.put(`/scm/price-scheme/${id}`, data)
export const changePriceSchemeStatus = (id, status) => request.put(`/scm/price-scheme/${id}/status`, { status })
export const deletePriceScheme = (ids) => request.delete(`/scm/price-scheme/${Array.isArray(ids) ? ids.join(',') : ids}`)
