import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')
/** 数组参数按逗号拼接（后端 List 参数） */
const joinArrays = (params = {}) =>
  Object.fromEntries(Object.entries(params).map(([k, v]) => [k, Array.isArray(v) ? (v.length ? v.join(',') : undefined) : v]))

// ---------------------------------------------------------------- 要货单

/** params { current, size, keyword, statuses: [], storeId, supplyWarehouseId, begin, end, materialId } */
export const pageRequisition = (params) => request.get('/dist/requisition', { params: joinArrays(params) })
/** 详情：含 items [{ id, materialId, materialCode, materialName, spec, unit, qty, suggestQty, approvedQty, deliveredQty, stockQty, transitQty, supplyAvailable, remark }] */
export const getRequisition = (id) => request.get(`/dist/requisition/${id}`)
/** { warehouseId, supplyWarehouseId, bizDate, expectedDate, remark, items: [{ materialId, qty, suggestQty, remark }] } → id */
export const addRequisition = (data) => request.post('/dist/requisition', data)
export const updateRequisition = (id, data) => request.put(`/dist/requisition/${id}`, data)
export const deleteRequisition = (id) => request.delete(`/dist/requisition/${ids(id)}`)
/** 提交 → 提交后的状态（APPROVING / SUBMITTED） */
export const submitRequisition = (id) => request.post(`/dist/requisition/${id}/submit`)
/** 审核：items [{ itemId, approvedQty }] */
export const auditRequisition = (id, items) => request.post(`/dist/requisition/${id}/audit`, { items })
export const rejectRequisition = (id, reason) => request.post(`/dist/requisition/${id}/reject`, { reason })
export const closeRequisition = (id, reason) => request.post(`/dist/requisition/${id}/close`, { reason })
/** 要货建议 → [{ materialId, minQty, stockQty, transitQty, supplyAvailable, suggestQty }] */
export const requisitionSuggest = (warehouseId, supplyWarehouseId, materialIds) =>
  request.get('/dist/requisition/suggest', { params: { warehouseId, supplyWarehouseId, materialIds: ids(materialIds) } })
/** 要货汇总 → [{ supplyWarehouseId, supplyWarehouseName, materialId, materialCode, ..., pendingQty, available, shortage, supplierId, supplierName }] */
export const requisitionSummary = (params) => request.get('/dist/requisition/summary', { params })
/** 选中的要货单生成配送单 → 配送单 ID 列表 */
export const requisitionToDelivery = (requisitionIds, bizDate) => request.post('/dist/requisition/to-delivery', { requisitionIds, bizDate })
/** 缺货生成采购订单：{ warehouseId, items: [{ materialId, qty, supplierId }] } → 采购订单 ID 列表 */
export const requisitionToPurchase = (data) => request.post('/dist/requisition/to-purchase', data)

// ---------------------------------------------------------------- 配送单 / 退仓单（kind：delivery | return）

export const pageDelivery = (kind, params) => request.get(`/dist/${kind}`, { params: joinArrays(params) })
export const getDelivery = (kind, id) => request.get(`/dist/${kind}/${id}`)
/** { fromWarehouseId, toWarehouseId, bizDate, remark, items: [{ requisitionItemId, materialId, locationId, batchNo, serialNos, qty, price, remark }] } → id */
export const addDelivery = (kind, data) => request.post(`/dist/${kind}`, data)
export const updateDelivery = (kind, id, data) => request.put(`/dist/${kind}/${id}`, data)
export const deleteDelivery = (kind, id) => request.delete(`/dist/${kind}/${ids(id)}`)
export const shipDelivery = (kind, id) => request.post(`/dist/${kind}/${id}/ship`)
/** 收货：{ receiveDate, items: [{ itemId, receivedQty, kind: SHORT | DAMAGED, reason }] } */
export const receiveDelivery = (kind, id, data) => request.post(`/dist/${kind}/${id}/receive`, data)
export const submitReturn = (id) => request.post(`/dist/return/${id}/submit`)
export const approveReturn = (id) => request.post(`/dist/return/${id}/approve`)
export const rejectReturn = (id, reason) => request.post(`/dist/return/${id}/reject`, { reason })
/** 配送价预览（加盟店的配送价方案价）→ { 物料 ID: 价格 } */
export const deliveryPrices = (toWarehouseId, materialIds) =>
  request.get('/dist/delivery/prices', { params: { toWarehouseId, materialIds: ids(materialIds) } })

// ---------------------------------------------------------------- 差异、在途、往来、报表、日期锁定

/** params { current, size, kind, keyword, statuses: [], storeId, begin, end, materialId } */
export const pageDiscrepancy = (params) => request.get('/dist/discrepancy', { params: joinArrays(params) })
/** { handling, ioCategory, remark } */
export const handleDiscrepancy = (id, data) => request.post(`/dist/discrepancy/${id}/handle`, data)
/** params { current, size, keyword, storeId, toStoreId, warehouseId, begin, end, materialId } */
export const pageTransit = (params) => request.get('/dist/transit', { params })
export const transitTotal = (params) => request.get('/dist/transit/total', { params })
export const pageLedger = (params) => request.get('/dist/ledger', { params })
export const ledgerSummary = (params) => request.get('/dist/ledger/summary', { params })
/** params { begin, end, regionId, storeId } */
export const storeCompare = (params) => request.get('/dist/report/store-compare', { params })
export const listDateLocks = () => request.get('/dist/date-lock')
/** { storeId（为空为全公司）, lockDate（为空解除）, remark } */
export const setDateLock = (data) => request.put('/dist/date-lock', data)
