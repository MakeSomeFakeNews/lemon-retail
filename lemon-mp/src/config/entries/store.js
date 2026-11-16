/** 工作台入口（见 index.js）：店长工作台（门店看板 + 本店库存、盘点、要货、收货、审批的快捷入口，页面在 pkg-store 分包） */
export default [
  {
    // 店长工作台：今天 / 昨天的 POS 销售、单数、客单价、毛利（有成本权限时）、畅销商品，待收货、草稿要货
    key: 'store-workbench',
    group: 'store',
    title: 'store.entry.workbench',
    color: '#13a8a8',
    url: '/pkg-store/index',
    perm: ['pos:sale:list', 'chain:pnl:list'],
    links: ['/chain/store-pnl']
  }
]
