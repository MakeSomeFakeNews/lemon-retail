import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

export const pageStore = (params) => request.get('/store/store', { params })
export const getStore = (id) => request.get(`/store/store/${id}`)
/** 新增门店，返回 ID；code 留空时自动生成 */
export const addStore = (data) => request.post('/store/store', data)
export const updateStore = (id, data) => request.put(`/store/store/${id}`, data)
export const changeStoreStatus = (id, status) => request.put(`/store/store/${id}/status`, { status })
export const deleteStore = (id) => request.delete(`/store/store/${ids(id)}`)
/**
 * 门店下拉（登录即可访问）：[{ id, code, name, shortName, type, status, regionId, defaultWarehouseId, defaultAccountId }]
 * params.all 为 true 时返回全部门店（默认只返回可管辖的门店），params.trading 为 true 时只返回可以开单的门店
 */
export const storeOptions = (params) => request.get('/store/store/options', { params })

/** 零售价 / 配送价方案下拉（scm 模块的价格方案）：[{ id, code, name }] */
export const priceSchemeOptions = () => request.get('/scm/price-scheme/options')
