import { t } from '@/locales'
import { pushWxDelivery, refreshWxOrder, retryWxOrder } from '@/api/channel/wxShop'
import { useAction } from '@/composables/useAction'
import { WxDeliveryStatus, WxErpStatus, WxOrderStatus } from '@/constants/enums'

/** 可以生成销售订单 / 重新处理：已付款且待生成、待映射、处理失败，或已在线上发货 / 取消但还没有出库 / 关闭 */
export const canRetry = (o) =>
  ([WxOrderStatus.TO_SHIP, WxOrderStatus.SHIPPED, WxOrderStatus.COMPLETED].includes(o.status) &&
    [WxErpStatus.PENDING, WxErpStatus.UNMAPPED, WxErpStatus.FAILED].includes(o.erpStatus)) ||
  (o.erpStatus === WxErpStatus.ORDERED && o.status !== WxOrderStatus.TO_SHIP && o.status !== WxOrderStatus.UNPAID)

/** 可以在 ERP 中发货：待发货且已生成销售订单 */
export const canShip = (o) => o.status === WxOrderStatus.TO_SHIP && o.erpStatus === WxErpStatus.ORDERED

/** 发货信息回传失败（或回传中断）可重试 */
export const canPush = (o) => o.deliveryStatus === WxDeliveryStatus.FAILED || o.deliveryStatus === WxDeliveryStatus.PUSHING

/** 渠道订单的单条操作（列表与详情共用），reload 为成功后的刷新 */
export function useOrderActions(reload) {
  const retry = useAction((o) => retryWxOrder(o.id), { successKey: 'channel.order.retried', reload })
  const refresh = useAction((o) => refreshWxOrder(o.id), { successKey: 'channel.order.refreshed', reload })
  const push = useAction((o) => pushWxDelivery(o.id), {
    confirm: () => t('channel.order.confirmPush'),
    successKey: 'channel.order.pushed',
    reload
  })
  return { retry, refresh, push }
}
