import { approveReturn, deleteDelivery, rejectReturn, shipDelivery, submitReturn } from '@/api/dist'
import { useAction } from '@/composables/useAction'
import { DistDeliveryStatus } from '@/constants/enums'
import { t } from '@/locales'

/**
 * 配送单 / 退仓单的单条操作（列表、详情共用）：发货、提交、审核、删除；驳回需要原因，由页面的 ReasonModal 处理。
 * kind：delivery | return；reload 在成功后执行
 */
export function useDeliveryActions(kind, reload) {
  const isReturn = kind === 'return'
  const ship = useAction((d) => shipDelivery(kind, d.id), {
    confirm: (d) => ({ title: t('dist.delivery.shipTitle', { no: d.deliveryNo }), content: t('dist.delivery.shipContent') }),
    successKey: 'dist.delivery.shipped',
    reload: () => reload()
  })
  const submit = useAction((d) => submitReturn(d.id), {
    confirm: (d) => t('dist.delivery.submitTitle', { no: d.deliveryNo }),
    reload: () => reload()
  })
  const approve = useAction((d) => approveReturn(d.id), {
    confirm: (d) => t('dist.delivery.approveTitle', { no: d.deliveryNo }),
    reload: () => reload()
  })
  const remove = useAction((d) => deleteDelivery(kind, d.id), { successKey: 'common.msg.deleteSuccess', reload: () => reload() })
  const reject = (d, reason) => rejectReturn(d.id, reason)
  const isDraft = (d) => d.status === DistDeliveryStatus.DRAFT
  /** 可以发货：配送单为草稿，退仓单为已审核 */
  const canShip = (d) => (isReturn ? d.status === DistDeliveryStatus.APPROVED : d.status === DistDeliveryStatus.DRAFT)
  const canReceive = (d) => d.status === DistDeliveryStatus.SHIPPED
  const perm = {
    edit: `dist:${kind}:edit`,
    remove: `dist:${kind}:remove`,
    ship: `dist:${kind}:ship`,
    receive: isReturn ? 'dist:return:receive' : 'dist:receive:confirm',
    audit: 'dist:return:audit'
  }
  return { ship, submit, approve, reject, remove, isDraft, canShip, canReceive, perm, isReturn }
}
