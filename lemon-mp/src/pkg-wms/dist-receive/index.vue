<template>
  <view>
    <view class="m-tip dr__hint">{{ t('dist.receive.tip') }}</view>
    <view v-for="d in paged.list" :key="d.id" class="l-item" @click="open(d)">
      <view class="l-item__head">
        <text class="l-item__title">{{ d.deliveryNo }}</text>
        <l-tag :text="dictText(dict.dist_delivery_status, d.status)" :color="dictColor(dict.dist_delivery_status, d.status)" />
      </view>
      <view class="l-item__desc">{{ t('dist.receive.from', { name: d.fromStoreName || d.fromWarehouseName }) }} → {{ d.toWarehouseName }}</view>
      <view class="dr__row">
        <text class="l-muted dr__meta">{{ d.bizDate }}</text>
        <text class="l-muted dr__meta">{{ t('dist.receive.shipped', { n: qty(d.totalQty) }) }}</text>
      </view>
    </view>
    <l-empty v-if="paged.loaded && !paged.list.length" />
    <l-load-more v-else-if="paged.list.length" :status="paged.status" />
  </view>
</template>

<script setup>
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { DistDeliveryStatus } from '@web/constants/enums'
import { pageDelivery } from '@/api/dist'
import { usePaged } from '@/composables/usePaged'
import { useTitle } from '@/composables/useTitle'
import { dictColor, dictText, useDict } from '@/store/dict'
import { qty } from '@/utils/format'

/** 门店收货：可管辖门店待收货（已发货）的配送单，点击逐行确认实收 */
const { t } = useI18n()
useTitle('dist.entry.receive')
const dict = useDict('dist_delivery_status')
const paged = usePaged((p) => pageDelivery({ ...p, statuses: DistDeliveryStatus.SHIPPED }))

onShow(() => paged.refresh())
onPullDownRefresh(() => paged.refresh())
onReachBottom(() => paged.more())

function open(d) {
  uni.navigateTo({ url: `/pkg-wms/dist-receive/detail?id=${d.id}` })
}
</script>

<style lang="scss">
@import '../mobile.scss';

.dr {
  &__hint {
    margin: 24rpx;
  }
  &__row {
    display: flex;
    justify-content: space-between;
    margin-top: 12rpx;
  }
  &__meta {
    font-size: 24rpx;
  }
}
</style>
