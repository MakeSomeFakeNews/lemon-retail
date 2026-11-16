/** 门店要货与收货（dist.xxx） */
export default {
  entry: {
    requisition: '门店要货',
    receive: '门店收货'
  },
  requisition: {
    add: '新增要货',
    edit: '编辑要货单',
    warehouse: '收货仓库',
    bizDate: '要货日期',
    expectedDate: '期望到货',
    qty: '要货数量',
    stock: '现存 {n}',
    transit: '在途 {n}',
    suggest: '建议 {n}',
    supply: '供货可用 {n}',
    approved: '批准 {n}',
    delivered: '已配送 {n}',
    addMaterial: '添加商品',
    scanTip: '扫描商品条码或输入编码',
    useSuggest: '按建议量',
    saveDraft: '保存草稿',
    saveSubmit: '保存并提交',
    submitConfirm: '提交后不能修改，确定提交？',
    noItems: '请添加商品',
    noQty: '请填写要货数量',
    items: '{n} 种商品',
    rejectReason: '驳回原因：{reason}',
    notFound: '未找到商品 {code}'
  },
  receive: {
    tip: '本店待收货的配送单',
    from: '发货：{name}',
    shipped: '发货 {n}',
    received: '实收',
    damaged: '破损',
    reason: '差异说明',
    receiveDate: '收货日期',
    confirm: '确认收货',
    confirmDiff: '有 {n} 行实收与发货不一致，将生成收货差异，确定收货？',
    confirmAll: '确定按发货数量收货？',
    serialTip: '序列号商品须按发货数量收货',
    done: '已收货'
  }
}
