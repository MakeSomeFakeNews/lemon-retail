import { deletePriceAdjust, submitPriceAdjust, voidPriceAdjust, withdrawPriceAdjust } from '@/api/scm/priceAdjust'
import { useAction } from '@/composables/useAction'
import { PriceAdjustStatus } from '@/constants/enums'
import { t } from '@/locales'

/** 调价单的单条操作（列表、详情共用）：提交、撤回、作废、删除；reload 在成功后执行 */
export function useAdjustActions(reload) {
  const submit = useAction((d) => submitPriceAdjust(d.id), {
    confirm: (d) => ({
      title: t('scm.priceAdjust.submitTitle', { no: d.adjustNo }),
      content: d.effectiveTime ? t('scm.priceAdjust.submitScheduled', { time: d.effectiveTime }) : t('scm.priceAdjust.submitImmediate')
    }),
    successKey: (status) => t(`scm.priceAdjust.submitted.${status}`),
    reload: () => reload()
  })
  const withdraw = useAction((d) => withdrawPriceAdjust(d.id), {
    confirm: (d) => t('scm.priceAdjust.withdrawTitle', { no: d.adjustNo }),
    reload: () => reload()
  })
  const voidAdjust = useAction((d) => voidPriceAdjust(d.id), {
    confirm: (d) => ({ title: t('scm.priceAdjust.voidTitle', { no: d.adjustNo }), content: t('scm.priceAdjust.voidContent'), okType: 'danger' }),
    reload: () => reload()
  })
  const remove = useAction((d) => deletePriceAdjust(d.id), { successKey: 'common.msg.deleteSuccess', reload: () => reload() })
  const isDraft = (d) => d.status === PriceAdjustStatus.DRAFT
  return { submit, withdraw, voidAdjust, remove, isDraft }
}
