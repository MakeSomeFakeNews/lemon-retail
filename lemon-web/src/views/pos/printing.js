import { printDocs } from '@/utils/print'
import { printName as n } from '@/utils/printTemplates'
import { money, qty } from '@/utils/number'
import { dictLabel } from './pos'
import { t } from '@/locales'
import { PaymentStatus, ReceiptPaper, SaleKind } from '@/constants/enums'

/**
 * 零售前台的打印（后端 JasperReports 生成 PDF，模板见 utils/printTemplates.js 的 pos.receipt80 / pos.receipt58 / pos.shift）。
 * 表头字段、列、签字栏的名称用打印名称（中文匹配键），显示时按界面语言翻译
 */

/** 小票：sale 为单据详情（含 items、payments），paper 为收银台的小票纸宽 */
export function printReceipt(sale, paper = ReceiptPaper.MM80) {
  const pays = (sale.payments || []).filter((p) => p.status === PaymentStatus.SUCCESS)
  const methods = pays.map((p) => `${dictLabel('pos_pay_method', p.payMethod)} ${money(Math.abs(p.amount))}`).join(' / ')
  const discount = Number(sale.listAmount || 0) - Number(sale.amount || 0) + Number(sale.orderDiscount || 0)
  const doc = {
    title: t(sale.kind === SaleKind.RETURN ? 'pos.print.returnTitle' : 'pos.print.saleTitle', { store: sale.storeName }),
    no: sale.saleNo,
    fields: [
      [n('store'), sale.storeName, false, n('store')],
      [n('terminal'), sale.terminalName, false, n('terminal')],
      [n('cashier'), sale.cashierName, false, n('cashier')],
      [n('saleTime'), sale.saleTime, false, n('saleTime')],
      [n('member'), sale.memberName || sale.memberCode || '-', false, n('member')],
      [n('totalQty'), qty(sale.totalQty), false, n('totalQty')],
      [n('listAmount'), money(sale.listAmount), false, n('listAmount')],
      [n('discountAmount'), money(discount), false, n('discountAmount')],
      [n('roundOff'), money(sale.roundOff), false, n('roundOff')],
      [n('receivable'), money(sale.payableAmount), false, n('receivable')],
      [n('paid'), money(sale.paidAmount), false, n('paid')],
      [n('change'), money(sale.changeAmount), false, n('change')],
      [n('payMethods'), methods, true, n('payMethods')]
    ],
    columns: [
      { key: 'name', title: n('goods') },
      { key: 'qty', title: n('quantity'), align: 'right' },
      { key: 'price', title: n('salePrice'), align: 'right' },
      { key: 'amount', title: n('amount'), align: 'right' }
    ],
    rows: (sale.items || []).map((i) => ({ name: i.materialName, qty: `${qty(i.inputQty)}${i.unit || ''}`, price: money(i.salePrice), amount: money(i.finalAmount) })),
    note: t('pos.print.thanks')
  }
  const type = paper === ReceiptPaper.MM58 ? 'pos.receipt58' : 'pos.receipt80'
  return printDocs(doc, sale.saleNo, type)
}

/** 交班单：report 为 /pos/shift/{id}/report 的结果 */
export function printShift(report) {
  const s = report.shift
  const doc = {
    title: n('posShift'),
    no: s.shiftNo,
    fields: [
      [n('store'), s.storeName, false, n('store')],
      [n('terminal'), s.terminalName, false, n('terminal')],
      [n('cashier'), s.cashierName, false, n('cashier')],
      [n('shiftNo'), s.shiftNo, false, n('shiftNo')],
      [n('openTime'), s.openTime, false, n('openTime')],
      [n('closeTime'), s.closeTime || '-', false, n('closeTime')],
      [n('openingCash'), money(s.openingCash), false, n('openingCash')],
      [n('saleCount'), report.sales?.saleCount ?? 0, false, n('saleCount')],
      [n('saleAmount'), money(report.sales?.saleAmount), false, n('saleAmount')],
      [n('returnAmount'), money(report.sales?.returnAmount), false, n('returnAmount')],
      [n('expectedCash'), money(report.expectedCash), false, n('expectedCash')],
      [n('countedCash'), s.countedCash == null ? '-' : money(s.countedCash), false, n('countedCash')],
      [n('cashDiff'), s.cashDiff == null ? '-' : money(s.cashDiff), false, n('cashDiff')]
    ],
    columns: [
      { key: 'method', title: n('payMethod') },
      { key: 'account', title: n('account') },
      { key: 'receipt', title: n('receipts'), align: 'right' },
      { key: 'refund', title: n('refunds'), align: 'right' },
      { key: 'net', title: n('netAmount'), align: 'right' },
      { key: 'count', title: n('payCount'), align: 'right' }
    ],
    rows: (report.pays || []).map((p) => ({
      method: dictLabel('pos_pay_method', p.payMethod),
      account: p.accountName,
      receipt: money(p.receiptAmount),
      refund: money(p.refundAmount),
      net: money(p.amount),
      count: p.payCount
    })),
    note: s.remark,
    signs: [n('cashier'), n('handover'), n('storeManager')]
  }
  return printDocs(doc, s.shiftNo, 'pos.shift')
}
