<template>
  <view v-if="doc">
    <view class="m-card">
      <view class="m-row">
        <text class="m-title">{{ doc.deliveryNo }}</text>
        <l-tag :text="dictText(dict.dist_delivery_status, doc.status)" :color="dictColor(dict.dist_delivery_status, doc.status)" />
      </view>
      <view class="m-sub">{{ t('dist.receive.from', { name: doc.fromStoreName || doc.fromWarehouseName }) }} → {{ doc.toWarehouseName }}</view>
      <picker mode="date" :value="receiveDate" @change="(e) => (receiveDate = e.detail.value)">
        <view class="m-input">
          <text class="m-input__label">{{ t('dist.receive.receiveDate') }}</text>
          <text class="m-input__value">{{ receiveDate }}</text>
        </view>
      </picker>
    </view>
    <view v-for="row in rows" :key="row.id" class="m-card">
      <view class="m-title">{{ row.materialCode }} {{ row.materialName }}</view>
      <view class="m-sub">{{ t('dist.receive.shipped', { n: qty(row.qty) }) }} {{ row.unit }}{{ row.batchNo ? ` · ${row.batchNo}` : '' }}</view>
      <view class="m-input">
        <text class="m-input__label">{{ t('dist.receive.received') }}</text>
        <input
          :value="row.received"
          type="digit"
          :disabled="row.serialEnabled === 1"
          :class="diffClass(row)"
          @blur="(e) => (row.received = toQty(e.detail.value))"
        />
      </view>
      <view v-if="row.serialEnabled === 1" class="m-sub">{{ t('dist.receive.serialTip') }}</view>
      <block v-if="Number(row.received) < Number(row.qty)">
        <view class="m-input">
          <text class="m-input__label">{{ t('dist.receive.damaged') }}</text>
          <switch :checked="row.damaged" style="transform: scale(0.7)" @change="(e) => (row.damaged = e.detail.value)" />
        </view>
      </block>
      <view v-if="Number(row.received) !== Number(row.qty)" class="m-input">
        <text class="m-input__label">{{ t('dist.receive.reason') }}</text>
        <input v-model="row.reason" maxlength="255" :placeholder="t('common.pleaseInput')" placeholder-class="l-placeholder" />
      </view>
    </view>
    <view class="l-footer-space" />
    <view v-if="canReceive" class="l-footer">
      <button class="l-btn" :loading="saving" :disabled="saving" @click="confirm">{{ t('dist.receive.confirm') }}</button>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { t, useI18n } from '@/locales'
import { DiscrepancyKind, DistDeliveryStatus } from '@web/constants/enums'
import { getDelivery, receiveDelivery } from '@/api/dist'
import { useTitle } from '@/composables/useTitle'
import { runAction } from '@/composables/useAction'
import { dictColor, dictText, useDict } from '@/store/dict'
import { hasPerm } from '@/store/user'
import { qty, today } from '@/utils/format'

/** 收货：逐行填写实收数量（默认等于发货数量），少于发货时可标记破损并填写说明；有差异的生成收货差异，由总部处理 */
useI18n()
useTitle('dist.entry.receive')
const dict = useDict('dist_delivery_status')
const doc = ref(null)
const rows = ref([])
const receiveDate = ref(today())
const saving = ref(false)
const canReceive = computed(() => hasPerm('dist:receive:confirm') && doc.value?.status === DistDeliveryStatus.SHIPPED)

const toQty = (v) => {
  const n = parseFloat(v)
  return Number.isFinite(n) && n >= 0 ? n : 0
}
const diffClass = (row) => (Number(row.received) < Number(row.qty) ? 'qty-short' : Number(row.received) > Number(row.qty) ? 'qty-done' : '')

onLoad(async (q) => {
  const d = await getDelivery(q.id)
  doc.value = d
  rows.value = (d.items || []).map((i) => ({ ...i, received: Number(i.qty), damaged: false, reason: '' }))
})

async function confirm() {
  const diffs = rows.value.filter((r) => Number(r.received) !== Number(r.qty)).length
  saving.value = true
  try {
    await runAction(
      () =>
        receiveDelivery(doc.value.id, {
          receiveDate: receiveDate.value,
          items: rows.value.map((r) => ({
            itemId: r.id,
            receivedQty: r.received,
            kind: r.damaged ? DiscrepancyKind.DAMAGED : DiscrepancyKind.SHORT,
            reason: r.reason || null
          }))
        }),
      { confirm: diffs ? t('dist.receive.confirmDiff', { n: diffs }) : t('dist.receive.confirmAll'), success: t('dist.receive.done'), then: () => uni.navigateBack() }
    )
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss">
@import '../mobile.scss';
</style>
