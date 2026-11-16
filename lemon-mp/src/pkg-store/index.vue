<template>
  <view class="sw">
    <picker v-if="stores.length > 1" :range="stores" range-key="name" :value="storeIndex" @change="changeStore">
      <view class="sw__store">
        <text class="sw__store-name">{{ storeName || t('common.pleaseSelect') }}</text>
        <text class="sw__store-switch">{{ t('store.switchStore') }} ›</text>
      </view>
    </picker>
    <view v-else class="sw__store">
      <text class="sw__store-name">{{ storeName }}</text>
    </view>
    <l-empty v-if="loaded && !stores.length" :text="t('store.noStore')" />

    <block v-if="data">
      <view class="sw__date l-muted">{{ t('store.today', { date: data.date }) }}</view>
      <view class="sw__cards">
        <view v-for="c in cards" :key="c.key" class="sw__card">
          <view class="sw__card-label">{{ c.label }}</view>
          <view class="sw__card-value">{{ c.text }}</view>
          <view v-if="c.rate !== null" class="sw__card-rate" :class="c.rate >= 0 ? 'up' : 'down'">
            {{ t('store.vsYesterday', { rate: (c.rate >= 0 ? '+' : '') + c.rate + '%' }) }}
          </view>
          <view v-else class="sw__card-rate l-muted">{{ t('store.yesterday', { value: c.prevText }) }}</view>
        </view>
      </view>

      <l-card :title="t('store.shortcuts')">
        <view class="sw__grid">
          <view v-for="s in shortcuts" :key="s.key" class="sw__entry" @click="go(s)">
            <view class="sw__icon" :style="{ background: s.color }">
              {{ s.title.slice(0, 1) }}
              <text v-if="s.badge" class="sw__badge">{{ s.badge > 99 ? '99+' : s.badge }}</text>
            </view>
            <text class="sw__entry-title">{{ s.title }}</text>
          </view>
        </view>
      </l-card>

      <l-card :title="t('store.topItems')">
        <view v-if="!data.topItems.length" class="l-muted">{{ t('store.noSales') }}</view>
        <view v-for="(i, idx) in data.topItems" :key="i.materialId" class="sw__top">
          <text class="sw__top-rank">{{ idx + 1 }}</text>
          <view class="sw__top-name">
            <view>{{ i.materialName }}</view>
            <view class="l-muted sw__top-code">{{ i.materialCode }}</view>
          </view>
          <view class="sw__top-num">
            <view>{{ money(i.amount) }}</view>
            <view class="l-muted sw__top-code">{{ qty(i.qty) }}{{ i.unit || '' }}</view>
          </view>
        </view>
      </l-card>
    </block>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { storeDashboard, storeOptions } from '@/api/chain'
import { useTitle } from '@/composables/useTitle'
import { hasPerm } from '@/store/user'
import { money, qty } from '@/utils/format'

/**
 * 店长工作台（MP-01）：门店看板（今天的 POS 销售额、单数、客单价、毛利与昨天对比，畅销商品）和本店的快捷入口：
 * 库存查询（只看本店仓库）、盘点、门店要货（草稿数）、门店收货（待收货数）、审批。多个门店时可切换，选择记在本机
 */
const { t } = useI18n()
useTitle('store.entry.workbench')
const STORE_KEY = 'lemon_mp_store'
const stores = ref([])
const storeId = ref(null)
const data = ref(null)
const loaded = ref(false)

const store = computed(() => stores.value.find((s) => s.id === storeId.value))
const storeName = computed(() => store.value?.name || '')
const storeIndex = computed(() => Math.max(0, stores.value.findIndex((s) => s.id === storeId.value)))

/** 较昨天的变化（%），昨天为 0 时为空 */
const rate = (now, prev) => (Number(prev) ? Math.round(((Number(now) - Number(prev)) / Math.abs(Number(prev))) * 1000) / 10 : null)
const cards = computed(() => {
  const d = data.value
  if (!d) return []
  const list = [
    { key: 'sales', label: t('store.sales'), now: d.today.sales, prev: d.yesterday.sales, fmt: money },
    { key: 'orders', label: t('store.orders'), now: d.today.orders, prev: d.yesterday.orders, fmt: (v) => String(v ?? 0) },
    { key: 'avgTicket', label: t('store.avgTicket'), now: d.today.avgTicket, prev: d.yesterday.avgTicket, fmt: (v) => (v == null ? '-' : money(v)) }
  ]
  if (d.today.grossProfit != null) {
    list.push({ key: 'grossProfit', label: t('store.grossProfit'), now: d.today.grossProfit, prev: d.yesterday.grossProfit, fmt: money })
  }
  return list.map((c) => ({ ...c, text: c.fmt(c.now), prevText: c.fmt(c.prev), rate: rate(c.now, c.prev) }))
})

const shortcuts = computed(() => {
  const d = data.value || {}
  const wh = store.value?.defaultWarehouseId
  return [
    {
      key: 'stock',
      title: t('store.stock'),
      color: '#1677ff',
      perm: 'wms:stock:list',
      url: wh ? `/pkg-wms/stock/index?warehouseId=${wh}&warehouseName=${encodeURIComponent(storeName.value)}` : '/pkg-wms/stock/index'
    },
    { key: 'check', title: t('store.check'), color: '#52c41a', perm: 'wms:check:edit', url: '/pkg-wms/check/index' },
    { key: 'requisition', title: t('store.requisition'), color: '#722ed1', perm: 'dist:requisition:list', url: '/pkg-wms/requisition/index', badge: d.draftRequisitions },
    { key: 'receive', title: t('store.receive'), color: '#08979c', perm: 'dist:receive:confirm', url: '/pkg-wms/dist-receive/index', badge: d.pendingReceive },
    { key: 'approval', title: t('store.approval'), color: '#fa8c16', perm: 'wf:task:todo', url: '/pages/approval/index', tab: true }
  ].filter((s) => hasPerm(s.perm))
})

async function load() {
  if (!storeId.value) return
  data.value = await storeDashboard(storeId.value)
}

async function init() {
  stores.value = (await storeOptions().catch(() => [])) || []
  loaded.value = true
  const saved = Number(uni.getStorageSync(STORE_KEY)) || null
  storeId.value = stores.value.some((s) => s.id === saved) ? saved : stores.value[0]?.id || null
  await load()
}

function changeStore(e) {
  storeId.value = stores.value[e.detail.value]?.id || null
  uni.setStorageSync(STORE_KEY, storeId.value)
  load()
}

function go(s) {
  if (s.tab) uni.switchTab({ url: s.url })
  else uni.navigateTo({ url: s.url })
}

onLoad(init)
onShow(() => {
  if (loaded.value) load()
})
onPullDownRefresh(async () => {
  await load().catch(() => {})
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss">
.sw {
  padding-bottom: 24rpx;
  &__store {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin: 24rpx 24rpx 0;
    padding: 24rpx 28rpx;
    background: #fff;
    border-radius: $l-radius;
  }
  &__store-name {
    font-size: 32rpx;
    font-weight: 600;
  }
  &__store-switch {
    font-size: 24rpx;
    color: $l-text-3;
  }
  &__date {
    margin: 20rpx 32rpx 0;
    font-size: 24rpx;
  }
  &__cards {
    display: flex;
    flex-wrap: wrap;
    gap: 20rpx;
    padding: 16rpx 24rpx 0;
  }
  &__card {
    width: calc(50% - 10rpx);
    box-sizing: border-box;
    padding: 24rpx;
    background: #fff;
    border-radius: $l-radius;
  }
  &__card-label {
    font-size: 24rpx;
    color: $l-text-3;
  }
  &__card-value {
    margin-top: 8rpx;
    font-size: 40rpx;
    font-weight: 600;
  }
  &__card-rate {
    margin-top: 4rpx;
    font-size: 22rpx;
    &.up {
      color: #cf1322;
    }
    &.down {
      color: #389e0d;
    }
  }
  &__grid {
    display: flex;
    flex-wrap: wrap;
  }
  &__entry {
    width: 25%;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 12rpx 0;
  }
  &__icon {
    position: relative;
    width: 84rpx;
    height: 84rpx;
    border-radius: 20rpx;
    color: #fff;
    font-size: 34rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  &__badge {
    position: absolute;
    top: -12rpx;
    right: -16rpx;
    min-width: 32rpx;
    padding: 0 8rpx;
    line-height: 32rpx;
    border-radius: 16rpx;
    background: #ff4d4f;
    font-size: 20rpx;
    text-align: center;
  }
  &__entry-title {
    margin-top: 10rpx;
    font-size: 24rpx;
  }
  &__top {
    display: flex;
    align-items: center;
    padding: 14rpx 0;
  }
  &__top-rank {
    width: 48rpx;
    color: $l-text-3;
  }
  &__top-name {
    flex: 1;
  }
  &__top-num {
    text-align: right;
  }
  &__top-code {
    font-size: 22rpx;
  }
}
</style>
