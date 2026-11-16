import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

// ---------------------------------------------------------------- 收银台管理
/** params { current, size, keyword, storeId, status } */
export const pageTerminal = (params) => request.get('/pos/terminal', { params })
/** 可管辖门店中启用的收银台 */
export const terminalOptions = () => request.get('/pos/terminal/options')
export const getTerminal = (id) => request.get(`/pos/terminal/${id}`)
/** { code, name, storeId, warehouseId, cashAccountId, receiptPaper, status, sort, remark } → id */
export const addTerminal = (data) => request.post('/pos/terminal', data)
export const updateTerminal = (id, data) => request.put(`/pos/terminal/${id}`, data)
export const deleteTerminal = (id) => request.delete(`/pos/terminal/${ids(id)}`)
/** 门店收款账户 → { 支付方式: 结算账户 ID } */
export const payAccounts = (storeId) => request.get('/pos/terminal/pay-accounts', { params: { storeId } })
/** { storeId, items: [{ payMethod, accountId }] } */
export const savePayAccounts = (data) => request.put('/pos/terminal/pay-accounts', data)

// ---------------------------------------------------------------- 收银
/** { terminal, storeName, shift, provider, payMethods, maxDiscountRate, rounding, perms } */
export const cashierContext = (terminalId) => request.get('/pos/context', { params: { terminalId } })
/** 扫码 / 编码识别商品 → { materialId, code, name, unit, ratio, listPrice, price, priceSource, limitQty, minPrice, minPriceBlock, stockQty, ... } */
export const lookupGoods = (terminalId, code, memberId) => request.get('/pos/lookup', { params: { terminalId, code, memberId } })
/** 试算：{ terminalId, memberId, lines: [{ materialId, unit, qty, price, discountRate }], orderDiscount } */
export const previewSale = (data) => request.post('/pos/sale/preview', data)
/** 结算：同试算，另有 tenders: [{ payMethod, amount, outTradeNo, refNo }]、remark、heldId → 单据详情 */
export const checkoutSale = (data) => request.post('/pos/sale/checkout', data)
/** 退货：{ terminalId, origSaleId, memberId, lines: [{ origItemId | materialId, unit, qty, price }], refundMethod, remark } → 退货单详情 */
export const returnSale = (data) => request.post('/pos/sale/return', data)
export const voidSale = (id, reason) => request.post(`/pos/sale/${id}/void`, { reason })
export const saleByNo = (saleNo) => request.get('/pos/sale/by-no', { params: { saleNo } })

/** 挂单：{ terminalId, memberId, amount, itemCount, remark, content } */
export const holdSale = (data) => request.post('/pos/held', data)
export const heldList = (terminalId) => request.get('/pos/held', { params: { terminalId } })
export const resumeHeld = (id, terminalId) => request.post(`/pos/held/${id}/resume`, null, { params: { terminalId } })
export const deleteHeld = (id, terminalId) => request.delete(`/pos/held/${id}`, { params: { terminalId } })

/** 聚合支付收款：{ terminalId, payMethod, amount, authCode, outTradeNo } → 收款记录 */
export const createPay = (data) => request.post('/pos/pay', data)
export const queryPay = (outTradeNo) => request.get(`/pos/pay/${outTradeNo}`)
export const confirmPay = (outTradeNo, refNo) => request.post(`/pos/pay/${outTradeNo}/confirm`, { refNo })
export const cancelPay = (outTradeNo) => request.post(`/pos/pay/${outTradeNo}/cancel`)
export const retryRefund = (id) => request.post(`/pos/payment/${id}/retry-refund`)

export const identifyMember = (code) => request.get('/pos/member/identify', { params: { code } })
/** { terminalId, phone, name } */
export const quickMember = (data) => request.post('/pos/member/quick', data)

// ---------------------------------------------------------------- 班次
export const pageShift = (params) => request.get('/pos/shift', { params })
export const currentShift = (terminalId) => request.get('/pos/shift/current', { params: { terminalId } })
export const openShift = (data) => request.post('/pos/shift/open', data)
/** 交班单 { shift, pays, sales, cashAmount, expectedCash, unsettled } */
export const shiftReport = (id) => request.get(`/pos/shift/${id}/report`)
export const closeShift = (id, data) => request.post(`/pos/shift/${id}/close`, data)

// ---------------------------------------------------------------- POS 单据
/** params { current, size, keyword, kind, status, storeId, terminalId, cashierId, memberId, materialId, begin, end } */
export const pageSale = (params) => request.get('/pos/sale', { params })
export const getSale = (id) => request.get(`/pos/sale/${id}`)

// ---------------------------------------------------------------- 会员
export const pageMember = (params) => request.get('/pos/member', { params })
export const getMember = (id) => request.get(`/pos/member/${id}`)
export const memberSales = (id, params) => request.get(`/pos/member/${id}/sales`, { params })
export const addMember = (data) => request.post('/pos/member', data)
export const updateMember = (id, data) => request.put(`/pos/member/${id}`, data)
export const changeMemberStatus = (id, status) => request.put(`/pos/member/${id}/status`, { status })
export const deleteMember = (id) => request.delete(`/pos/member/${ids(id)}`)
export const listMemberLevel = () => request.get('/pos/member-level')
export const addMemberLevel = (data) => request.post('/pos/member-level', data)
export const updateMemberLevel = (id, data) => request.put(`/pos/member-level/${id}`, data)
export const deleteMemberLevel = (id) => request.delete(`/pos/member-level/${ids(id)}`)

// ---------------------------------------------------------------- 特价活动
export const pageSpecial = (params) => request.get('/pos/special', { params })
export const getSpecial = (id) => request.get(`/pos/special/${id}`)
/** { name, beginTime, endTime, channel, storeScope, targetIds, memberOnly, remark, items: [{ materialId, specialPrice, discountRate, limitQty }] } */
export const addSpecial = (data) => request.post('/pos/special', data)
export const updateSpecial = (id, data) => request.put(`/pos/special/${id}`, data)
export const deleteSpecial = (id) => request.delete(`/pos/special/${ids(id)}`)
export const activateSpecial = (id) => request.post(`/pos/special/${id}/activate`)
export const endSpecial = (id) => request.post(`/pos/special/${id}/end`)

// ---------------------------------------------------------------- 门店日结
export const pageDailyClose = (params) => request.get('/pos/daily-close', { params })
export const getDailyClose = (id) => request.get(`/pos/daily-close/${id}`)
export const dailySummary = (storeId, businessDate) => request.get('/pos/daily-close/summary', { params: { storeId, businessDate } })
/** { storeId, businessDate, remark } → id */
export const closeDay = (data) => request.post('/pos/daily-close', data)
export const reopenDay = (id) => request.post(`/pos/daily-close/${id}/reopen`)

// ---------------------------------------------------------------- 报表、支付通道
/** 收银日报：params { groupBy, storeId, terminalId, cashierId, begin, end } → { rows, total } */
export const cashierReport = (params) => request.get('/pos/report/cashier', { params })
export const saleDetailReport = (params) => request.get('/pos/report/detail', { params })
export const getPayConfig = () => request.get('/pos/pay-config')
export const savePayConfig = (data) => request.put('/pos/pay-config', data)

// ---------------------------------------------------------------- 会员营销：积分与储值规则、积分、储值、充值、跨店结算
/** { rule, excludes, tiers } */
export const getMemberRule = () => request.get('/pos/member-rule')
/** { rule: { pointsEnabled, earnAmount, earnPoints, redeemEnabled, redeemPoints, redeemMaxRate, storedValueEnabled, remark }, excludes: [{ materialId } | { categoryCode }], tiers: [{ minAmount, bonusAmount }] } */
export const saveMemberRule = (data) => request.put('/pos/member-rule', data)
/** params { current, size, keyword, memberId, storeId, changeType, begin, end } */
export const pagePointsLog = (params) => request.get('/pos/points-log', { params })
/** { memberId, points, reason } */
export const adjustPoints = (data) => request.post('/pos/points/adjust', data)
/** { before, reason } → { members, points } */
export const clearPoints = (data) => request.post('/pos/points/clear', data)
export const pageStoredValueLog = (params) => request.get('/pos/stored-value-log', { params })
/** { memberId, amount, storeId, reason } */
export const adjustStoredValue = (data) => request.post('/pos/stored-value/adjust', data)
export const memberPointsLog = (id, params) => request.get(`/pos/member/${id}/points-log`, { params })
export const memberStoredValueLog = (id, params) => request.get(`/pos/member/${id}/stored-value-log`, { params })
/** params { current, size, keyword, storeId, memberId, begin, end } */
export const pageTopup = (params) => request.get('/pos/topup', { params })
export const getTopup = (id) => request.get(`/pos/topup/${id}`)
/** 后台充值 { storeId, memberId, amount, bonusAmount, payMethod, accountId, refNo, remark } → 充值单 */
export const adminTopup = (data) => request.post('/pos/topup', data)
/** 收银台充值 { terminalId, memberId, amount, bonusAmount, tenders, remark } → 充值单 */
export const cashierTopup = (data) => request.post('/pos/cashier/topup', data)
/** 储值跨店结算 params { storeId, begin, end } → { rows, total } */
export const storedValueSettle = (params) => request.get('/pos/report/stored-value', { params })

// ---------------------------------------------------------------- 满减 / 满折 / 满赠
export const pagePromotion = (params) => request.get('/pos/promotion', { params })
export const getPromotion = (id) => request.get(`/pos/promotion/${id}`)
export const addPromotion = (data) => request.post('/pos/promotion', data)
export const updatePromotion = (id, data) => request.put(`/pos/promotion/${id}`, data)
export const deletePromotion = (id) => request.delete(`/pos/promotion/${ids(id)}`)
export const activatePromotion = (id) => request.post(`/pos/promotion/${id}/activate`)
export const endPromotion = (id) => request.post(`/pos/promotion/${id}/end`)
