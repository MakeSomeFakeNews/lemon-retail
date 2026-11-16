import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 会计科目 */
export const accountTree = (params) => request.get('/fin/account', { params })
/** 全部科目（含上级），账簿选择 */
export const allAccounts = () => request.get('/fin/account/all')
/** 启用的末级科目 */
export const accountOptions = () => request.get('/fin/account/options')
/** 科目范围用途（库存现金 CASH、银行存款 BANK）当前使用的科目编码：{ CASH: '1001', BANK: '1002' }，按凭证科目设置 */
export const accountScopes = () => request.get('/fin/account/scopes')
export const addAccount = (data) => request.post('/fin/account', data)
export const updateAccount = (id, data) => request.put(`/fin/account/${id}`, data)
export const changeAccountStatus = (id, status) => request.put(`/fin/account/${id}/status`, { status })
export const deleteAccount = (id) => request.delete(`/fin/account/${ids(id)}`)

/** 凭证 */
export const pageVoucher = (params) => request.get('/fin/voucher', { params })
export const getVoucher = (id) => request.get(`/fin/voucher/${id}`)
export const addVoucher = (data) => request.post('/fin/voucher', data)
export const updateVoucher = (id, data) => request.put(`/fin/voucher/${id}`, data)
export const deleteVoucher = (id) => request.delete(`/fin/voucher/${ids(id)}`)
/** action：audit / unaudit / post / unpost */
export const voucherAction = (action, id) => request.post(`/fin/voucher/${action}/${ids(id)}`)

/** 会计期间 */
export const pagePeriod = (params) => request.get('/fin/period', { params })
export const closePeriod = (period) => request.post(`/fin/period/${period}/close`)
export const reopenPeriod = (period) => request.post(`/fin/period/${period}/reopen`)

/** 账簿与报表，期间格式 yyyyMM */
export const trialBalance = (params) => request.get('/fin/report/trial-balance', { params })
export const ledger = (params) => request.get('/fin/report/ledger', { params })
/** 辅助核算余额：{ dimension: customer | supplier, begin, end, accountId? } */
export const auxBalance = (params) => request.get('/fin/report/aux', { params })
export const balanceSheet = (period) => request.get('/fin/report/balance-sheet', { params: { period } })
export const incomeStatement = (period) => request.get('/fin/report/income', { params: { period } })
export const incomeTrend = (period) => request.get('/fin/report/income/trend', { params: { period } })

/** 应收 / 应付：kind = ar | ap */
export const pageBill = (kind, params) => request.get(`/fin/bill/${kind}`, { params })
export const unsettledBills = (kind, partnerId) => request.get(`/fin/bill/${kind}/unsettled`, { params: { partnerId } })
export const billAging = (kind, params) => request.get(`/fin/bill/${kind}/aging`, { params })
/** 往来汇总：一个往来单位一行（期初、单据余额、预收预付、坏账、收付款折让、净欠款），合计另取 */
export const billSummary = (kind, params) => request.get(`/fin/bill/${kind}/summary`, { params })
export const billSummaryTotal = (kind, params) => request.get(`/fin/bill/${kind}/summary/total`, { params })
export const addBill = (kind, data) => request.post(`/fin/bill/${kind}`, data)
export const deleteBill = (kind, id) => request.delete(`/fin/bill/${kind}/${ids(id)}`)
/** 返回生成的凭证数 */
export const billVoucher = (kind, id) => request.post(`/fin/bill/${kind}/voucher/${ids(id)}`)

/** 收款 / 付款：kind = receipt | payment */
export const pageSettlement = (kind, params) => request.get(`/fin/settlement/${kind}`, { params })
export const getSettlement = (kind, id) => request.get(`/fin/settlement/${kind}/${id}`)
export const addSettlement = (kind, data) => request.post(`/fin/settlement/${kind}`, data)
/** 修改草稿或已拒绝的付款单；确认草稿（核销生效或提交付款审批） */
export const updateSettlement = (kind, id, data) => request.put(`/fin/settlement/${kind}/${id}`, data)
export const confirmSettlement = (kind, id) => request.post(`/fin/settlement/${kind}/${id}/confirm`)
/** 往来单位可以冲抵的预收 / 预付款 */
export const settlementAdvances = (kind, partnerId) => request.get(`/fin/settlement/${kind}/advances`, { params: { partnerId } })
export const deleteSettlement = (kind, id) => request.delete(`/fin/settlement/${kind}/${ids(id)}`)
export const settlementVoucher = (kind, id) => request.post(`/fin/settlement/${kind}/voucher/${ids(id)}`)
/** 折让记录：已生效收付款中有折让的核销明细 */
export const pageSettlementDiscounts = (kind, params) => request.get(`/fin/settlement/${kind}/discounts`, { params })

/** 期初余额 */
export const accountOpenings = () => request.get('/fin/opening/accounts')
export const saveAccountOpenings = (items) => request.put('/fin/opening/accounts', { items })
/** 启用向导：各项期初数据的完成情况 */
export const openingSummary = () => request.get('/fin/opening/summary')
/** 期初库存导入：生成启用期间前一天的期初入库单 */
export const importOpeningStock = (startPeriod, data) => request.post('/fin/opening/stock-in/import', data, { params: { startPeriod } })
/** 期初往来导入：kind 为 AR / AP */
export const importOpeningBills = (kind, data) => request.post(`/fin/opening/bills/${kind}/import`, data)

/** 坏账核销：data { amount, date, reason }，返回凭证 ID */
export const badDebt = (kind, id, data) => request.post(`/fin/bill/${kind}/${id}/bad-debt`, data)
/** 红字冲销：返回红字凭证 ID */
export const reverseVoucher = (id, date) => request.post(`/fin/voucher/${id}/reverse`, null, { params: { date } })

/** 结账检查：[{ name, level: OK | WARN | ERROR, message }] */
export const closeCheck = (period) => request.get(`/fin/period/${period}/check`)
/** 增值税结转：data { cityRate, eduRate, localRate }，返回 { voucherId, output, input, payable, surcharge } */
export const transferVat = (period, data) => request.post(`/fin/period/${period}/vat`, data)
/** 现金流量表，期间 yyyyMM */
export const cashFlow = (begin, end) => request.get('/fin/report/cash-flow', { params: { begin, end } })

/** 发票：kind = OUTPUT 销项 | INPUT 进项 */
export const pageInvoice = (params) => request.get('/fin/invoice', { params })
export const getInvoice = (id) => request.get(`/fin/invoice/${id}`)
export const invoiceOpenBills = (kind, partnerId) => request.get('/fin/invoice/open-bills', { params: { kind, partnerId } })
export const addInvoice = (data) => request.post('/fin/invoice', data)
export const cancelInvoice = (id) => request.post(`/fin/invoice/${id}/cancel`)
/** 导入数电票 XML 文件（可多个）→ { total, imported, skipped, matched, errors } */
export const importInvoiceXml = (kind, files, autoMatch = true) => {
  const data = new FormData()
  files.forEach((f) => data.append('files', f))
  return request.post('/fin/invoice/import-xml', data, { params: { kind, autoMatch }, timeout: 120000 })
}
/** 导入发票清单（Excel 解析后的行） */
export const importInvoiceRows = (kind, data, autoMatch = true) => request.post('/fin/invoice/import', data, { params: { kind, autoMatch } })
/** 勾稽：items [{ billId, amount }] */
export const matchInvoice = (id, items) => request.post(`/fin/invoice/${id}/match`, { items })
export const unmatchInvoice = (id) => request.post(`/fin/invoice/${id}/unmatch`)
/** 进项认证：period 为空时取消认证 */
export const certifyInvoice = (ids, period) => request.post('/fin/invoice/certify', { ids, period })
/** 未开票收入（OUTPUT）/ 暂估应付（INPUT） */
export const uninvoiced = (kind, asOf) => request.get('/fin/invoice/uninvoiced', { params: { kind, asOf } })

/** 预收 / 预付：kind = RECEIPT | PAYMENT */
export const pageAdvance = (params) => request.get('/fin/advance', { params })
export const getAdvance = (id) => request.get(`/fin/advance/${id}`)
export const addAdvance = (data) => request.post('/fin/advance', data)
/** 冲抵：data { date, items: [{ billId, amount }] } */
export const applyAdvance = (id, data) => request.post(`/fin/advance/${id}/apply`, data)
export const deleteAdvance = (id) => request.delete(`/fin/advance/${id}`)

/** 经营分析 */
export const grossMargin = (params) => request.get('/fin/analysis/gross-margin', { params })
export const grossMarginDetail = (params) => request.get('/fin/analysis/gross-margin/detail', { params })
export const partnerStatement = (params) => request.get('/fin/partner-statement', { params })
/** 库龄分析：{ asOf, warehouseId, category, keyword, buckets: '30,60,90' } → { buckets, rows, total } */
export const stockAge = (params) => request.get('/fin/analysis/stock-age', { params })

/** 币种与汇率 */
export const currencies = () => request.get('/fin/currency')
/** 按币种编码新增或修改：{ code, name, symbol, status, sort } */
export const saveCurrency = (data) => request.put('/fin/currency', data)
/** 汇率：{ period?, currency? } → [{ id, currency, period, rate }] */
export const exchangeRates = (params) => request.get('/fin/currency/rate', { params })
/** 某日的记账汇率，未维护时为空 */
export const rateOf = (currency, date) => request.get('/fin/currency/rate/of', { params: { currency, date } })
/** 新增或修改某月汇率：{ currency, period, rate } */
export const saveRate = (data) => request.put('/fin/currency/rate', data)
/** 期末调汇：{ voucherId, reverseVoucherId, lines } */
export const revalue = (period) => request.post(`/fin/currency/revalue/${period}`)

// ---------------------------------------------------------------- 资金管理：账户调拨
export const pageFundTransfer = (params) => request.get('/fin/fund-transfer', { params })
export const getFundTransfer = (id) => request.get(`/fin/fund-transfer/${id}`)
export const addFundTransfer = (data) => request.post('/fin/fund-transfer', data)
export const deleteFundTransfer = (id) => request.delete(`/fin/fund-transfer/${ids(id)}`)

/** 凭证科目设置：[{ role, group, label, dimension, dictType, defaultCode, dimensionDefaults, accountId, items }] */
export const accountRules = () => request.get('/fin/account-rule')
/** 保存一个用途的设置：{ accountId, items: { 维度取值: 科目ID } }，为空的使用预置科目 */
export const saveAccountRule = (role, data) => request.put(`/fin/account-rule/${role}`, data)

/** 出纳：日记账 { accountId?, begin, end, posted } → [{ type, entryDate, voucherNo, summary, counterAccounts, debit, credit, balance }] */
export const cashJournal = (params) => request.get('/fin/cashier/journal', { params })
/** 资金日报 { begin, end, posted } → { rows, total } */
export const cashDaily = (params) => request.get('/fin/cashier/daily', { params })
/** 未生成凭证的收付款 → { RECEIPT: { count, amount }, PAYMENT: {...} } */
export const cashUnvouchered = () => request.get('/fin/cashier/unvouchered')

/** 销售提成：规则 */
export const commissionRules = () => request.get('/fin/commission/rule')
export const addCommissionRule = (data) => request.post('/fin/commission/rule', data)
export const updateCommissionRule = (id, data) => request.put(`/fin/commission/rule/${id}`, data)
export const deleteCommissionRule = (id) => request.delete(`/fin/commission/rule/${id}`)
/** 提成表：period → { id, period, status, totalAmount, lines }，没有计算时为 null */
export const commissionSheet = (period) => request.get(`/fin/commission/${period}`)
export const generateCommission = (period) => request.post(`/fin/commission/${period}/generate`)
export const adjustCommission = (lineId, data) => request.put(`/fin/commission/line/${lineId}`, data)
export const confirmCommission = (period) => request.post(`/fin/commission/${period}/confirm`)
export const unconfirmCommission = (period) => request.post(`/fin/commission/${period}/unconfirm`)

// 冲账单
export const pageOffset = (params) => request.get('/fin/offset', { params })
export const getOffset = (id) => request.get(`/fin/offset/${id}`)
export const addOffset = (data) => request.post('/fin/offset', data)
export const updateOffset = (id, data) => request.put(`/fin/offset/${id}`, data)
export const deleteOffset = (id) => request.delete(`/fin/offset/${ids(id)}`)
export const reviewOffset = (id) => request.post(`/fin/offset/${id}/review`)
export const cancelReviewOffset = (id) => request.post(`/fin/offset/${id}/cancel-review`)

/**
 * 应收应付对冲（issue #95）：关联的客户与供应商之间用应付抵减应收。
 * 候选：{ supplierId, supplierCode, supplierName, receivables, payables }（两边未核销的本位币单据）；
 * 新增：{ customerId, nettingDate, remark, items: [{ billId, amount }] }，应收合计须等于应付合计
 */
export const pageNetting = (params) => request.get('/fin/netting', { params })
export const getNetting = (id) => request.get(`/fin/netting/${id}`)
export const nettingCandidates = (customerId) => request.get('/fin/netting/candidates', { params: { customerId } })
export const addNetting = (data) => request.post('/fin/netting', data)
export const deleteNetting = (id) => request.delete(`/fin/netting/${ids(id)}`)

// ---------------------------------------------------------------- 其他收支（其他收入单、费用支出单、收支类别）
export const listIoCategory = (params) => request.get('/fin/io/category', { params })
export const addIoCategory = (data) => request.post('/fin/io/category', data)
export const updateIoCategory = (id, data) => request.put(`/fin/io/category/${id}`, data)
export const deleteIoCategory = (id) => request.delete(`/fin/io/category/${ids(id)}`)
export const pageIo = (params) => request.get('/fin/io', { params })
export const getIo = (id) => request.get(`/fin/io/${id}`)
export const addIo = (data) => request.post('/fin/io', data)
export const updateIo = (id, data) => request.put(`/fin/io/${id}`, data)
export const deleteIo = (id) => request.delete(`/fin/io/${ids(id)}`)
export const confirmIo = (id) => request.post(`/fin/io/${id}/confirm`)
export const cancelConfirmIo = (id) => request.post(`/fin/io/${id}/cancel-confirm`)
export const ioSummary = (params) => request.get('/fin/io/summary', { params })
export const pageIoLines = (params) => request.get('/fin/io/lines', { params })
