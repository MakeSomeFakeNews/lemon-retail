import { closeRequisition, deleteRequisition, submitRequisition } from '@/api/dist'
import { useAction } from '@/composables/useAction'
import { RequisitionStatus } from '@/constants/enums'
import { t } from '@/locales'

/** 要货单的单条操作（列表、详情共用）：提交、关闭、删除；reload 在成功后执行 */
export function useRequisitionActions(reload) {
  const submit = useAction((d) => submitRequisition(d.id), {
    confirm: (d) => ({ title: t('dist.requisition.submitTitle', { no: d.reqNo }), content: t('dist.requisition.submitContent') }),
    successKey: (status) => t(`dist.requisition.submitted.${status}`),
    reload: () => reload()
  })
  const close = useAction((d) => closeRequisition(d.id, null), {
    confirm: (d) => ({ title: t('dist.audit.closeTitle', { no: d.reqNo }), content: t('dist.audit.closeTip'), okType: 'danger' }),
    reload: () => reload()
  })
  const remove = useAction((d) => deleteRequisition(d.id), { successKey: 'common.msg.deleteSuccess', reload: () => reload() })
  const isDraft = (d) => d.status === RequisitionStatus.DRAFT
  const deliverable = (d) => d.status === RequisitionStatus.APPROVED || d.status === RequisitionStatus.PARTIAL
  return { submit, close, remove, isDraft, deliverable }
}
