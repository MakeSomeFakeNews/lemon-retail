import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

// ---------------------------------------------------------------- 加盟商账户

/** params { current, size, keyword, storeId, settleMode } → 加盟店及账户、应收 / 预收余额、可用额度、保证金余额 */
export const pageFranchiseAccount = (params) => request.get('/chain/franchise/account', { params })
export const getFranchiseAccount = (storeId) => request.get(`/chain/franchise/account/${storeId}`)
/** { creditLimit, remark } */
export const updateFranchiseAccount = (storeId, data) => request.put(`/chain/franchise/account/${storeId}`, data)
/** 充值（预付货款）：{ date, accountId, method, amount, remark } → 预收款单 ID */
export const topupFranchise = (storeId, data) => request.post(`/chain/franchise/account/${storeId}/topup`, data)
/** 加盟往来（配送、退回生成的加盟应收）：{ current, size, storeId, begin, end, keyword } */
export const pageFranchiseLedger = (params) => request.get('/chain/franchise/ledger', { params })
/** 加盟商往来明细：{ partnerName, opening, rows, increase, decrease, closing } */
export const franchisePartnerStatement = (storeId, begin, end) =>
  request.get(`/chain/franchise/account/${storeId}/statement`, { params: { begin, end } })
/** 加盟结算汇总：{ begin, end, settleMode } */
export const franchiseSummary = (params) => request.get('/chain/franchise/summary', { params })

// ---------------------------------------------------------------- 对账单

export const pageFranchiseStatement = (params) => request.get('/chain/franchise/statement', { params })
/** 详情（含 rows：往来明细） */
export const getFranchiseStatement = (id) => request.get(`/chain/franchise/statement/${id}`)
/** 生成（重新生成待确认的）：{ storeId, period: yyyy-MM, remark } → ID */
export const generateFranchiseStatement = (data) => request.post('/chain/franchise/statement', data)
export const confirmFranchiseStatement = (id) => request.post(`/chain/franchise/statement/${id}/confirm`)
export const unconfirmFranchiseStatement = (id) => request.post(`/chain/franchise/statement/${id}/unconfirm`)
export const deleteFranchiseStatement = (id) => request.delete(`/chain/franchise/statement/${ids(id)}`)

// ---------------------------------------------------------------- 保证金

export const pageFranchiseDeposit = (params) => request.get('/chain/franchise/deposit', { params })
export const franchiseDepositBalance = (storeId) => request.get('/chain/franchise/deposit/balance', { params: { storeId } })
/** { storeId, kind: RECEIVE | REFUND, bizDate, amount, accountId, remark } */
export const addFranchiseDeposit = (data) => request.post('/chain/franchise/deposit', data)
export const deleteFranchiseDeposit = (id) => request.delete(`/chain/franchise/deposit/${ids(id)}`)

// ---------------------------------------------------------------- 门店损益与费用分摊

/** 按日期范围：{ begin, end, regionId, storeId } → 每个门店一行，最后一行为合计（storeId 为空） */
export const storePnl = (params) => request.get('/chain/pnl', { params })
/** 门店 × 月份：{ beginMonth, endMonth, regionId, storeId } → 每个门店每月一行（period）+ 门店合计行（period 为空） */
export const storePnlGrid = (params) => request.get('/chain/pnl/grid', { params })
/** 穿透：{ storeId, begin, end } → { months, expenses, incomes } */
export const storePnlDetail = (params) => request.get('/chain/pnl/detail', { params })
export const getAllocRule = () => request.get('/chain/alloc/rule')
/** { basis, includeDc, includeFranchise, regionId, remark } */
export const saveAllocRule = (data) => request.put('/chain/alloc/rule', data)
export const pageAllocRun = (params) => request.get('/chain/alloc/run', { params })
/** 详情（含 records） */
export const getAllocRun = (id) => request.get(`/chain/alloc/run/${id}`)
/** 运行某月分摊 → ID */
export const runAlloc = (period) => request.post('/chain/alloc/run', { period })
export const deleteAllocRun = (id) => request.delete(`/chain/alloc/run/${id}`)

// ---------------------------------------------------------------- 补货建议

/** { storeId, keyword, onlySuggest } */
export const replenishSuggest = (params) => request.get('/chain/replenish', { params })
/** → { storeId, salesDays, leadDays, safetyDays, storeLeadDays, storeSafetyDays } */
export const replenishParams = (storeId) => request.get('/chain/replenish/params', { params: { storeId } })
/** { leadDays, safetyDays }（为空取默认值） */
export const saveReplenishParams = (storeId, data) => request.put(`/chain/replenish/params/${storeId}`, data)
/** 一键生成要货单：{ warehouseId, expectedDate, items: [{ materialId, qty, suggestQty }] } → 要货单 ID */
export const replenishToRequisition = (data) => request.post('/chain/replenish/requisition', data)
