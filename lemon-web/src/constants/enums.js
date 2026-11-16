/**
 * 后端枚举常量 —— 由 scripts/gen-enums.mjs 生成，请勿手工修改；后端新增或修改枚举后运行 npm run gen:enums
 *
 * 来源：lemon-common、lemon-framework、lemon-modules 中文件名与枚举同名的顶层 Java 枚举，键和值都是枚举常量名。
 * 命名：Java 简单类名；多个模块有同名枚举时加模块前缀（如 WmsDocStatus、MesDocStatus）。
 * 用法：import { WorkOrderStatus } from '@/constants/enums'；record.status === WorkOrderStatus.DRAFT
 */

/** com.lemon.erp.modules.bi.model.AbcClass */
export const AbcClass = Object.freeze({
  A: 'A', // 累计占比 70% 以内
  B: 'B', // 累计占比 90% 以内
  C: 'C', // 其余
})

/** com.lemon.erp.modules.fin.model.AccountCategory（字典 fin_account_category） */
export const AccountCategory = Object.freeze({
  ASSET: 'ASSET',
  LIABILITY: 'LIABILITY',
  EQUITY: 'EQUITY',
  COST: 'COST',
  PROFIT: 'PROFIT', // 损益类
})

/** com.lemon.erp.modules.fin.model.AccountDirection */
export const AccountDirection = Object.freeze({
  DEBIT: 'DEBIT',
  CREDIT: 'CREDIT',
})

/** com.lemon.erp.modules.fin.model.AccountRole */
export const AccountRole = Object.freeze({
  CASH: 'CASH', // 库存现金
  BANK: 'BANK', // 银行存款
  RECEIVABLE: 'RECEIVABLE',
  PAYABLE: 'PAYABLE',
  ADVANCE_RECEIVED: 'ADVANCE_RECEIVED',
  PREPAYMENT: 'PREPAYMENT',
  FREIGHT_ADVANCE: 'FREIGHT_ADVANCE', // 代垫运费
  INVENTORY: 'INVENTORY', // 默认
  REVENUE: 'REVENUE',
  OTHER_REVENUE: 'OTHER_REVENUE',
  SELLING_EXPENSE: 'SELLING_EXPENSE',
  SALES_FREIGHT: 'SALES_FREIGHT', // 销售运费
  PURCHASE_FREIGHT: 'PURCHASE_FREIGHT', // 采购运费
  TRANSFER_FEE: 'TRANSFER_FEE', // 调拨费用
  FINANCE_EXPENSE: 'FINANCE_EXPENSE',
  NON_OPERATING_INCOME: 'NON_OPERATING_INCOME',
  NON_OPERATING_EXPENSE: 'NON_OPERATING_EXPENSE',
  RECEIPT_DISCOUNT: 'RECEIPT_DISCOUNT', // 收款折让 / 抹零
  PAYMENT_DISCOUNT: 'PAYMENT_DISCOUNT', // 付款折让 / 抹零
  OFFSET_AR: 'OFFSET_AR', // 应收冲账
  OFFSET_AP: 'OFFSET_AP', // 应付冲账
  INPUT_VAT: 'INPUT_VAT', // 采购返利
  OUTPUT_VAT: 'OUTPUT_VAT',
  UNPAID_VAT: 'UNPAID_VAT',
  CITY_TAX: 'CITY_TAX',
  EDU_SURCHARGE: 'EDU_SURCHARGE',
  LOCAL_EDU_SURCHARGE: 'LOCAL_EDU_SURCHARGE',
  TAX_SURCHARGE: 'TAX_SURCHARGE',
  PROFIT_THIS_YEAR: 'PROFIT_THIS_YEAR',
  PROFIT_DISTRIBUTION: 'PROFIT_DISTRIBUTION',
})

/** com.lemon.erp.modules.fin.model.AdjustItem */
export const AdjustItem = Object.freeze({
  DISCOUNT: 'DISCOUNT', // 整单优惠
  FREIGHT: 'FREIGHT', // 本公司承担的运费
  SETTLE_DISCOUNT: 'SETTLE_DISCOUNT', // 收款折让 / 抹零
  BAD_DEBT: 'BAD_DEBT', // 坏账核销
})

/** com.lemon.erp.modules.fin.model.AdjustMode */
export const AdjustMode = Object.freeze({
  BOTTOM: 'BOTTOM', // 汇总到底部
  ALLOCATE: 'ALLOCATE', // 分摊到各行
})

/** com.lemon.erp.framework.ai.AiCleanseField */
export const AiCleanseField = Object.freeze({
  NAME: 'NAME', // 名称
  SPEC: 'SPEC', // 规格型号
  CATEGORY: 'CATEGORY', // 分类
  UNIT: 'UNIT', // 计量单位
  CONTACT: 'CONTACT', // 联系人
  ADDRESS: 'ADDRESS', // 地址
})

/** com.lemon.erp.modules.ai.model.AiCleanseKind */
export const AiCleanseKind = Object.freeze({
  DUPLICATE: 'DUPLICATE', // 疑似重复
  NORMALIZE: 'NORMALIZE', // 规范化
  MISSING: 'MISSING', // 缺失字段提示
})

/** com.lemon.erp.modules.ai.model.AiCleansePhase */
export const AiCleansePhase = Object.freeze({
  LOADING: 'LOADING', // 读取记录、程序分组
  DUPLICATE: 'DUPLICATE', // 模型判断疑似重复
  NORMALIZE: 'NORMALIZE', // 模型给出规范化建议
  DONE: 'DONE', // 结束
})

/** com.lemon.erp.modules.ai.model.AiCleanseTaskStatus */
export const AiCleanseTaskStatus = Object.freeze({
  RUNNING: 'RUNNING', // 进行中
  COMPLETED: 'COMPLETED', // 已完成
  FAILED: 'FAILED', // 失败
  CANCELLED: 'CANCELLED', // 已取消
})

/** com.lemon.erp.modules.ai.model.AiFeature */
export const AiFeature = Object.freeze({
  CHAT: 'CHAT', // AI 问数
  EXTRACT: 'EXTRACT', // 智能录入
  CLEANSE: 'CLEANSE', // 数据整理
  REPORT: 'REPORT', // 智能报告
  TEST: 'TEST', // 「AI 设置」中的测试连接
})

/** com.lemon.erp.modules.ai.model.AiMessageRole */
export const AiMessageRole = Object.freeze({
  USER: 'USER', // 用户的提问
  ASSISTANT: 'ASSISTANT', // 模型的回答
  TOOL: 'TOOL', // 工具的返回结果
})

/** com.lemon.erp.modules.ai.model.AiProvider */
export const AiProvider = Object.freeze({
  DEEPSEEK: 'DEEPSEEK', // DeepSeek
  QWEN: 'QWEN', // 通义千问
  ZHIPU: 'ZHIPU', // 智谱 GLM
  MOONSHOT: 'MOONSHOT', // Kimi
  DOUBAO: 'DOUBAO', // 豆包
  CUSTOM: 'CUSTOM', // 其他兼容 OpenAI 协议的服务
})

/** com.lemon.erp.modules.ai.model.AiReportPeriodType */
export const AiReportPeriodType = Object.freeze({
  WEEK: 'WEEK',
  MONTH: 'MONTH',
})

/** com.lemon.erp.modules.ai.model.AiReportStatus */
export const AiReportStatus = Object.freeze({
  GENERATING: 'GENERATING', // 正在生成
  DONE: 'DONE', // 已完成
  FAILED: 'FAILED', // 生成失败
})

/** com.lemon.erp.modules.ai.model.AiSuggestionStatus */
export const AiSuggestionStatus = Object.freeze({
  PENDING: 'PENDING', // 待处理
  APPLIED: 'APPLIED', // 已采纳
  IGNORED: 'IGNORED', // 已忽略
})

/** com.lemon.erp.modules.bi.model.AlertLevel（字典 bi_alert_level） */
export const AlertLevel = Object.freeze({
  INFO: 'INFO', // 提示
  WARN: 'WARN', // 警告
  DANGER: 'DANGER', // 严重
})

/** com.lemon.erp.modules.bi.model.AlertOperator */
export const AlertOperator = Object.freeze({
  GT: 'GT', // 大于
  GE: 'GE', // 不小于
  LT: 'LT', // 小于
  LE: 'LE', // 不大于
})

/** com.lemon.erp.modules.workflow.model.ApprovalAction（字典 wf_task_action） */
export const ApprovalAction = Object.freeze({
  START: 'START', // 发起
  APPROVE: 'APPROVE',
  REJECT: 'REJECT',
  TRANSFER: 'TRANSFER',
  CANCEL: 'CANCEL', // 发起人撤回
  TERMINATE: 'TERMINATE', // 管理员终止
  CC: 'CC', // 抄送
  PENDING: 'PENDING', // 待审批
})

/** com.lemon.erp.modules.workflow.model.ApproveMode */
export const ApproveMode = Object.freeze({
  SEQUENTIAL: 'SEQUENTIAL', // 依次审批
  AND: 'AND', // 会签
  OR: 'OR', // 或签
})

/** com.lemon.erp.framework.workflow.ApproveStatus（字典 sys_approve_status） */
export const ApproveStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  APPROVED: 'APPROVED', // 已生效
  APPROVING: 'APPROVING', // 审批中
  REJECTED: 'REJECTED', // 已拒绝
  CANCELED: 'CANCELED', // 已撤回
  DONE: 'DONE', // 已生效
})

/** com.lemon.erp.modules.wms.model.AssemblyKind（字典 wms_assembly_kind） */
export const AssemblyKind = Object.freeze({
  ASSEMBLE: 'ASSEMBLE', // 组装
  DISASSEMBLE: 'DISASSEMBLE', // 拆卸
})

/** com.lemon.erp.modules.workflow.model.AssigneeType */
export const AssigneeType = Object.freeze({
  USER: 'USER',
  ROLE: 'ROLE',
  DEPT_LEADER: 'DEPT_LEADER', // 发起人的部门主管
  INITIATOR: 'INITIATOR',
})

/** com.lemon.erp.modules.fin.model.AuxItem */
export const AuxItem = Object.freeze({
  DEPT: 'DEPT',
  CUSTOMER: 'CUSTOMER',
  SUPPLIER: 'SUPPLIER',
  MATERIAL: 'MATERIAL',
})

/** com.lemon.erp.modules.bi.model.BiAlertMetric */
export const BiAlertMetric = Object.freeze({
  AR_OVERDUE: 'AR_OVERDUE',
  STOCK_BELOW_SAFETY: 'STOCK_BELOW_SAFETY',
  STOCK_ABOVE_MAX: 'STOCK_ABOVE_MAX',
  SLOW_MOVING: 'SLOW_MOVING',
  GROSS_MARGIN: 'GROSS_MARGIN',
})

/** com.lemon.erp.modules.bi.model.BiCube */
export const BiCube = Object.freeze({
  SALES: 'SALES',
  PURCHASE: 'PURCHASE',
  INVENTORY: 'INVENTORY',
  FINANCE: 'FINANCE',
})

/** com.lemon.erp.modules.fin.model.BillKind */
export const BillKind = Object.freeze({
  AR: 'AR', // 应收
  AP: 'AP', // 应付
})

/** com.lemon.erp.modules.fin.model.BillSource（字典 fin_bill_source） */
export const BillSource = Object.freeze({
  SALES_OUT: 'SALES_OUT', // 销售出库
  SALES_RETURN: 'SALES_RETURN', // 销售退货
  PURCHASE_IN: 'PURCHASE_IN', // 采购入库
  PURCHASE_RETURN: 'PURCHASE_RETURN', // 采购退货
  MANUAL: 'MANUAL', // 期初 / 手工
  OFFSET: 'OFFSET', // 冲账单
  ORDER_ADJUST: 'ORDER_ADJUST', // 订单尾差调整
  PURCHASE_FREIGHT: 'PURCHASE_FREIGHT', // 采购运费
  SALES_FREIGHT: 'SALES_FREIGHT', // 销售运费
  SALES_RETURN_FREIGHT: 'SALES_RETURN_FREIGHT', // 销售退货运费
  PURCHASE_RETURN_FREIGHT: 'PURCHASE_RETURN_FREIGHT', // 采购退货运费
  TRANSFER_FEE: 'TRANSFER_FEE', // 调拨费用
  POS_DAILY_CLOSE: 'POS_DAILY_CLOSE', // 门店日结
})

/** com.lemon.erp.modules.fin.model.BillStatus（字典 fin_bill_status） */
export const BillStatus = Object.freeze({
  UNSETTLED: 'UNSETTLED', // 未核销
  PARTIAL: 'PARTIAL', // 部分核销
  SETTLED: 'SETTLED', // 已核销
})

/** com.lemon.erp.framework.setting.CaptchaMode */
export const CaptchaMode = Object.freeze({
  OFF: 'OFF',
  ALWAYS: 'ALWAYS',
  AFTER_FAILURE: 'AFTER_FAILURE',
})

/** com.lemon.erp.framework.setting.CaptchaType */
export const CaptchaType = Object.freeze({
  MATH: 'MATH',
  CHAR: 'CHAR',
})

/** com.lemon.erp.modules.fin.model.CheckLevel */
export const CheckLevel = Object.freeze({
  OK: 'OK', // 通过
  WARN: 'WARN', // 提示
  ERROR: 'ERROR', // 错误
})

/** com.lemon.erp.modules.fin.model.CommissionBasis（字典 fin_commission_basis） */
export const CommissionBasis = Object.freeze({
  SALES: 'SALES', // 销售额
  PROFIT: 'PROFIT', // 毛利
  RECEIPT: 'RECEIPT', // 回款
})

/** com.lemon.erp.modules.fin.model.CommissionStatus */
export const CommissionStatus = Object.freeze({
  DRAFT: 'DRAFT',
  CONFIRMED: 'CONFIRMED',
})

/** com.lemon.erp.modules.workflow.model.ConditionOperator */
export const ConditionOperator = Object.freeze({
  EQ: 'EQ',
  NE: 'NE',
  GT: 'GT',
  GE: 'GE',
  LT: 'LT',
  LE: 'LE',
  IN: 'IN', // 属于其中之一
  CONTAINS: 'CONTAINS', // 文本包含
})

/** com.lemon.erp.modules.scm.model.CreditControl */
export const CreditControl = Object.freeze({
  ALL: 'ALL', // 审核和出库时都控制
  SHIPMENT: 'SHIPMENT', // 只在出库时控制
  NONE: 'NONE', // 不控制
})

/** com.lemon.erp.modules.system.model.CustomFieldType（字典 sys_custom_field_type） */
export const CustomFieldType = Object.freeze({
  TEXT: 'TEXT',
  TEXTAREA: 'TEXTAREA',
  NUMBER: 'NUMBER', // 数字
  DATE: 'DATE', // 日期
  SELECT: 'SELECT', // 下拉
  BOOL: 'BOOL', // 是否
})

/** com.lemon.erp.modules.pos.model.DailyCloseStatus（字典 pos_daily_close_status） */
export const DailyCloseStatus = Object.freeze({
  CLOSED: 'CLOSED',
  REOPENED: 'REOPENED',
})

/** com.lemon.erp.modules.dashboard.model.DashboardBillKind */
export const DashboardBillKind = Object.freeze({
  AR: 'AR', // 应收
  AP: 'AP', // 应付
})

/** com.lemon.erp.modules.dashboard.model.DashboardOrderKind */
export const DashboardOrderKind = Object.freeze({
  SALES: 'SALES', // 销售订单
  PURCHASE: 'PURCHASE', // 采购订单
})

/** com.lemon.erp.framework.security.datascope.DataScopeType（字典 sys_data_scope） */
export const DataScopeType = Object.freeze({
  ALL: 'ALL',
  CUSTOM: 'CUSTOM',
  DEPT: 'DEPT',
  DEPT_AND_CHILD: 'DEPT_AND_CHILD',
  SELF: 'SELF',
})

/** com.lemon.erp.modules.bi.model.DateFilter */
export const DateFilter = Object.freeze({
  DATE: 'DATE', // 按业务日期
  PERIOD: 'PERIOD', // 按月份
  SNAPSHOT: 'SNAPSHOT', // 按快照日期
})

/** com.lemon.erp.modules.bi.model.DateRange */
export const DateRange = Object.freeze({
  THIS_MONTH: 'THIS_MONTH', // 本月
  THIS_QUARTER: 'THIS_QUARTER', // 本季度
  THIS_YEAR: 'THIS_YEAR', // 本年
  LAST_12M: 'LAST_12M', // 最近 12 个月
  ALL: 'ALL', // 全部
})

/** com.lemon.erp.modules.dist.model.DeliveryKind（字典 dist_delivery_kind） */
export const DeliveryKind = Object.freeze({
  DELIVERY: 'DELIVERY', // 配送
  RETURN: 'RETURN', // 门店退仓
})

/** com.lemon.erp.modules.scm.model.DeliveryStatus（字典 scm_delivery_status） */
export const DeliveryStatus = Object.freeze({
  PENDING: 'PENDING',
  PICKED: 'PICKED',
  SHIPPED: 'SHIPPED',
  CANCELLED: 'CANCELLED',
})

/** com.lemon.erp.modules.system.model.DictKind */
export const DictKind = Object.freeze({
  SYSTEM: 'SYSTEM', // 系统字典
  BUSINESS: 'BUSINESS', // 业务字典
})

/** com.lemon.erp.modules.dist.model.DiscrepancyHandling（字典 dist_discrepancy_handling） */
export const DiscrepancyHandling = Object.freeze({
  RETURN_SOURCE: 'RETURN_SOURCE', // 退回发货方
  SOURCE_WRITE_OFF: 'SOURCE_WRITE_OFF', // 发货方承担
  TARGET_WRITE_OFF: 'TARGET_WRITE_OFF', // 收货方承担
  ACCEPT: 'ACCEPT', // 收货方留用
})

/** com.lemon.erp.modules.dist.model.DiscrepancyKind（字典 dist_discrepancy_kind） */
export const DiscrepancyKind = Object.freeze({
  SHORT: 'SHORT', // 少收
  DAMAGED: 'DAMAGED', // 破损
  OVER: 'OVER', // 多收
})

/** com.lemon.erp.modules.dist.model.DiscrepancyStatus（字典 dist_discrepancy_status） */
export const DiscrepancyStatus = Object.freeze({
  PENDING: 'PENDING', // 待处理
  HANDLED: 'HANDLED', // 已处理
})

/** com.lemon.erp.modules.dist.model.DistDeliveryStatus（字典 dist_delivery_status） */
export const DistDeliveryStatus = Object.freeze({
  DRAFT: 'DRAFT',
  SUBMITTED: 'SUBMITTED', // 待审核
  APPROVED: 'APPROVED', // 已审核
  SHIPPED: 'SHIPPED', // 已发货
  RECEIVED: 'RECEIVED', // 已收货
  DIFF: 'DIFF', // 收货有差异
  CLOSED: 'CLOSED', // 已完成
})

/** com.lemon.erp.modules.wms.model.DocStatus（字典 wms_doc_status） */
export const DocStatus = Object.freeze({
  DRAFT: 'DRAFT',
  TRANSIT: 'TRANSIT', // 在途
  APPROVING: 'APPROVING', // 审批中
  DONE: 'DONE',
})

/** com.lemon.erp.modules.workflow.model.EmptyAction */
export const EmptyAction = Object.freeze({
  AUTO_PASS: 'AUTO_PASS', // 自动通过
  TO_ADMIN: 'TO_ADMIN', // 转交给超级管理员审批
})

/** com.lemon.erp.modules.bi.model.EtlStatus（字典 bi_etl_status） */
export const EtlStatus = Object.freeze({
  RUNNING: 'RUNNING', // 运行中
  SUCCESS: 'SUCCESS', // 成功
  FAILED: 'FAILED', // 失败
})

/** com.lemon.erp.modules.bi.model.EtlTrigger */
export const EtlTrigger = Object.freeze({
  MANUAL: 'MANUAL', // 手动
  SCHEDULE: 'SCHEDULE', // 定时
})

/** com.lemon.erp.modules.fin.model.FeeBearer（字典 fin_fee_bearer） */
export const FeeBearer = Object.freeze({
  FROM: 'FROM',
  TO: 'TO',
})

/** com.lemon.erp.framework.workflow.FlowStatus（字典 wf_status） */
export const FlowStatus = Object.freeze({
  RUNNING: 'RUNNING', // 审批中
  APPROVED: 'APPROVED', // 审批通过
  REJECTED: 'REJECTED', // 审批拒绝
  CANCELED: 'CANCELED', // 发起人撤回或管理员终止
})

/** com.lemon.erp.modules.workflow.model.FormFieldType */
export const FormFieldType = Object.freeze({
  INPUT: 'INPUT',
  TEXTAREA: 'TEXTAREA',
  NUMBER: 'NUMBER', // 整数保存为 Long
  DATE: 'DATE', // yyyy-MM-dd
  SELECT: 'SELECT',
})

/** com.lemon.erp.modules.store.model.FranchiseSettleMode（字典 store_settle_mode） */
export const FranchiseSettleMode = Object.freeze({
  PREPAID: 'PREPAID', // 预付货款
  CREDIT: 'CREDIT', // 月结
})

/** com.lemon.erp.modules.scm.model.FreightBearer（字典 scm_freight_bearer） */
export const FreightBearer = Object.freeze({
  COMPANY: 'COMPANY', // 本公司承担
  CUSTOMER: 'CUSTOMER', // 客户一方
  SUPPLIER: 'SUPPLIER', // 供应商一方
})

/** com.lemon.erp.modules.scm.model.FreightSettle（字典 scm_freight_settle） */
export const FreightSettle = Object.freeze({
  CASH: 'CASH', // 现付
  MONTHLY: 'MONTHLY', // 月结
})

/** com.lemon.erp.modules.fin.model.FundDirection（字典 fin_fund_direction） */
export const FundDirection = Object.freeze({
  IN: 'IN',
  OUT: 'OUT',
})

/** com.lemon.erp.modules.fin.model.FundSource */
export const FundSource = Object.freeze({
  AR: 'AR', // 未核销的应收单
  AP: 'AP', // 未核销的应付单
  NOTE_IN: 'NOTE_IN', // 持有中的应收票据到期
  NOTE_OUT: 'NOTE_OUT', // 未兑付的应付票据到期
  CLAIM: 'CLAIM', // 已审批待付款的报销
  PLAN: 'PLAN', // 手工计划项
})

/** com.lemon.erp.modules.bi.model.GoodsRankBy */
export const GoodsRankBy = Object.freeze({
  QTY: 'QTY', // 净销量
  AMOUNT: 'AMOUNT', // 净销售额
  PROFIT: 'PROFIT', // 毛利
})

/** com.lemon.erp.modules.system.im.ImPlatform（字典 sys_im_platform） */
export const ImPlatform = Object.freeze({
  DINGTALK: 'DINGTALK',
  WECOM: 'WECOM',
  FEISHU: 'FEISHU',
})

/** com.lemon.erp.modules.fin.model.InvoiceKind */
export const InvoiceKind = Object.freeze({
  OUTPUT: 'OUTPUT', // 销项发票
  INPUT: 'INPUT', // 进项发票
})

/** com.lemon.erp.modules.fin.model.InvoiceSource */
export const InvoiceSource = Object.freeze({
  MANUAL: 'MANUAL', // 手工登记
  XML: 'XML', // 导入电子发票 XML
  EXCEL: 'EXCEL', // Excel 批量导入
})

/** com.lemon.erp.modules.fin.model.InvoiceStatus */
export const InvoiceStatus = Object.freeze({
  VALID: 'VALID', // 有效
  VOID: 'VOID', // 已作废
})

/** com.lemon.erp.modules.fin.model.InvoiceType（字典 fin_invoice_type） */
export const InvoiceType = Object.freeze({
  SPECIAL: 'SPECIAL', // 增值税专用发票
  NORMAL: 'NORMAL', // 普通发票
  E_SPECIAL: 'E_SPECIAL', // 数电票
  E_NORMAL: 'E_NORMAL', // 数电票
})

/** com.lemon.erp.modules.fin.model.IoCategoryCode */
export const IoCategoryCode = Object.freeze({
  SALES_FREIGHT: 'SALES_FREIGHT', // 销售运费
  FREIGHT_ADVANCE: 'FREIGHT_ADVANCE', // 代垫运费
  TRANSFER_FEE: 'TRANSFER_FEE', // 调拨费用
  RETAIL_SALES: 'RETAIL_SALES', // 零售收入
  RETAIL_REFUND: 'RETAIL_REFUND', // 零售退款
})

/** com.lemon.erp.modules.fin.model.IoPartnerType（字典 fin_io_partner_type） */
export const IoPartnerType = Object.freeze({
  CUSTOMER: 'CUSTOMER',
  SUPPLIER: 'SUPPLIER',
  OTHER: 'OTHER',
})

/** com.lemon.erp.modules.fin.model.IoStatus（字典 fin_io_status） */
export const IoStatus = Object.freeze({
  DRAFT: 'DRAFT',
  CONFIRMED: 'CONFIRMED',
})

/** com.lemon.erp.modules.system.model.JobRunStatus */
export const JobRunStatus = Object.freeze({
  SUCCESS: 'SUCCESS', // 成功
  FAILED: 'FAILED', // 失败
})

/** com.lemon.erp.modules.system.model.JobTrigger */
export const JobTrigger = Object.freeze({
  SCHEDULE: 'SCHEDULE', // 按 Cron 表达式定时触发
  MANUAL: 'MANUAL', // 手动立即执行
})

/** com.lemon.erp.modules.bi.model.JxcDimension */
export const JxcDimension = Object.freeze({
  CATEGORY: 'CATEGORY', // 物料分类
  BRAND: 'BRAND', // 品牌
  REGION: 'REGION', // 客户区域
  CUSTOMER: 'CUSTOMER', // 客户
  SALESMAN: 'SALESMAN', // 业务员
  SUPPLIER: 'SUPPLIER', // 供应商
  STORE: 'STORE', // 门店
  MATERIAL: 'MATERIAL', // 商品
  DAY: 'DAY', // 日
  MONTH: 'MONTH', // 月
})

/** com.lemon.erp.modules.fin.model.LedgerRowType */
export const LedgerRowType = Object.freeze({
  OPEN: 'OPEN', // 期初余额
  ENTRY: 'ENTRY', // 凭证分录
  DAY: 'DAY', // 本日合计
  TOTAL: 'TOTAL', // 本期合计
})

/** com.lemon.erp.modules.wms.model.LocationType（字典 wms_location_type） */
export const LocationType = Object.freeze({
  STORAGE: 'STORAGE',
  PICK: 'PICK',
  RECEIVE: 'RECEIVE',
  SHIP: 'SHIP',
  DEFECT: 'DEFECT',
})

/** com.lemon.erp.modules.fin.model.MarginGroup */
export const MarginGroup = Object.freeze({
  MATERIAL: 'MATERIAL',
  CUSTOMER: 'CUSTOMER',
  MONTH: 'MONTH',
})

/** com.lemon.erp.modules.bi.model.MeasureFormat */
export const MeasureFormat = Object.freeze({
  MONEY: 'MONEY', // 金额
  QTY: 'QTY', // 数量
  COUNT: 'COUNT', // 计数
  PERCENT: 'PERCENT', // 百分比
  PRICE: 'PRICE', // 单价
})

/** com.lemon.erp.modules.pos.model.MemberGender（字典 pos_member_gender） */
export const MemberGender = Object.freeze({
  MALE: 'MALE',
  FEMALE: 'FEMALE',
  UNKNOWN: 'UNKNOWN',
})

/** com.lemon.erp.modules.system.model.MenuType（字典 sys_menu_type） */
export const MenuType = Object.freeze({
  M: 'M', // 目录
  C: 'C', // 菜单
  F: 'F', // 按钮
})

/** com.lemon.erp.framework.message.MessageCategory（字典 sys_message_category） */
export const MessageCategory = Object.freeze({
  APPROVAL: 'APPROVAL', // 审批
  REMIND: 'REMIND', // 提醒
  ALERT: 'ALERT', // 预警
  SYSTEM: 'SYSTEM', // 系统
  NOTICE: 'NOTICE', // 公告
})

/** com.lemon.erp.modules.workflow.model.NodeStatus */
export const NodeStatus = Object.freeze({
  DONE: 'DONE', // 已完成
  RUNNING: 'RUNNING', // 进行中
  REJECTED: 'REJECTED', // 已拒绝
})

/** com.lemon.erp.modules.workflow.model.NodeType */
export const NodeType = Object.freeze({
  START: 'START', // 发起人
  APPROVER: 'APPROVER', // 审批人
  CC: 'CC', // 抄送人
  CONDITION: 'CONDITION', // 条件分支
  PARALLEL: 'PARALLEL', // 并行分支
})

/** com.lemon.erp.modules.system.model.NoticeScope（字典 sys_notice_scope） */
export const NoticeScope = Object.freeze({
  ALL: 'ALL', // 全员
  DEPT: 'DEPT', // 指定部门
  ROLE: 'ROLE', // 指定角色
})

/** com.lemon.erp.modules.system.model.NoticeStatus（字典 sys_notice_status） */
export const NoticeStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  PUBLISHED: 'PUBLISHED', // 已发布
  WITHDRAWN: 'WITHDRAWN', // 已撤回
})

/** com.lemon.erp.modules.fin.model.OffsetStatus（字典 fin_offset_status） */
export const OffsetStatus = Object.freeze({
  DRAFT: 'DRAFT', // 待复核
  REVIEWED: 'REVIEWED', // 已复核
})

/** com.lemon.erp.framework.log.OperType（字典 sys_oper_type） */
export const OperType = Object.freeze({
  OTHER: 'OTHER',
  INSERT: 'INSERT',
  UPDATE: 'UPDATE',
  DELETE: 'DELETE',
  GRANT: 'GRANT',
  EXPORT: 'EXPORT',
  FORCE: 'FORCE',
  CLEAN: 'CLEAN',
  IMPORT: 'IMPORT',
})

/** com.lemon.erp.modules.scm.model.OrderChangeAction */
export const OrderChangeAction = Object.freeze({
  ADD: 'ADD', // 新增明细
  MODIFY: 'MODIFY', // 修改数量 / 单价
  REMOVE: 'REMOVE', // 删除明细
})

/** com.lemon.erp.modules.scm.model.OrderChangeStatus（字典 scm_change_status） */
export const OrderChangeStatus = Object.freeze({
  APPROVING: 'APPROVING', // 审批中
  APPROVED: 'APPROVED', // 已生效
  REJECTED: 'REJECTED', // 已驳回
})

/** com.lemon.erp.modules.scm.model.OrderKind */
export const OrderKind = Object.freeze({
  PURCHASE: 'PURCHASE',
  SALES: 'SALES',
})

/** com.lemon.erp.modules.scm.model.OrderSource（字典 scm_order_source） */
export const OrderSource = Object.freeze({
  MANUAL: 'MANUAL', // 手工
  REORDER: 'REORDER', // 补货建议
  REQUISITION: 'REQUISITION', // 门店要货
  DIRECT: 'DIRECT', // 直接开单
  WX_SHOP: 'WX_SHOP', // 微信小店
})

/** com.lemon.erp.modules.scm.model.OrderStatus（字典 scm_order_status） */
export const OrderStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  APPROVING: 'APPROVING', // 审批中
  APPROVED: 'APPROVED', // 已审核
  PARTIAL: 'PARTIAL', // 部分入库 / 出库
  COMPLETED: 'COMPLETED', // 已完成
  CLOSED: 'CLOSED', // 已关闭
})

/** com.lemon.erp.modules.bi.model.OverstockBase */
export const OverstockBase = Object.freeze({
  MAX_STOCK: 'MAX_STOCK', // 库存上限
  LAST_MONTH: 'LAST_MONTH', // 上个月销量
  RECENT_DAYS: 'RECENT_DAYS', // 最近 N 天销量
})

/** com.lemon.erp.modules.fin.model.PartnerLedgerType */
export const PartnerLedgerType = Object.freeze({
  BILL: 'BILL',
  SETTLEMENT: 'SETTLEMENT',
  ADVANCE: 'ADVANCE',
  NETTING: 'NETTING', // 应收应付对冲
})

/** com.lemon.erp.framework.ref.PartnerType */
export const PartnerType = Object.freeze({
  CUSTOMER: 'CUSTOMER',
  SUPPLIER: 'SUPPLIER',
})

/** com.lemon.erp.framework.setting.PasswordComplexity */
export const PasswordComplexity = Object.freeze({
  LETTER_DIGIT: 'LETTER_DIGIT',
  MIXED_CASE_DIGIT: 'MIXED_CASE_DIGIT',
  LETTER_DIGIT_SYMBOL: 'LETTER_DIGIT_SYMBOL',
})

/** com.lemon.erp.modules.pos.model.PayMethod（字典 pos_pay_method） */
export const PayMethod = Object.freeze({
  CASH: 'CASH', // 现金
  WECHAT: 'WECHAT', // 微信
  ALIPAY: 'ALIPAY', // 支付宝
  BANK_CARD: 'BANK_CARD', // 银行卡
  STORED_VALUE: 'STORED_VALUE', // 储值
})

/** com.lemon.erp.modules.pos.model.PayProviderType（字典 pos_pay_provider） */
export const PayProviderType = Object.freeze({
  OFFLINE: 'OFFLINE', // 线下登记
  MOCK: 'MOCK', // 模拟通道
})

/** com.lemon.erp.modules.pos.model.PaymentStatus（字典 pos_payment_status） */
export const PaymentStatus = Object.freeze({
  PENDING: 'PENDING', // 处理中
  SUCCESS: 'SUCCESS', // 成功
  FAILED: 'FAILED', // 失败
  CLOSED: 'CLOSED', // 已关闭
})

/** com.lemon.erp.modules.fin.model.PeriodStatus */
export const PeriodStatus = Object.freeze({
  OPEN: 'OPEN', // 未结账
  CLOSED: 'CLOSED', // 已结账
})

/** com.lemon.erp.modules.fin.model.PlanUnit */
export const PlanUnit = Object.freeze({
  WEEK: 'WEEK',
  MONTH: 'MONTH',
})

/** com.lemon.erp.modules.pos.model.PosPriceSource（字典 pos_price_source） */
export const PosPriceSource = Object.freeze({
  SPECIAL: 'SPECIAL',
  MEMBER: 'MEMBER',
  SCHEME: 'SCHEME',
  RETAIL: 'RETAIL',
  MANUAL: 'MANUAL',
})

/** com.lemon.erp.modules.pos.model.PosReportDimension（字典 pos_report_dimension） */
export const PosReportDimension = Object.freeze({
  DATE: 'DATE',
  STORE: 'STORE',
  TERMINAL: 'TERMINAL',
  CASHIER: 'CASHIER',
  SHIFT: 'SHIFT',
})

/** com.lemon.erp.modules.pos.model.PosRounding */
export const PosRounding = Object.freeze({
  NONE: 'NONE', // 不抹零
  JIAO: 'JIAO', // 抹分
  YUAN: 'YUAN', // 抹角
  ROUND_JIAO: 'ROUND_JIAO', // 四舍五入到角
})

/** com.lemon.erp.modules.scm.model.PriceAdjustStatus（字典 scm_price_adjust_status） */
export const PriceAdjustStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  APPROVING: 'APPROVING', // 审批中
  APPROVED: 'APPROVED', // 已审核、待生效
  EFFECTIVE: 'EFFECTIVE', // 已生效
  VOIDED: 'VOIDED', // 已作废
})

/** com.lemon.erp.modules.scm.model.PriceAdjustTarget（字典 scm_price_adjust_target） */
export const PriceAdjustTarget = Object.freeze({
  HQ: 'HQ', // 总部价
  SCHEME: 'SCHEME', // 门店价格方案
})

/** com.lemon.erp.modules.scm.model.PriceControlMode（字典 scm_price_control_mode） */
export const PriceControlMode = Object.freeze({
  WARN: 'WARN', // 提示
  BLOCK: 'BLOCK', // 禁止保存
})

/** com.lemon.erp.modules.scm.model.PriceSource */
export const PriceSource = Object.freeze({
  CUSTOMER_PRICE: 'CUSTOMER_PRICE',
  PRICE_LIST: 'PRICE_LIST',
  PRICE_LEVEL: 'PRICE_LEVEL',
  STORE_SCHEME: 'STORE_SCHEME', // 门店价格方案的零售价
  RETAIL: 'RETAIL',
  SUPPLIER_PRICE: 'SUPPLIER_PRICE',
  LAST_PRICE: 'LAST_PRICE',
  REF_COST: 'REF_COST',
})

/** com.lemon.erp.modules.scm.model.PromoType（字典 scm_promo_type） */
export const PromoType = Object.freeze({
  DISCOUNT: 'DISCOUNT',
  REDUCE: 'REDUCE',
  GIFT: 'GIFT',
})

/** com.lemon.erp.modules.scm.model.PurchaseFreightCost */
export const PurchaseFreightCost = Object.freeze({
  INVENTORY: 'INVENTORY', // 计入存货成本
  EXPENSE: 'EXPENSE', // 计入当期费用
})

/** com.lemon.erp.modules.scm.model.QuickPrice */
export const QuickPrice = Object.freeze({
  REF_COST: 'REF_COST', // 参考进货价
  AVG_COST: 'AVG_COST', // 库存均价
  LAST_PURCHASE: 'LAST_PURCHASE', // 最新进价
  LAST_SALES: 'LAST_SALES', // 最新售价
  RETAIL: 'RETAIL',
  WHOLESALE1: 'WHOLESALE1',
  WHOLESALE2: 'WHOLESALE2',
  WHOLESALE3: 'WHOLESALE3',
  WHOLESALE4: 'WHOLESALE4',
  WHOLESALE5: 'WHOLESALE5',
  WHOLESALE6: 'WHOLESALE6',
  WHOLESALE7: 'WHOLESALE7',
  WHOLESALE8: 'WHOLESALE8',
  NONE: 'NONE', // 不显示价格
})

/** com.lemon.erp.modules.pos.model.ReceiptPaper（字典 pos_receipt_paper） */
export const ReceiptPaper = Object.freeze({
  MM80: 'MM80',
  MM58: 'MM58',
})

/** com.lemon.erp.framework.ref.RefType */
export const RefType = Object.freeze({
  MATERIAL: 'MATERIAL',
  WAREHOUSE: 'WAREHOUSE',
  CUSTOMER: 'CUSTOMER',
  SUPPLIER: 'SUPPLIER',
  USER: 'USER',
  DEPT: 'DEPT',
  MATERIAL_CATEGORY: 'MATERIAL_CATEGORY', // 物料分类
  STORE: 'STORE', // 门店
  REGION: 'REGION', // 区域
  PRICE_SCHEME: 'PRICE_SCHEME', // 门店价格方案
})

/** com.lemon.erp.modules.dist.model.RequisitionStatus（字典 dist_requisition_status） */
export const RequisitionStatus = Object.freeze({
  DRAFT: 'DRAFT',
  APPROVING: 'APPROVING', // 审批中
  SUBMITTED: 'SUBMITTED', // 待审核
  APPROVED: 'APPROVED', // 已审核
  PARTIAL: 'PARTIAL', // 部分配送
  DELIVERED: 'DELIVERED', // 已配送
  CLOSED: 'CLOSED', // 已关闭
})

/** com.lemon.erp.modules.wms.model.ReservationSource */
export const ReservationSource = Object.freeze({
  SALES: 'SALES', // 销售订单
})

/** com.lemon.erp.modules.scm.model.RfmMode */
export const RfmMode = Object.freeze({
  THRESHOLD: 'THRESHOLD', // 固定阈值
  AVERAGE: 'AVERAGE', // 平均分
})

/** com.lemon.erp.modules.scm.model.RfmSegment */
export const RfmSegment = Object.freeze({
  IMPORTANT_VALUE: 'IMPORTANT_VALUE', // 重要价值
  IMPORTANT_DEVELOP: 'IMPORTANT_DEVELOP', // 重要发展
  IMPORTANT_KEEP: 'IMPORTANT_KEEP', // 重要保持
  IMPORTANT_RETAIN: 'IMPORTANT_RETAIN', // 重要挽留
  GENERAL_VALUE: 'GENERAL_VALUE', // 一般价值
  GENERAL_DEVELOP: 'GENERAL_DEVELOP', // 一般发展
  GENERAL_KEEP: 'GENERAL_KEEP', // 一般保持
  GENERAL_RETAIN: 'GENERAL_RETAIN', // 一般挽留
})

/** com.lemon.erp.modules.pos.model.SaleChannel（字典 pos_sale_channel） */
export const SaleChannel = Object.freeze({
  POS: 'POS', // 门店收银
  ALL: 'ALL', // 全部渠道
})

/** com.lemon.erp.modules.pos.model.SaleKind（字典 pos_sale_kind） */
export const SaleKind = Object.freeze({
  SALE: 'SALE',
  RETURN: 'RETURN',
})

/** com.lemon.erp.modules.pos.model.SaleStatus（字典 pos_sale_status） */
export const SaleStatus = Object.freeze({
  COMPLETED: 'COMPLETED',
  VOIDED: 'VOIDED',
})

/** com.lemon.erp.modules.scm.model.SalesChannel（字典 scm_channel） */
export const SalesChannel = Object.freeze({
  DIRECT: 'DIRECT',
  DEALER: 'DEALER',
})

/** com.lemon.erp.modules.bi.model.SalesRateBase */
export const SalesRateBase = Object.freeze({
  PURCHASE: 'PURCHASE', // 本期进货量
  OPENING_PURCHASE: 'OPENING_PURCHASE', // 期初库存 + 本期进货量
})

/** com.lemon.erp.modules.bi.model.SalesRateMode */
export const SalesRateMode = Object.freeze({
  SLOW: 'SLOW', // 滞销
  HOT: 'HOT', // 畅销
})

/** com.lemon.erp.modules.bi.model.ScreenKey */
export const ScreenKey = Object.freeze({
  WAREHOUSE: 'WAREHOUSE',
  SALES: 'SALES',
})

/** com.lemon.erp.modules.wms.model.SerialStatus（字典 wms_serial_status） */
export const SerialStatus = Object.freeze({
  IN_STOCK: 'IN_STOCK',
  OUT: 'OUT',
})

/** com.lemon.erp.framework.setting.Setting */
export const Setting = Object.freeze({
  SYSTEM_NAME: 'SYSTEM_NAME',
  SHOW_DEMO_ACCOUNT: 'SHOW_DEMO_ACCOUNT',
  WATERMARK: 'WATERMARK',
  CAPTCHA_MODE: 'CAPTCHA_MODE',
  CAPTCHA_TYPE: 'CAPTCHA_TYPE',
  LOGIN_MAX_RETRY: 'LOGIN_MAX_RETRY',
  LOGIN_LOCK_MINUTES: 'LOGIN_LOCK_MINUTES',
  SESSION_EXPIRE_HOURS: 'SESSION_EXPIRE_HOURS',
  SESSION_SINGLE: 'SESSION_SINGLE',
  PASSWORD_MIN_LENGTH: 'PASSWORD_MIN_LENGTH',
  PASSWORD_COMPLEXITY: 'PASSWORD_COMPLEXITY',
  PASSWORD_EXPIRE_DAYS: 'PASSWORD_EXPIRE_DAYS',
  USER_INITIAL_PASSWORD: 'USER_INITIAL_PASSWORD',
  ATTACHMENT_MAX_MB: 'ATTACHMENT_MAX_MB',
})

/** com.lemon.erp.framework.setting.SettingType */
export const SettingType = Object.freeze({
  TEXT: 'TEXT',
  NUMBER: 'NUMBER',
  BOOL: 'BOOL',
  SELECT: 'SELECT',
  DECIMAL: 'DECIMAL', // 小数
  TABLE: 'TABLE', // 表格
})

/** com.lemon.erp.modules.fin.model.SettleKind */
export const SettleKind = Object.freeze({
  RECEIPT: 'RECEIPT', // 收款
  PAYMENT: 'PAYMENT', // 付款
})

/** com.lemon.erp.modules.pos.model.ShiftStatus（字典 pos_shift_status） */
export const ShiftStatus = Object.freeze({
  OPEN: 'OPEN',
  CLOSED: 'CLOSED',
})

/** com.lemon.erp.modules.pos.model.SpecialScope（字典 pos_special_scope） */
export const SpecialScope = Object.freeze({
  ALL: 'ALL', // 全部门店
  STORES: 'STORES', // 指定门店
  REGIONS: 'REGIONS', // 指定区域
})

/** com.lemon.erp.modules.pos.model.SpecialStatus（字典 pos_special_status） */
export const SpecialStatus = Object.freeze({
  DRAFT: 'DRAFT',
  ACTIVE: 'ACTIVE',
  ENDED: 'ENDED',
})

/** com.lemon.erp.modules.wms.model.StockAlert */
export const StockAlert = Object.freeze({
  LOW: 'LOW', // 低于安全库存
  HIGH: 'HIGH', // 高于库存上限
})

/** com.lemon.erp.modules.bi.model.StockAlertFilter */
export const StockAlertFilter = Object.freeze({
  LOW: 'LOW', // 低于库存下限
  HIGH: 'HIGH', // 高于库存上限
  ANY: 'ANY', // 低于下限或高于上限
})

/** com.lemon.erp.modules.wms.model.StockAlertMode（字典 wms_stock_alert_mode） */
export const StockAlertMode = Object.freeze({
  TOTAL: 'TOTAL', // 按总库存
  WAREHOUSE: 'WAREHOUSE', // 分仓库
})

/** com.lemon.erp.modules.bi.model.StockAlertTab */
export const StockAlertTab = Object.freeze({
  WAREHOUSE: 'WAREHOUSE', // 仓库
  HIGH: 'HIGH', // 高于上限
  LOW: 'LOW', // 低于下限
})

/** com.lemon.erp.modules.wms.model.StockBizType（字典 wms_biz_type） */
export const StockBizType = Object.freeze({
  IN: 'IN',
  OUT: 'OUT',
  TRANSFER: 'TRANSFER',
  CHECK: 'CHECK',
})

/** com.lemon.erp.modules.wms.model.StockInType（字典 wms_in_type） */
export const StockInType = Object.freeze({
  PURCHASE: 'PURCHASE', // 采购入库
  SALES_RETURN: 'SALES_RETURN', // 销售退货
  OTHER: 'OTHER', // 其他入库
  CONSIGN_IN: 'CONSIGN_IN', // 寄售入库
  OPENING: 'OPENING', // 期初入库
  ASSEMBLY: 'ASSEMBLY', // 组装拆卸
  RETAIL_RETURN: 'RETAIL_RETURN', // 零售退货
})

/** com.lemon.erp.modules.wms.model.StockOutType（字典 wms_out_type） */
export const StockOutType = Object.freeze({
  SALES: 'SALES', // 销售出库
  PURCHASE_RETURN: 'PURCHASE_RETURN', // 采购退货
  OTHER: 'OTHER', // 其他出库
  SCRAP: 'SCRAP', // 报废出库
  ASSEMBLY: 'ASSEMBLY', // 组装拆卸
  RETAIL: 'RETAIL', // 零售出库
})

/** com.lemon.erp.modules.bi.model.StockQtyFilter */
export const StockQtyFilter = Object.freeze({
  POSITIVE: 'POSITIVE', // 库存 &gt
  ZERO: 'ZERO', // 库存 = 0
  NEGATIVE: 'NEGATIVE', // 库存 &lt
  NONZERO: 'NONZERO', // 库存 ≠ 0
})

/** com.lemon.erp.framework.store.StoreScope（字典 sys_store_scope） */
export const StoreScope = Object.freeze({
  ALL: 'ALL', // 全部门店
  ASSIGNED: 'ASSIGNED', // 所属门店 + 指定的管辖门店
})

/** com.lemon.erp.framework.store.StoreStatus（字典 store_status） */
export const StoreStatus = Object.freeze({
  PREPARING: 'PREPARING', // 筹备
  OPEN: 'OPEN', // 营业
  SUSPENDED: 'SUSPENDED', // 停业
  CLOSED: 'CLOSED', // 闭店
})

/** com.lemon.erp.framework.store.StoreType（字典 store_type） */
export const StoreType = Object.freeze({
  HQ: 'HQ', // 总部
  DIRECT: 'DIRECT', // 直营店
  FRANCHISE: 'FRANCHISE', // 加盟店
  DC: 'DC', // 配送中心
})

/** com.lemon.erp.modules.scm.model.SupplierLevel（字典 scm_supplier_level） */
export const SupplierLevel = Object.freeze({
  A: 'A', // A 优秀
  B: 'B', // B 良好
  C: 'C', // C 合格
  D: 'D', // D 待改进
})

/** com.lemon.erp.modules.scm.model.TradeKind（字典 scm_trade_kind） */
export const TradeKind = Object.freeze({
  PURCHASE_IN: 'PURCHASE_IN',
  PURCHASE_RETURN: 'PURCHASE_RETURN',
  SALES_OUT: 'SALES_OUT',
  SALES_RETURN: 'SALES_RETURN',
})

/** com.lemon.erp.modules.wms.model.TransferFeeSettle（字典 scm_freight_settle） */
export const TransferFeeSettle = Object.freeze({
  CASH: 'CASH', // 现付
  MONTHLY: 'MONTHLY', // 月结
})

/** com.lemon.erp.modules.wms.model.TransferPricing */
export const TransferPricing = Object.freeze({
  SAME: 'SAME', // 同价调拨
  REF_COST: 'REF_COST', // 参考成本价
  AVG_COST: 'AVG_COST', // 库存均价
  MIN_SALE: 'MIN_SALE', // 最低售价
  PRICE_LEVEL: 'PRICE_LEVEL', // 价格列
})

/** com.lemon.erp.modules.bi.model.TurnoverBasis */
export const TurnoverBasis = Object.freeze({
  QTY: 'QTY', // 销售数量
  COST: 'COST', // 成本价
  RETAIL: 'RETAIL', // 零售价
  WHOLESALE: 'WHOLESALE', // 批发价
})

/** com.lemon.erp.modules.bi.model.TurnoverOutFilter */
export const TurnoverOutFilter = Object.freeze({
  HAS_OUT: 'HAS_OUT', // 不显示本期出库为 0 的商品
  NO_OUT: 'NO_OUT', // 只显示本期出库为 0 的商品
})

/** com.lemon.erp.modules.fin.model.VoucherSource（字典 fin_voucher_source） */
export const VoucherSource = Object.freeze({
  MANUAL: 'MANUAL',
  AR: 'AR', // 应收单
  AP: 'AP', // 应付单
  RECEIPT: 'RECEIPT', // 收款单
  PAYMENT: 'PAYMENT', // 付款单
  CLOSE: 'CLOSE', // 期末结转损益
  ADVANCE: 'ADVANCE', // 预收预付及冲抵
  BAD_DEBT: 'BAD_DEBT', // 坏账核销
  REVERSAL: 'REVERSAL', // 红字冲销
  TAX: 'TAX', // 增值税结转、附加税计提
  FX: 'FX', // 期末调汇
  TRANSFER: 'TRANSFER', // 账户调拨
  INCOME_EXPENSE: 'INCOME_EXPENSE', // 其他收支单
  NETTING: 'NETTING', // 应收应付对冲
})

/** com.lemon.erp.modules.fin.model.VoucherStatus（字典 fin_voucher_status） */
export const VoucherStatus = Object.freeze({
  DRAFT: 'DRAFT',
  APPROVING: 'APPROVING',
  AUDITED: 'AUDITED',
  POSTED: 'POSTED',
})

/** com.lemon.erp.modules.wms.model.WarehouseType（字典 wms_warehouse_type） */
export const WarehouseType = Object.freeze({
  NORMAL: 'NORMAL', // 普通仓
  RAW: 'RAW', // 原料仓
  FINISHED: 'FINISHED', // 成品仓
  DEFECT: 'DEFECT', // 不良品仓
  CONSIGN_OUT: 'CONSIGN_OUT', // 客户寄售仓
  CONSIGN_IN: 'CONSIGN_IN', // 供应商寄售仓
})

/** com.lemon.erp.modules.channel.model.WxAftersaleErpStatus（字典 ch_wx_aftersale_erp_status） */
export const WxAftersaleErpStatus = Object.freeze({
  NONE: 'NONE', // 未处理
  DONE: 'DONE', // 已处理
  SKIPPED: 'SKIPPED', // 无需处理
  FAILED: 'FAILED', // 处理失败
})

/** com.lemon.erp.modules.channel.model.WxAftersaleStatus（字典 ch_wx_aftersale_status） */
export const WxAftersaleStatus = Object.freeze({
  PENDING: 'PENDING', // 待商家处理
  REJECTED: 'REJECTED', // 商家已拒绝
  WAIT_RETURN: 'WAIT_RETURN', // 待买家退货
  WAIT_RECEIPT: 'WAIT_RECEIPT', // 待商家收货
  REFUNDING: 'REFUNDING', // 退款中
  REFUNDED: 'REFUNDED', // 已退款
  REFUND_FAILED: 'REFUND_FAILED', // 退款失败
  CANCELLED: 'CANCELLED', // 已取消 / 关闭
})

/** com.lemon.erp.modules.channel.model.WxAftersaleType（字典 ch_wx_aftersale_type） */
export const WxAftersaleType = Object.freeze({
  REFUND: 'REFUND',
  RETURN: 'RETURN',
})

/** com.lemon.erp.modules.channel.model.WxDeliveryStatus（字典 ch_wx_delivery_status） */
export const WxDeliveryStatus = Object.freeze({
  NONE: 'NONE', // 未发货
  PUSHING: 'PUSHING', // 回传中
  PUSHED: 'PUSHED', // 已回传
  FAILED: 'FAILED', // 回传失败
  PLATFORM: 'PLATFORM', // 在小店后台发货
})

/** com.lemon.erp.modules.channel.model.WxErpStatus（字典 ch_wx_erp_status） */
export const WxErpStatus = Object.freeze({
  NONE: 'NONE', // 未处理
  PENDING: 'PENDING', // 待生成
  UNMAPPED: 'UNMAPPED', // 待映射
  ORDERED: 'ORDERED', // 已生成销售订单
  OUTBOUND: 'OUTBOUND', // 已出库
  CLOSED: 'CLOSED', // 已关闭
  FAILED: 'FAILED', // 处理失败
})

/** com.lemon.erp.modules.channel.model.WxOrderStatus（字典 ch_wx_order_status） */
export const WxOrderStatus = Object.freeze({
  UNPAID: 'UNPAID', // 待付款
  TO_SHIP: 'TO_SHIP', // 待发货
  SHIPPED: 'SHIPPED', // 已发货 / 待收货
  COMPLETED: 'COMPLETED', // 已完成
  CANCELLED: 'CANCELLED', // 已取消
})

/** com.lemon.erp.modules.channel.model.WxShopMode（字典 ch_wx_shop_mode） */
export const WxShopMode = Object.freeze({
  MOCK: 'MOCK',
  LIVE: 'LIVE',
})

/** com.lemon.erp.modules.channel.model.WxSyncTrigger（字典 ch_wx_sync_trigger） */
export const WxSyncTrigger = Object.freeze({
  MANUAL: 'MANUAL', // 手工「立即同步」
  SCHEDULE: 'SCHEDULE', // 定时任务
  CALLBACK: 'CALLBACK', // 小店消息推送
})

/** com.lemon.erp.modules.channel.model.WxSyncType（字典 ch_wx_sync_type） */
export const WxSyncType = Object.freeze({
  PRODUCT: 'PRODUCT', // 商品
  STOCK: 'STOCK', // 库存
  ORDER: 'ORDER', // 订单
  AFTERSALE: 'AFTERSALE', // 售后
})
