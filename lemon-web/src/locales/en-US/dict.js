// 字典标签英文翻译：dict_type -> { dict_value: 英文 }（与 sql/init.sql 一致），中文直接使用数据库中的标签
export default {
  // 通用状态
  sys_common_status: {
    '1': 'Enabled', // 启用
    '0': 'Disabled', // 停用
  },
  // 显示状态
  sys_show_hide: {
    '1': 'Visible', // 显示
    '0': 'Hidden', // 隐藏
  },
  // 菜单类型
  sys_menu_type: {
    'M': 'Directory', // 目录
    'C': 'Menu', // 菜单
    'F': 'Button', // 按钮
  },
  // 操作类型
  sys_oper_type: {
    '0': 'Other', // 其他
    '1': 'Create', // 新增
    '2': 'Update', // 修改
    '3': 'Delete', // 删除
    '4': 'Grant', // 授权
    '5': 'Export', // 导出
    '6': 'Force Logout', // 强退
    '7': 'Clear', // 清空
    '8': 'Import', // 导入
  },
  // 成功失败
  sys_success_fail: {
    '1': 'Success', // 成功
    '0': 'Failed', // 失败
  },
  // 数据权限
  sys_data_scope: {
    '1': 'All Data', // 全部数据
    '2': 'Custom', // 自定义数据
    '3': 'Own Department', // 本部门数据
    '4': 'Own Department & Below', // 本部门及以下数据
    '5': 'Self Only', // 仅本人数据
  },
  // 流程分类
  wf_category: {
    'PURCHASE': 'Purchasing', // 采购
    'SALES': 'Sales', // 销售
    'INVENTORY': 'Inventory', // 库存
    'FINANCE': 'Finance', // 财务
    'OTHER': 'Other', // 其他
    'ADMIN': 'Administration', // 行政
  },
  // 流程状态
  wf_status: {
    'RUNNING': 'In Approval', // 审批中
    'APPROVED': 'Approved', // 已通过
    'REJECTED': 'Rejected', // 已拒绝
    'CANCELED': 'Withdrawn', // 已撤销
  },
  // 审批动作
  wf_task_action: {
    'APPROVE': 'Approve', // 同意
    'REJECT': 'Reject', // 拒绝
    'TRANSFER': 'Transfer', // 转交
  },
  // 商品品牌
  wms_material_category: {
    'RAW': 'Raw Material', // 原材料
    'SEMI': 'Semi-finished', // 半成品
    'FINISHED': 'Finished Goods', // 成品
    'PACKING': 'Packaging', // 包装材料
    'OTHER': 'Other', // 其他
    'LOW_VALUE': 'Low-value Consumable', // 低值易耗品
  },
  wms_material_brand: {
    'OWN': 'Own Brand', // 自有品牌
    'OTHER': 'Other', // 其他
  },
  // 发票类型（开票类型，扩展值为税率）
  scm_invoice_kind: {
    'SPECIAL_13': 'VAT Special Invoice 13%', // 增值税专票 13%
    'SPECIAL_9': 'VAT Special Invoice 9%', // 增值税专票 9%
    'SPECIAL_6': 'VAT Special Invoice 6%', // 增值税专票 6%
    'NORMAL_13': 'VAT General Invoice 13%', // 增值税普票 13%
    'NORMAL_3': 'VAT General Invoice 3%', // 增值税普票 3%
    'NORMAL_1': 'VAT General Invoice 1%', // 增值税普票 1%
    'NONE': 'No Invoice', // 不开票
  },
  // 客户区域
  scm_region: {
    'EAST': 'East China', // 华东
    'SOUTH': 'South China', // 华南
    'NORTH': 'North China', // 华北
    'CENTRAL': 'Central China', // 华中
    'SOUTHWEST': 'Southwest China', // 西南
    'NORTHWEST': 'Northwest China', // 西北
    'NORTHEAST': 'Northeast China', // 东北
    'OVERSEAS': 'Overseas', // 海外
  },
  // 物料分类
  // 入库类型
  wms_in_type: {
    'PURCHASE': 'Purchase Receipt', // 采购入库
    'SALES_RETURN': 'Sales Return', // 销售退货
    'OTHER': 'Other Inbound', // 其他入库
    'CONSIGN_IN': 'Consignment Inbound', // 寄售入库
    'OPENING': 'Opening Inbound', // 期初入库
    'ASSEMBLY': 'Assembly/Disassembly', // 组装拆卸
  },
  // 出库类型
  // 入库类别（业务字典）
  wms_in_category: {
    SAMPLE_RETURN: 'Sample Return', // 样品退回
    GIFT: 'Gift Received', // 受赠入库
    USE_RETURN: 'Usage Return', // 领用退回
    OTHER: 'Other' // 其他
  },
  // 出库类别（业务字典）
  wms_out_category: {
    SAMPLE: 'Samples', // 样品出库
    GIFT: 'Giveaways', // 赠品出库
    WELFARE: 'Employee Welfare', // 福利领用
    RD: 'R&D Use', // 研发领用
    OFFICE: 'Office Use', // 办公领用
    OTHER: 'Other' // 其他
  },
  wms_out_type: {
    'SALES': 'Sales Delivery', // 销售出库
    'PURCHASE_RETURN': 'Purchase Return', // 采购退货
    'OTHER': 'Other Outbound', // 其他出库
    'SCRAP': 'Scrap Outbound', // 报废出库
    'ASSEMBLY': 'Assembly/Disassembly', // 组装拆卸
  },
  // 单据状态
  wms_doc_status: {
    'DRAFT': 'Draft', // 草稿
    'DONE': 'Completed', // 已完成
    'TRANSIT': 'In Transit', // 在途
    'APPROVING': 'In Approval', // 审批中
  },
  // 库存业务类型
  wms_biz_type: {
    'IN': 'Inbound', // 入库
    'OUT': 'Outbound', // 出库
    'TRANSFER': 'Transfer', // 调拨
    'CHECK': 'Stock Count', // 盘点
  },
  // 序列号状态
  wms_serial_status: {
    'IN_STOCK': 'In Stock', // 在库
    'OUT': 'Shipped Out', // 已出库
  },
  // 供应商等级
  scm_supplier_level: {
    'A': 'A - Excellent', // A 优秀
    'B': 'B - Good', // B 良好
    'C': 'C - Qualified', // C 合格
    'D': 'D - Needs Improvement', // D 待改进
  },
  // 客户等级
  scm_customer_level: {
    'VIP': 'Key Account', // 重点客户
    'NORMAL': 'Regular', // 普通客户
    'POTENTIAL': 'Prospect', // 潜在客户
  },
  // 订单状态
  scm_order_status: {
    'DRAFT': 'Draft', // 草稿
    'APPROVING': 'In Approval', // 审批中
    'APPROVED': 'Approved', // 已审核
    'PARTIAL': 'Partially Completed', // 部分完成
    'COMPLETED': 'Completed', // 已完成
    'CLOSED': 'Closed', // 已关闭
  },
  // 出入库种类
  scm_trade_kind: {
    'PURCHASE_IN': 'Purchase Receipt', // 采购入库
    'PURCHASE_RETURN': 'Purchase Return', // 采购退货
    'SALES_OUT': 'Sales Delivery', // 销售出库
    'SALES_RETURN': 'Sales Return', // 销售退货
  },
  // 订单来源
  scm_order_source: {
    'MANUAL': 'Manual', // 手工
    'REORDER': 'Replenishment Suggestion', // 补货建议
    'DIRECT': 'Direct Document', // 直接开单
  },
  // 科目类别
  fin_account_category: {
    'ASSET': 'Assets', // 资产
    'LIABILITY': 'Liabilities', // 负债
    'EQUITY': 'Owners\' Equity', // 所有者权益
    'COST': 'Costs', // 成本
    'PROFIT': 'Profit & Loss', // 损益
  },
  // 凭证状态
  fin_voucher_status: {
    'DRAFT': 'Unapproved', // 未审核
    'AUDITED': 'Approved', // 已审核
    'POSTED': 'Posted', // 已记账
    'APPROVING': 'In Approval', // 审批中
  },
  // 凭证来源
  fin_voucher_source: {
    'MANUAL': 'Manual', // 手工
    'AR': 'Receivable', // 应收
    'AP': 'Payable', // 应付
    'RECEIPT': 'Receipt', // 收款
    'PAYMENT': 'Payment', // 付款
    'CLOSE': 'P&L Carry-forward', // 结转损益
    'ADVANCE': 'Advances', // 预收预付
    'BAD_DEBT': 'Bad Debt', // 坏账
    'REVERSAL': 'Reversal', // 红字冲销
    'TAX': 'Taxes', // 税费
    'FX': 'Exchange Gain/Loss', // 汇兑
    'TRANSFER': 'Account Transfer', // 账户调拨
    'INCOME_EXPENSE': 'Other Income / Expense', // 其他收支
    'NETTING': 'AR/AP Netting', // 应收应付对冲
  },
  // 核销状态
  fin_bill_status: {
    'UNSETTLED': 'Unsettled', // 未核销
    'PARTIAL': 'Partially Settled', // 部分核销
    'SETTLED': 'Settled', // 已核销
  },
  // 应收应付来源
  fin_bill_source: {
    'SALES_OUT': 'Sales Delivery', // 销售出库
    'SALES_RETURN': 'Sales Return', // 销售退货
    'PURCHASE_IN': 'Purchase Receipt', // 采购入库
    'PURCHASE_RETURN': 'Purchase Return', // 采购退货
    'MANUAL': 'Opening / Manual', // 期初 / 手工
    'OFFSET': 'Offset', // 冲账单
    'ORDER_ADJUST': 'Order Rounding Adjustment', // 订单尾差调整
    'PURCHASE_FREIGHT': 'Purchase Freight', // 采购运费
    'SALES_FREIGHT': 'Sales Freight', // 销售运费
    'SALES_RETURN_FREIGHT': 'Sales Return Freight', // 销售退货运费
    'PURCHASE_RETURN_FREIGHT': 'Purchase Return Freight', // 采购退货运费
    'TRANSFER_FEE': 'Transfer Fee', // 调拨费用
  },
  // 运费承担
  scm_freight_bearer: {
    'COMPANY': 'Company Pays', // 本公司承担
    'CUSTOMER': 'Customer Pays (Advanced)', // 客户承担（代垫）
    'SUPPLIER': 'Advanced by Supplier', // 供应商代垫
  },
  // 运费结算方式
  scm_freight_settle: {
    'CASH': 'Cash', // 现付
    'MONTHLY': 'Monthly', // 月结
  },
  // 冲账项目
  fin_offset_item: {
    'FREIGHT': 'Freight Paid on Behalf', // 垫付运费
    'REBATE': 'Sales Rebate', // 销售返利
    'WARRANTY': 'Warranty Compensation', // 三包赔偿
    'OTHER': 'Other', // 其他
  },
  // 冲账单状态
  fin_offset_status: {
    'DRAFT': 'Pending Review', // 待复核
    'REVIEWED': 'Reviewed', // 已复核
  },
  // 结算方式
  fin_settle_method: {
    'TRANSFER': 'Bank Transfer', // 银行转账
    'CASH': 'Cash', // 现金
    'DRAFT': 'Acceptance Bill', // 承兑汇票
    'ONLINE': 'Alipay / WeChat Pay', // 支付宝 / 微信
  },
  // 同步状态
  bi_etl_status: {
    'RUNNING': 'Running', // 运行中
    'SUCCESS': 'Success', // 成功
    'FAILED': 'Failed', // 失败
  },
  // 预警级别
  bi_alert_level: {
    'INFO': 'Info', // 提示
    'WARN': 'Warning', // 警告
    'DANGER': 'Critical', // 严重
  },
  // 消息类别
  sys_message_category: {
    'APPROVAL': 'Approval', // 审批
    'REMIND': 'Reminder', // 提醒
    'ALERT': 'Alert', // 预警
    'SYSTEM': 'System', // 系统
    'NOTICE': 'Notice', // 公告
  },
  // 审批状态
  sys_approve_status: {
    'APPROVED': 'Effective', // 已生效
    'APPROVING': 'In Approval', // 审批中
    'REJECTED': 'Rejected', // 已拒绝
    'CANCELED': 'Withdrawn', // 已撤回
    'DONE': 'Effective', // 已生效
    'DRAFT': 'Draft', // 草稿
  },
  // 冻结状态
  // 发货通知状态
  scm_delivery_status: {
    'PENDING': 'To Pick', // 待拣货
    'PICKED': 'Picked', // 已拣货
    'SHIPPED': 'Shipped', // 已出库
    'CANCELLED': 'Canceled', // 已取消
  },
  // 库存预警方式
  wms_stock_alert_mode: {
    'TOTAL': 'By Total Stock', // 按总库存
    'WAREHOUSE': 'Per Warehouse', // 分仓库
  },
  // 仓库类型
  wms_warehouse_type: {
    'NORMAL': 'General', // 普通仓
    'RAW': 'Raw Materials', // 原料仓
    'FINISHED': 'Finished Goods', // 成品仓
    'DEFECT': 'Defective Goods', // 不良品仓
    'CONSIGN_OUT': 'Customer Consignment', // 客户寄售仓
    'CONSIGN_IN': 'Supplier Consignment', // 供应商寄售仓
  },
  // 发票类型
  fin_invoice_type: {
    'SPECIAL': 'Special VAT Invoice', // 增值税专用发票
    'NORMAL': 'General VAT Invoice', // 普通发票
    'E_SPECIAL': 'Digital Invoice (Special)', // 数电票（专票）
    'E_NORMAL': 'Digital Invoice (General)', // 数电票（普票）
  },
  // 变更单状态
  scm_change_status: {
    'APPROVING': 'In Approval', // 审批中
    'APPROVED': 'Effective', // 已生效
    'REJECTED': 'Rejected', // 已驳回
  },
  // 促销类型
  scm_promo_type: {
    'DISCOUNT': 'Discount', // 折扣
    'REDUCE': 'Spend & Save', // 满减
    'GIFT': 'Buy & Get Free', // 买赠
  },
  // 收支方向
  fin_fund_direction: {
    'IN': 'Income', // 收入
    'OUT': 'Expense', // 支出
  },
  // 收支单状态
  fin_io_status: {
    'DRAFT': 'Unconfirmed', // 待确认
    'CONFIRMED': 'Confirmed', // 已确认
  },
  // 关联单位类型
  fin_io_partner_type: {
    'CUSTOMER': 'Customer', // 客户
    'SUPPLIER': 'Supplier', // 供应商
    'OTHER': 'Other', // 其他
  },
  // 手续费支付账户
  fin_fee_bearer: {
    'FROM': 'Paid by Source Account', // 转出账户支付
    'TO': 'Paid by Receiving Account', // 转入账户支付
  },
  // 销售渠道
  scm_channel: {
    'DIRECT': 'Direct', // 直销
    'DEALER': 'Dealer', // 经销商
  },
  // 库位类型
  wms_location_type: {
    'STORAGE': 'Storage', // 存储位
    'PICK': 'Picking', // 拣货位
    'RECEIVE': 'Receiving Staging', // 收货暂存
    'SHIP': 'Shipping Staging', // 发货暂存
    'DEFECT': 'Defective', // 不良品位
  },
  // 组装拆卸种类
  wms_assembly_kind: {
    'ASSEMBLE': 'Assembly', // 组装
    'DISASSEMBLE': 'Disassembly', // 拆卸
  },
  // 提成依据
  fin_commission_basis: {
    'SALES': 'Sales Amount', // 销售额
    'PROFIT': 'Gross Profit', // 毛利
    'RECEIPT': 'Collections', // 回款
  },
  // 价格控制方式
  scm_price_control_mode: {
    'WARN': 'Warn', // 提示
    'BLOCK': 'Block Saving', // 禁止保存
  },
  // 自定义字段类型
  sys_custom_field_type: {
    'TEXT': 'Text', // 文本
    'TEXTAREA': 'Multi-line Text', // 多行文本
    'NUMBER': 'Number', // 数字
    'DATE': 'Date', // 日期
    'SELECT': 'Dropdown', // 下拉选择
    'BOOL': 'Yes/No', // 是否
  },
  // 集成平台
  sys_im_platform: {
    'DINGTALK': 'DingTalk', // 钉钉
    'WECOM': 'WeCom', // 企业微信
    'FEISHU': 'Feishu (Lark)', // 飞书
  },
  // 公告状态
  sys_notice_status: {
    'DRAFT': 'Draft', // 草稿
    'PUBLISHED': 'Published', // 已发布
    'WITHDRAWN': 'Withdrawn', // 已撤回
  },
  // 公告发布范围
  sys_notice_scope: {
    'ALL': 'Everyone', // 全员
    'DEPT': 'Departments', // 指定部门
    'ROLE': 'Roles', // 指定角色
  },
  // 公告类型
  sys_notice_type: {
    'NOTICE': 'Notice', // 通知
    'ANNOUNCE': 'Announcement', // 公告
    'POLICY': 'Policy', // 制度
    'EVENT': 'Event', // 活动
  },
  // 门店类型
  store_type: {
    'HQ': 'Headquarters', // 总部
    'DIRECT': 'Direct Store', // 直营店
    'FRANCHISE': 'Franchise Store', // 加盟店
    'DC': 'Distribution Center', // 配送中心
  },
  // 门店状态
  store_status: {
    'PREPARING': 'Preparing', // 筹备
    'OPEN': 'Open', // 营业
    'SUSPENDED': 'Suspended', // 停业
    'CLOSED': 'Closed', // 闭店
  },
  // 加盟结算方式
  store_settle_mode: {
    'PREPAID': 'Prepaid', // 预付货款
    'CREDIT': 'Monthly Credit', // 月结
  },
  // 门店范围
  sys_store_scope: {
    'ALL': 'All Stores', // 全部门店
    'ASSIGNED': 'Assigned Stores', // 指定门店
  },
  // 调价单状态
  scm_price_adjust_status: {
    'DRAFT': 'Draft', // 草稿
    'APPROVING': 'In Approval', // 审批中
    'APPROVED': 'Pending Effect', // 待生效
    'EFFECTIVE': 'Effective', // 已生效
    'VOIDED': 'Voided', // 已作废
  },
  // 调价对象
  scm_price_adjust_target: {
    'HQ': 'HQ Price', // 总部价
    'SCHEME': 'Price Scheme', // 价格方案
  },
}
