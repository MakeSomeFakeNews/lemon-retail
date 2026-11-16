import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

// ---------- 店铺设置 ----------
export const pageWxShop = (params) => request.get('/channel/wx-shop', { params })
/** 店铺下拉（可管辖门店的店铺）：[{ id, name, mode, status, storeId }] */
export const wxShopOptions = () => request.get('/channel/wx-shop/options')
/** 详情：密钥不返回，appSecretSet / pushTokenSet / aesKeySet 表示是否已设置；callbackPath 为消息推送地址（相对 /api） */
export const getWxShop = (id) => request.get(`/channel/wx-shop/${id}`)
export const addWxShop = (data) => request.post('/channel/wx-shop', data)
export const updateWxShop = (id, data) => request.put(`/channel/wx-shop/${id}`, data)
export const deleteWxShop = (id) => request.delete(`/channel/wx-shop/${ids(id)}`)
/** 测试连接，返回说明文字 */
export const testWxShop = (id) => request.post(`/channel/wx-shop/${id}/test`)
/** 立即同步：type = PRODUCT | STOCK | ORDER | AFTERSALE，返回 { total, succeeded, failed, success, message } */
export const syncWxShop = (id, type) => request.post(`/channel/wx-shop/${id}/sync`, null, { params: { type } })
/** 快递公司：[{ id, name }] */
export const wxDeliveryCompanies = (id) => request.get(`/channel/wx-shop/${id}/delivery-companies`)
/** 模拟店铺：用物料生成模拟商品，返回数量 */
export const mockWxProducts = (id) => request.post(`/channel/wx-shop/${id}/mock/products`)
/** 模拟店铺：生成已付款的模拟订单并同步，返回数量 */
export const mockWxOrders = (id, count) => request.post(`/channel/wx-shop/${id}/mock/orders`, { count })

// ---------- 商品映射 ----------
export const pageWxProduct = (params) => request.get('/channel/wx-product', { params })
export const mapWxProduct = (id, data) => request.put(`/channel/wx-product/${id}/mapping`, data)
export const unmapWxProduct = (id) => request.delete(`/channel/wx-product/${id}/mapping`)
/** 自动匹配，返回新匹配的数量 */
export const autoMatchWxProduct = (shopId) => request.post('/channel/wx-product/auto-match', null, { params: { shopId } })

// ---------- 渠道订单 ----------
export const pageWxOrder = (params) => request.get('/channel/wx-order', { params })
export const getWxOrder = (id) => request.get(`/channel/wx-order/${id}`)
export const retryWxOrder = (id) => request.post(`/channel/wx-order/${id}/retry`)
export const refreshWxOrder = (id) => request.post(`/channel/wx-order/${id}/refresh`)
/** 发货：{ companyId, companyName, waybillNo } */
export const shipWxOrder = (id, data) => request.post(`/channel/wx-order/${id}/ship`, data)
export const pushWxDelivery = (id) => request.post(`/channel/wx-order/${id}/push-delivery`)

// ---------- 售后单 ----------
export const pageWxAftersale = (params) => request.get('/channel/wx-aftersale', { params })
export const acceptWxAftersale = (id) => request.post(`/channel/wx-aftersale/${id}/accept`)
export const rejectWxAftersale = (id, reason) => request.post(`/channel/wx-aftersale/${id}/reject`, { reason })
export const confirmWxAftersale = (id) => request.post(`/channel/wx-aftersale/${id}/confirm-receive`)
export const refreshWxAftersale = (id) => request.post(`/channel/wx-aftersale/${id}/refresh`)
export const retryWxAftersale = (id) => request.post(`/channel/wx-aftersale/${id}/retry`)

// ---------- 同步日志 ----------
export const pageWxSyncLog = (params) => request.get('/channel/wx-sync-log', { params })
