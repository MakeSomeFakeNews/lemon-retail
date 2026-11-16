<template>
  <view>
    <l-segmented v-model="status" :options="statuses" @change="paged.refresh()" />
    <view v-for="r in paged.list" :key="r.id" class="l-item" @click="open(r)">
      <view class="l-item__head">
        <text class="l-item__title">{{ r.reqNo }}</text>
        <l-tag :text="dictText(dict.dist_requisition_status, r.status)" :color="dictColor(dict.dist_requisition_status, r.status)" />
      </view>
      <view class="l-item__desc">{{ [r.storeName, r.supplyWarehouseName].filter(Boolean).join(' → ') }}</view>
      <view class="req__row">
        <text class="l-muted req__meta">{{ r.bizDate }} · {{ t('dist.requisition.items', { n: r.itemCount }) }}</text>
        <text class="l-muted req__meta">{{ t('dist.requisition.approved', { n: qty(r.approvedQty) }) }} · {{ t('dist.requisition.delivered', { n: qty(r.deliveredQty) }) }}</text>
      </view>
      <view v-if="r.rejectReason && r.status === RequisitionStatus.DRAFT" class="req__reject">{{ t('dist.requisition.rejectReason', { reason: r.rejectReason }) }}</view>
    </view>
    <l-empty v-if="paged.loaded && !paged.list.length" />
    <l-load-more v-else-if="paged.list.length" :status="paged.status" />
    <view class="l-footer-space" />
    <view v-if="canAdd" class="l-footer">
      <button class="l-btn" @click="create">{{ t('dist.requisition.add') }}</button>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { RequisitionStatus } from '@web/constants/enums'
import { pageRequisition } from '@/api/dist'
import { usePaged } from '@/composables/usePaged'
import { useTitle } from '@/composables/useTitle'
import { dictColor, dictText, useDict } from '@/store/dict'
import { hasPerm } from '@/store/user'
import { qty } from '@/utils/format'

/** 门店要货：本店（可管辖门店）的要货单，草稿可继续编辑提交；按状态筛选 */
const { t } = useI18n()
useTitle('dist.entry.requisition')
const dict = useDict('dist_requisition_status')
const canAdd = hasPerm('dist:requisition:add')
const status = ref('')
const statuses = computed(() => [
  { label: t('common.all'), value: '' },
  ...[RequisitionStatus.DRAFT, RequisitionStatus.SUBMITTED, RequisitionStatus.APPROVED, RequisitionStatus.DELIVERED].map((s) => ({
    label: dictText(dict.dist_requisition_status, s),
    value: s
  }))
])
const paged = usePaged((p) => pageRequisition({ ...p, statuses: status.value || undefined }))

// 从编辑页返回时刷新
onShow(() => paged.refresh())
onPullDownRefresh(() => paged.refresh())
onReachBottom(() => paged.more())

function open(r) {
  uni.navigateTo({ url: `/pkg-wms/requisition/edit?id=${r.id}` })
}
function create() {
  uni.navigateTo({ url: '/pkg-wms/requisition/edit' })
}
</script>

<style lang="scss">
.req {
  &__row {
    display: flex;
    justify-content: space-between;
    margin-top: 12rpx;
  }
  &__meta {
    font-size: 24rpx;
  }
  &__reject {
    margin-top: 8rpx;
    font-size: 24rpx;
    color: $l-danger;
  }
}
</style>
