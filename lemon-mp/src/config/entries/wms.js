/** 工作台入口（见 index.js）：开单、扫码作业与查询，权限与 Web 端对应接口一致 */
export default [
  {
    // 快速开单：销售出库直接开单（不引用订单），参数关闭时不显示
    key: 'wms-quick-sale',
    group: 'sales',
    title: 'wms.entry.quickSale',
    color: '#f5222d',
    url: '/pkg-wms/sale/index',
    perm: 'scm:salesOut:add',
    rule: 'scm.directDoc.enabled'
  },
  {
    // 销售订单进度：按订单号或客户查询，查看明细的发货进度、出库记录和应收
    key: 'scm-sales-order',
    group: 'sales',
    title: 'trade.entry.order',
    color: '#fa8c16',
    url: '/pkg-wms/order/index',
    perm: 'scm:sales:list',
    links: ['/scm/sales']
  },
  {
    // 商品速查：名称、规格、简拼搜索商品，看库存和按设置显示的价格（价格按权限由后端过滤），点击联查图片、库存分布、价格历史
    key: 'scm-quick-lookup',
    group: 'sales',
    title: 'lookup.entry',
    color: '#fa541c',
    url: '/pkg-wms/lookup/index',
    perm: 'scm:quickLookup:list',
    links: ['/scm/quick-lookup']
  },
  {
    // 快速入库：采购入库直接开单（不引用采购订单），参数关闭时不显示
    key: 'wms-quick-purchase',
    group: 'warehouse',
    title: 'trade.entry.purchase',
    color: '#2f54eb',
    url: '/pkg-wms/purchase/index',
    perm: 'scm:purchaseIn:add',
    rule: 'scm.directDoc.enabled'
  },
  {
    // 单据草稿：销售出库、采购入库的草稿，查看、审核记账、删除；参数「进销存单据先存草稿」关闭时不显示
    key: 'scm-trade-draft',
    group: 'sales',
    title: 'trade.entry.draft',
    color: '#faad14',
    url: '/pkg-wms/draft/index',
    perm: ['scm:salesOut:list', 'scm:purchaseIn:list'],
    rule: 'scm.tradeDoc.draft'
  },
  {
    key: 'wms-stock',
    group: 'warehouse',
    title: 'wms.entry.stock',
    color: '#13c2c2',
    url: '/pkg-wms/stock/index',
    perm: 'wms:stock:list',
    links: ['/wms/stock']
  },
  {
    // 积压分析：截至某日库存超过积压基数（库存上限 / 上月销量 / 近 N 天销量）的商品，与 Web 端进销存报表相同的接口
    key: 'wms-overstock',
    group: 'warehouse',
    title: 'wms.entry.overstock',
    color: '#d48806',
    url: '/pkg-wms/overstock/index',
    perm: 'bi:jxc:overstock',
    links: ['/scm/jxc-overstock']
  },
  {
    key: 'wms-receive',
    group: 'warehouse',
    title: 'wms.entry.receive',
    color: '#1677ff',
    url: '/pkg-wms/receive/index',
    perm: 'scm:purchaseIn:add',
    links: ['/scm/purchase-in']
  },
  {
    key: 'wms-putaway',
    group: 'warehouse',
    title: 'wms.entry.putaway',
    color: '#722ed1',
    url: '/pkg-wms/putaway/index',
    perm: 'wms:transfer:add'
  },
  {
    key: 'wms-pick',
    group: 'warehouse',
    title: 'wms.entry.pick',
    color: '#fa8c16',
    url: '/pkg-wms/pick/index',
    perm: 'scm:delivery:pick',
    links: ['/scm/delivery']
  },
  {
    key: 'wms-check',
    group: 'warehouse',
    title: 'wms.entry.check',
    color: '#52c41a',
    url: '/pkg-wms/check/index',
    perm: 'wms:check:edit',
    links: ['/wms/check']
  }
]
