<template>
  <view>
    <!-- 只读：已提交的要货单 -->
    <block v-if="readonly && doc">
      <view class="m-card">
        <view class="m-row">
          <text class="m-title">{{ doc.reqNo }}</text>
          <l-tag :text="dictText(dict.dist_requisition_status, doc.status)" :color="dictColor(dict.dist_requisition_status, doc.status)" />
        </view>
        <view class="m-sub">{{ doc.storeName }} → {{ doc.supplyWarehouseName }}</view>
        <view class="m-sub">{{ t('dist.requisition.bizDate') }} {{ doc.bizDate }}{{ doc.expectedDate ? ` · ${t('dist.requisition.expectedDate')} ${doc.expectedDate}` : '' }}</view>
      </view>
      <view v-for="i in doc.items" :key="i.id" class="m-card">
        <view class="m-title">{{ i.materialCode }} {{ i.materialName }}</view>
        <view class="m-sub">{{ t('dist.requisition.qty') }} {{ qty(i.qty) }} {{ i.unit }}</view>
        <view class="m-sub">
          {{ t('dist.requisition.approved', { n: i.approvedQty == null ? '-' : qty(i.approvedQty) }) }} · {{ t('dist.requisition.delivered', { n: qty(i.deliveredQty) }) }}
        </view>
      </view>
    </block>

    <!-- 新增 / 编辑草稿 -->
    <block v-else>
      <view v-if="doc && doc.rejectReason" class="m-tip m-tip--fail">{{ t('dist.requisition.rejectReason', { reason: doc.rejectReason }) }}</view>
      <view class="m-card">
        <picker :range="warehouses" range-key="name" :value="warehouseIndex" @change="changeWarehouse">
          <view class="m-input">
            <text class="m-input__label">{{ t('dist.requisition.warehouse') }}</text>
            <text class="m-input__value" :class="{ 'l-placeholder': !warehouseId }">{{ warehouseName || t('common.pleaseSelect') }}</text>
          </view>
        </picker>
        <picker mode="date" :value="bizDate" @change="(e) => (bizDate = e.detail.value)">
          <view class="m-input">
            <text class="m-input__label">{{ t('dist.requisition.bizDate') }}</text>
            <text class="m-input__value">{{ bizDate }}</text>
          </view>
        </picker>
        <picker mode="date" :value="expectedDate || bizDate" @change="(e) => (expectedDate = e.detail.value)">
          <view class="m-input">
            <text class="m-input__label">{{ t('dist.requisition.expectedDate') }}</text>
            <text class="m-input__value" :class="{ 'l-placeholder': !expectedDate }">{{ expectedDate || t('common.pleaseSelect') }}</text>
          </view>
        </picker>
        <view class="m-input">
          <text class="m-input__label">{{ t('common.remark') }}</text>
          <input v-model="remark" maxlength="255" :placeholder="t('common.pleaseInput')" placeholder-class="l-placeholder" />
        </view>
      </view>

      <view class="rq__scan">
        <view class="rq__scan-input"><l-scan-input :placeholder="t('dist.requisition.scanTip')" @scan="onScan" /></view>
        <text class="m-link rq__link" @click="openSheet">{{ t('dist.requisition.addMaterial') }}</text>
        <text class="m-link rq__link" @click="useSuggest">{{ t('dist.requisition.useSuggest') }}</text>
      </view>
      <view v-if="tip.text" class="m-tip" :class="{ 'm-tip--fail': !tip.ok }">{{ tip.text }}</view>

      <view v-for="row in rows" :key="row.key" class="m-card">
        <view class="m-row">
          <text class="m-title m-grow">{{ row.materialCode }} {{ row.materialName }}</text>
          <text class="m-link" @click="removeRow(row)">{{ t('common.delete') }}</text>
        </view>
        <view class="m-sub">
          {{ [t('dist.requisition.stock', { n: num(row.stockQty) }), t('dist.requisition.transit', { n: num(row.transitQty) }), t('dist.requisition.suggest', { n: num(row.suggestQty) })].join(' · ') }}
        </view>
        <view v-if="row.supplyAvailable != null" class="m-sub">{{ t('dist.requisition.supply', { n: num(row.supplyAvailable) }) }}</view>
        <view class="m-input">
          <text class="m-input__label">{{ t('dist.requisition.qty') }}</text>
          <input :value="row.qty" type="digit" :placeholder="t('common.pleaseInput')" placeholder-class="l-placeholder" @blur="(e) => (row.qty = toNumber(e.detail.value, null))" />
          <text class="m-sub">{{ row.unit }}</text>
        </view>
      </view>
      <l-empty v-if="!rows.length" :text="t('dist.requisition.noItems')" />

      <view class="l-footer-space" />
      <view class="l-footer">
        <button class="l-btn l-btn--default" :disabled="saving" @click="save(false)">{{ t('dist.requisition.saveDraft') }}</button>
        <button v-if="canSubmit" class="l-btn" :loading="saving" :disabled="saving" @click="save(true)">{{ t('dist.requisition.saveSubmit') }}</button>
      </view>

      <SearchSheet
        :open="sheetOpen"
        :title="t('dist.requisition.addMaterial')"
        :items="sheetItems"
        :loading="sheetLoading"
        :has-more="sheetHasMore"
        @search="searchSheet"
        @more="searchSheet(sheetKeyword, true)"
        @select="selectSheet"
        @close="sheetOpen = false"
      />
    </block>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { t, useI18n } from '@/locales'
import { RequisitionStatus } from '@web/constants/enums'
import { addRequisition, getRequisition, requisitionSuggest, submitRequisition, updateRequisition } from '@/api/dist'
import { OPTION_PAGE_SIZE, materialOptions, warehouseOptions } from '@/api/wms'
import { useTitle } from '@/composables/useTitle'
import { runAction } from '@/composables/useAction'
import { dictColor, dictText, useDict } from '@/store/dict'
import { hasPerm } from '@/store/user'
import { qty, today } from '@/utils/format'
import { resolveScan, rowKey, useScanTip } from '../mobile'
import SearchSheet from '../components/SearchSheet.vue'

/**
 * 要货单录入（店长）：收货仓库（本店的仓库，门店由仓库决定，供货仓库按门店的上级配送门店）、日期、商品（扫码或搜索）与要货数量；
 * 每行显示现存、在途、建议量（安全库存 − 现存 − 在途）和供货仓库可用。可保存草稿或保存并提交；已提交的只读查看
 */
useI18n()
useTitle('dist.entry.requisition')
const dict = useDict('dist_requisition_status')
const canSubmit = hasPerm('dist:requisition:submit')
const tip = useScanTip()
const doc = ref(null)
const readonly = computed(() => !!doc.value && doc.value.status !== RequisitionStatus.DRAFT)
const warehouses = ref([])
const warehouseId = ref(null)
const bizDate = ref(today())
const expectedDate = ref('')
const remark = ref('')
const rows = ref([])
const saving = ref(false)
const warehouseIndex = computed(() => Math.max(0, warehouses.value.findIndex((w) => w.id === warehouseId.value)))
const warehouseName = computed(() => warehouses.value.find((w) => w.id === warehouseId.value)?.name || '')
const num = (v) => (v == null ? '-' : qty(v))

const toNumber = (v, empty) => {
  const n = parseFloat(v)
  return Number.isFinite(n) && n > 0 ? n : empty
}

onLoad(async (q) => {
  warehouses.value = (await warehouseOptions().catch(() => [])) || []
  if (q.id) {
    const d = await getRequisition(q.id)
    doc.value = d
    warehouseId.value = d.warehouseId
    bizDate.value = d.bizDate
    expectedDate.value = d.expectedDate || ''
    remark.value = d.remark || ''
    rows.value = (d.items || []).map((i) => ({ ...i, key: `item-${i.id}`, qty: Number(i.qty) }))
  } else if (warehouses.value.length === 1) {
    warehouseId.value = warehouses.value[0].id
  }
})

function changeWarehouse(e) {
  warehouseId.value = warehouses.value[e.detail.value]?.id ?? null
  fillSuggest(rows.value)
}

async function fillSuggest(targets) {
  if (!warehouseId.value || !targets.length) return
  const list = (await requisitionSuggest(warehouseId.value, targets.map((r) => r.materialId)).catch(() => [])) || []
  for (const r of targets) {
    const s = list.find((x) => x.materialId === r.materialId)
    Object.assign(r, { stockQty: s?.stockQty, transitQty: s?.transitQty, suggestQty: s?.suggestQty, supplyAvailable: s?.supplyAvailable })
  }
}

function addMaterial(m) {
  const exists = rows.value.find((r) => r.materialId === m.id)
  if (exists) {
    exists.qty = (Number(exists.qty) || 0) + 1
    return tip.show(`${m.name} × ${exists.qty}`)
  }
  const row = { key: rowKey(), materialId: m.id, materialCode: m.code, materialName: m.name, unit: m.unit, qty: null }
  rows.value.unshift(row)
  fillSuggest([row])
  tip.show(m.name)
}

async function onScan(code) {
  const hit = await resolveScan(code)
  if (hit.type === 'material') return addMaterial(hit.material)
  tip.show(t('dist.requisition.notFound', { code }), false)
}

function removeRow(row) {
  rows.value = rows.value.filter((r) => r !== row)
}

function useSuggest() {
  rows.value.forEach((r) => {
    if (Number(r.suggestQty) > 0) r.qty = Number(r.suggestQty)
  })
}

// ---------------------------------------------------------------- 搜索商品
const sheetOpen = ref(false)
const sheetItems = ref([])
const sheetLoading = ref(false)
const sheetKeyword = ref('')
const sheetHasMore = ref(false)
let sheetPage = 1
let sheetSeq = 0
function openSheet() {
  sheetItems.value = []
  sheetHasMore.value = false
  sheetOpen.value = true
}
async function searchSheet(keyword, more = false) {
  const seq = ++sheetSeq
  const page = more ? sheetPage + 1 : 1
  if (!more) sheetKeyword.value = keyword
  sheetLoading.value = true
  try {
    const list = (await materialOptions(keyword, page).catch(() => [])) || []
    if (seq !== sheetSeq) return
    const items = list.map((m) => ({ id: m.id, title: m.name, sub: [m.code, m.spec, m.unit].filter(Boolean).join(' · '), raw: m }))
    sheetPage = page
    sheetItems.value = more ? [...sheetItems.value, ...items] : items
    sheetHasMore.value = list.length >= OPTION_PAGE_SIZE
  } finally {
    if (seq === sheetSeq) sheetLoading.value = false
  }
}
function selectSheet(item) {
  sheetOpen.value = false
  if (item) addMaterial(item.raw)
}

// ---------------------------------------------------------------- 保存
async function save(submit) {
  if (!warehouseId.value) return uni.showToast({ title: t('common.selectRequired', { name: t('dist.requisition.warehouse') }), icon: 'none' })
  if (!rows.value.length) return uni.showToast({ title: t('dist.requisition.noItems'), icon: 'none' })
  if (rows.value.some((r) => !(Number(r.qty) > 0))) return uni.showToast({ title: t('dist.requisition.noQty'), icon: 'none' })
  const data = {
    warehouseId: warehouseId.value,
    bizDate: bizDate.value,
    expectedDate: expectedDate.value || null,
    remark: remark.value || null,
    items: rows.value.map((r) => ({ materialId: r.materialId, qty: r.qty, suggestQty: r.suggestQty ?? null }))
  }
  saving.value = true
  try {
    await runAction(
      async () => {
        const id = doc.value ? doc.value.id : await addRequisition(data)
        if (doc.value) await updateRequisition(id, data)
        if (submit) await submitRequisition(id)
      },
      { confirm: submit ? t('dist.requisition.submitConfirm') : undefined, success: submit ? t('common.submitted') : t('common.saved'), then: () => uni.navigateBack() }
    )
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss">
@import '../mobile.scss';

.rq {
  &__scan {
    display: flex;
    align-items: center;
    gap: 16rpx;
    margin: 0 24rpx;
  }
  &__scan-input {
    flex: 1;
  }
  &__link {
    white-space: nowrap;
  }
}
</style>
