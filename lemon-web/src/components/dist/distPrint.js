import { t } from '@/locales'
import { materialColumns, printDocs } from '@/utils/print'
import { printName } from '@/utils/printTemplates'
import { money, price, qty } from '@/utils/number'

/** 配送模块的打印：要货单、配送单 / 退仓单（模板标识见 printTemplates.js 的 dist.*，匹配键为打印名称的中文） */
const col = (id, title, extra) => ({ id: printName(id), title, ...extra })
const num = (v) => (v == null ? '' : qty(v))

export function printRequisition(d) {
  printDocs(
    {
      title: t('dist.requisition.printTitle'),
      no: d.reqNo,
      fields: [
        [t('dist.common.requestStore'), d.storeName, false, printName('requestStore')],
        [t('dist.common.receiveWarehouse'), d.warehouseName, false, printName('receiveWarehouse')],
        [t('dist.common.supplyWarehouse'), d.supplyWarehouseName, false, printName('supplyWarehouse')],
        [t('dist.common.bizDate'), d.bizDate, false, printName('bizDate')],
        [t('dist.common.expectedDate'), d.expectedDate, false, printName('expectedDate')],
        [t('common.field.status'), d.statusLabel, false, printName('status')]
      ],
      columns: [
        ...materialColumns(),
        col('requestQty', t('dist.requisition.qty'), { key: 'qty', value: (r) => num(r.qty), align: 'right' }),
        col('approvedQty', t('dist.requisition.approvedQty'), { value: (r) => num(r.approvedQty), align: 'right' }),
        col('remark', t('common.field.remark'), { key: 'remark' })
      ],
      rows: d.items,
      totals: { qty: num(d.totalQty) },
      note: d.remark,
      signs: [[t('dist.common.createBy'), d.createByName, printName('prepared')], [t('dist.requisition.auditBy'), d.auditByName, printName('approved')]]
    },
    `${t('dist.requisition.printTitle')} ${d.reqNo}`,
    'dist.requisition'
  )
}

/** 配送单 / 退仓单：配送价、金额有值时打印（没有查看权限时后端不返回） */
export function printDelivery(d, isReturn) {
  const title = isReturn ? t('dist.delivery.returnTitle') : t('dist.delivery.deliveryTitle')
  printDocs(
    {
      title,
      no: d.deliveryNo,
      fields: [
        [t('dist.common.fromStore'), d.fromStoreName, false, printName('shipStore')],
        [t('dist.common.fromWarehouse'), d.fromWarehouseName, false, printName('shipWarehouse')],
        [t('dist.common.toStore'), d.toStoreName, false, printName('receiveStore')],
        [t('dist.common.toWarehouse'), d.toWarehouseName, false, printName('receiveWarehouse')],
        [t('dist.common.bizDate'), d.bizDate, false, printName('shipDate')],
        [t('dist.common.transferNo'), d.transferNo, false, printName('sourceNo')],
        [t('dist.delivery.totalAmount'), d.totalAmount == null ? '' : money(d.totalAmount), false, printName('deliveryAmount')]
      ],
      columns: [
        ...materialColumns(),
        col('batch', t('dist.delivery.batch'), { key: 'batchNo' }),
        col('quantity', t('common.field.quantity'), { key: 'qty', value: (r) => num(r.qty), align: 'right' }),
        col('deliveryPrice', t('dist.delivery.price'), { value: (r) => (r.price == null ? '' : price(r.price)), align: 'right' }),
        col('deliveryAmount', t('dist.delivery.amount'), { value: (r) => (r.amount == null ? '' : money(r.amount)), align: 'right' }),
        col('receivedQty', t('dist.delivery.receivedQty'), { value: (r) => num(r.receivedQty), align: 'right' }),
        col('remark', t('common.field.remark'), { key: 'remark' })
      ],
      rows: d.items,
      totals: { qty: num(d.totalQty) },
      note: d.remark,
      signs: [[t('dist.common.createBy'), d.createByName, printName('prepared')], [t('dist.delivery.shipBy'), d.shipByName, printName('storekeeper')], [t('dist.delivery.receiveBy'), '', printName('receiver')]]
    },
    `${title} ${d.deliveryNo}`,
    isReturn ? 'dist.return' : 'dist.delivery'
  )
}
