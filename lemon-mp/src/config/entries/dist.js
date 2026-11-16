/** 工作台入口（见 index.js）：店长的门店要货、门店收货（页面在 pkg-wms 分包，复用其搜索弹层和扫码工具） */
export default [
  {
    // 门店要货：为本店录入要货（显示现存、在途、建议量），保存草稿或提交
    key: 'dist-requisition',
    group: 'warehouse',
    title: 'dist.entry.requisition',
    color: '#722ed1',
    url: '/pkg-wms/requisition/index',
    perm: 'dist:requisition:list',
    links: ['/dist/requisition']
  },
  {
    // 门店收货：本店待收货的配送单，逐行确认实收数量，差异生成收货差异
    key: 'dist-receive',
    group: 'warehouse',
    title: 'dist.entry.receive',
    color: '#08979c',
    url: '/pkg-wms/dist-receive/index',
    perm: 'dist:receive:list',
    permAll: ['dist:receive:confirm'],
    links: ['/dist/receive']
  }
]
