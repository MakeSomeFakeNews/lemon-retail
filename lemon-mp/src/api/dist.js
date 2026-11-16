import { get, post, put } from '@/utils/request'

/** 门店要货与收货（与 Web 端 api/dist 相同的接口，权限、门店范围由后端控制） */

/** 要货单：{ current, size, statuses（逗号分隔）, keyword } */
export const pageRequisition = (params) => get('/dist/requisition', params)
export const getRequisition = (id) => get(`/dist/requisition/${id}`)
/** { warehouseId, bizDate, expectedDate, remark, items: [{ materialId, qty, suggestQty }] } → id */
export const addRequisition = (data) => post('/dist/requisition', data)
export const updateRequisition = (id, data) => put(`/dist/requisition/${id}`, data)
/** 提交 → 提交后的状态 */
export const submitRequisition = (id) => post(`/dist/requisition/${id}/submit`)
/** 要货建议 → [{ materialId, stockQty, transitQty, supplyAvailable, suggestQty }] */
export const requisitionSuggest = (warehouseId, materialIds) =>
  get('/dist/requisition/suggest', { warehouseId, materialIds: materialIds.join(',') }, { silent: true })

/** 配送单（门店收货）：{ current, size, statuses, keyword } */
export const pageDelivery = (params) => get('/dist/delivery', params)
export const getDelivery = (id) => get(`/dist/delivery/${id}`)
/** 收货：{ receiveDate, items: [{ itemId, receivedQty, kind, reason }] } */
export const receiveDelivery = (id, data) => post(`/dist/delivery/${id}/receive`, data)
