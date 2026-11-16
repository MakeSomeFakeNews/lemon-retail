/**
 * 门店损益的各项（与后端 ChainViews.PnlRow 同名），label 为消息键；pct 为百分比
 */
export const PNL_METRICS = [
  { key: 'retailSales', label: 'chain.pnl.retailSales' },
  { key: 'wholesaleSales', label: 'chain.pnl.wholesaleSales' },
  { key: 'sales', label: 'chain.pnl.sales' },
  { key: 'cost', label: 'chain.pnl.cost' },
  { key: 'grossProfit', label: 'chain.pnl.grossProfit' },
  { key: 'grossMargin', label: 'chain.pnl.grossMargin', pct: true },
  { key: 'transferIncome', label: 'chain.pnl.transferIncome' },
  { key: 'transferCost', label: 'chain.pnl.transferCost' },
  { key: 'otherIncome', label: 'chain.pnl.otherIncome' },
  { key: 'expense', label: 'chain.pnl.expense' },
  { key: 'allocated', label: 'chain.pnl.allocated' },
  { key: 'operatingProfit', label: 'chain.pnl.operatingProfit' },
  { key: 'operatingMargin', label: 'chain.pnl.operatingMargin', pct: true },
  { key: 'salesPerArea', label: 'chain.pnl.salesPerArea' },
  { key: 'salesPerStaff', label: 'chain.pnl.salesPerStaff' }
]

export const pct = (v) => (v == null ? '-' : `${Number(v).toFixed(2)}%`)
