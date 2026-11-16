<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('scm.priceAdjust.add')" :edit-title="$t('scm.priceAdjust.editTitle')" width="min(1100px, 100vw)">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('scm.priceAdjust.target')" name="target">
            <a-radio-group v-model:value="form.target" :options="toOptions(dict.scm_price_adjust_target)" @change="refreshPrices" />
          </a-form-item>
        </a-col>
        <a-col v-if="isScheme" :xs="24" :sm="16">
          <a-form-item :label="$t('scm.priceAdjust.scheme')" name="schemeIds">
            <a-select
              v-model:value="form.schemeIds"
              mode="multiple"
              show-search
              option-filter-prop="label"
              :options="schemeOptions"
              :placeholder="$t('scm.priceAdjust.schemePh')"
              @change="refreshPrices"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('scm.priceAdjust.effectiveMode')" name="scheduled">
            <a-radio-group v-model:value="form.scheduled">
              <a-radio :value="false">{{ $t('scm.priceAdjust.immediate') }}</a-radio>
              <a-radio :value="true">{{ $t('scm.priceAdjust.scheduled') }}</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col v-if="form.scheduled" :xs="24" :sm="8">
          <a-form-item :label="$t('scm.priceAdjust.effectiveTime')" name="effectiveTime">
            <a-date-picker v-model:value="form.effectiveTime" show-time value-format="YYYY-MM-DD HH:mm:ss" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('scm.priceAdjust.reason')" name="reason">
            <a-input v-model:value="form.reason" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>
      <div class="items-head">
        <span class="section-title">{{ $t('scm.priceAdjust.items') }}</span>
        <a-space>
          <span class="sub-text">{{ isScheme ? $t('scm.priceAdjust.itemsTipScheme') : $t('scm.priceAdjust.itemsTipHq') }}</span>
          <a-button size="small" @click="lines.addRow()"><template #icon><PlusOutlined /></template>{{ $t('common.action.addRow') }}</a-button>
        </a-space>
      </div>
      <LineItemsTable :items="form.items" :columns="itemColumns" :min-rows="1" :pagination="form.items.length > 50 ? { pageSize: 50 } : false">
        <template #cell="{ column, record }">
          <template v-if="column.key === 'material'">
            <a-form-item :name="lines.itemName(record, 'materialId')" :rules="[{ required: true, message: $t('scm.priceAdjust.materialRequired') }]" class="cell-item">
              <MaterialSelect v-model:value="record.materialId" :label="record.materialLabel" @select="(m) => applyMaterial(record, m)" />
            </a-form-item>
          </template>
          <template v-else-if="column.key === 'unit'">{{ record.unit }}</template>
          <template v-else-if="column.key === 'current'">
            <span>{{ record.currentRetail == null ? '-' : price(record.currentRetail) }}</span>
            <div v-if="isScheme && record.currentMember != null" class="sub-text">{{ $t('scm.priceAdjust.memberShort', { price: price(record.currentMember) }) }}</div>
          </template>
          <template v-else-if="column.key === 'retailPrice'">
            <a-input-number v-model:value="record.retailPrice" :min="0" :precision="4" :placeholder="isScheme ? $t('scm.priceAdjust.unchanged') : undefined" class="full" />
          </template>
          <template v-else-if="column.key === 'memberPrice'">
            <a-input-number v-model:value="record.memberPrice" :min="0" :precision="4" :placeholder="$t('scm.priceAdjust.unchanged')" class="full" />
          </template>
          <template v-else-if="column.key === 'remark'">
            <a-input v-model:value="record.remark" :maxlength="255" />
          </template>
        </template>
      </LineItemsTable>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import LineItemsTable from '@/components/common/LineItemsTable.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { addPriceAdjust, currentPrices, getPriceAdjust, updatePriceAdjust } from '@/api/scm/priceAdjust'
import { priceSchemeOptions } from '@/api/scm/priceScheme'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { lineKey, rowKey, useLineItems } from '@/composables/useLineItems'
import { toOptions } from '@/utils/dict'
import { price } from '@/utils/number'
import { PriceAdjustTarget } from '@/constants/enums'

/**
 * 调价单的新增、修改（草稿）：调价对象（总部零售价 / 一个或多个价格方案）、生效时间（立即 / 定时）、商品新价格。
 * 「当前价」：总部价时为总部零售价；只选一个方案时为该方案的方案价，选了多个方案时为总部零售价（供参考）
 */
const emit = defineEmits(['saved'])
const { t } = useI18n()
const dict = useDict('scm_price_adjust_target')

const schemes = ref([])
const schemeOptions = computed(() => schemes.value.map((s) => ({ value: s.id, label: `${s.code} ${s.name}` })))
const isScheme = computed(() => form.value.target === PriceAdjustTarget.SCHEME)

const emptyItem = () => ({ key: rowKey(), materialId: undefined, materialLabel: undefined, unit: '', currentRetail: undefined, currentMember: undefined, retailPrice: undefined, memberPrice: undefined, remark: '' })
const itemColumns = computed(() => [
  { title: t('scm.priceAdjust.material'), key: 'material', width: 300 },
  { title: t('common.field.unit'), key: 'unit', width: 70 },
  { title: t('scm.priceAdjust.currentPrice'), key: 'current', width: 120, align: 'right' },
  { title: t('scm.priceAdjust.newRetailPrice'), key: 'retailPrice', width: 140 },
  ...(isScheme.value ? [{ title: t('scm.priceAdjust.newMemberPrice'), key: 'memberPrice', width: 140 }] : []),
  { title: t('common.field.remark'), key: 'remark', width: 180 },
  { title: '', key: 'action', width: 50 }
])
const rules = computed(() => ({
  target: [{ required: true, message: t('scm.priceAdjust.targetRequired') }],
  schemeIds: [{ required: isScheme.value, type: 'array', min: 1, message: t('scm.priceAdjust.schemeRequired') }],
  effectiveTime: [{ required: form.value.scheduled, message: t('scm.priceAdjust.effectiveTimeRequired') }]
}))

const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, target: PriceAdjustTarget.HQ, schemeIds: [], scheduled: false, effectiveTime: undefined, reason: '', items: [emptyItem()] }),
  beforeOpen: async () => {
    schemes.value = await priceSchemeOptions().catch(() => [])
  },
  fetch: async (record) => {
    const d = await getPriceAdjust(record.id)
    // 编辑时补上已停用的方案，便于回显
    const missing = (d.schemeIds || []).filter((id) => !schemes.value.some((s) => s.id === id))
    if (missing.length) schemes.value = await priceSchemeOptions(d.schemeIds).catch(() => schemes.value)
    const f = {
      id: d.id,
      target: d.target,
      schemeIds: d.schemeIds || [],
      scheduled: !!d.effectiveTime,
      effectiveTime: d.effectiveTime || undefined,
      reason: d.reason || '',
      items: d.items.map((i) => ({
        key: lineKey(i),
        materialId: i.materialId,
        materialLabel: `${i.materialCode} ${i.materialName}`,
        unit: i.unit,
        currentRetail: undefined,
        currentMember: undefined,
        retailPrice: i.retailPrice == null ? undefined : Number(i.retailPrice),
        memberPrice: i.memberPrice == null ? undefined : Number(i.memberPrice),
        remark: i.remark || ''
      }))
    }
    await fillPrices(f, f.items)
    return f
  },
  create: async (data) => emit('saved', await addPriceAdjust(data)),
  update: async (id, data) => {
    await updatePriceAdjust(id, data)
    emit('saved', id)
  },
  toPayload: (f) => {
    const scheme = f.target === PriceAdjustTarget.SCHEME
    return {
      target: f.target,
      schemeIds: scheme ? f.schemeIds : [],
      effectiveTime: f.scheduled ? f.effectiveTime : null,
      reason: f.reason || null,
      items: f.items.filter((i) => i.materialId).map((i) => ({
        materialId: i.materialId,
        retailPrice: i.retailPrice ?? null,
        memberPrice: scheme ? i.memberPrice ?? null : null,
        remark: i.remark || null
      }))
    }
  }
})
const { formRef, form } = drawer
const lines = useLineItems(() => form.value.items, { newRow: emptyItem })

/** 当前价对照：总部价或只选一个方案时取该方案价，多个方案时取总部零售价 */
async function fillPrices(f, rows) {
  const targets = rows.filter((r) => r.materialId)
  if (!targets.length) return
  const schemeId = f.target === PriceAdjustTarget.SCHEME && f.schemeIds.length === 1 ? f.schemeIds[0] : undefined
  const map = await currentPrices(schemeId, targets.map((r) => r.materialId)).catch(() => ({}))
  targets.forEach((r) => {
    const p = map[r.materialId]
    r.currentRetail = p?.retailPrice ?? undefined
    r.currentMember = p?.memberPrice ?? undefined
  })
}
const refreshPrices = () => fillPrices(form.value, form.value.items)

function applyMaterial(row, m) {
  Object.assign(row, { materialLabel: m.label, unit: m.unit })
  fillPrices(form.value, [row])
}

defineExpose({ openCreate: () => drawer.openCreate(), openEdit: (record) => drawer.openEdit(record) })
</script>

<style scoped>
.full {
  width: 100%;
}
.cell-item {
  margin-bottom: 0;
}
.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0 12px;
}
.section-title {
  font-weight: 500;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
