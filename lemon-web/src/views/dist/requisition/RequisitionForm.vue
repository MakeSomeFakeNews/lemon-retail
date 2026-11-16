<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('dist.requisition.add')" :edit-title="$t('dist.requisition.editTitle')" width="min(1200px, 100vw)">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('dist.common.receiveWarehouse')" name="warehouseId">
            <WarehouseSelect v-model:value="form.warehouseId" :allow-clear="false" @update:value="refreshSuggest" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('dist.common.supplyWarehouse')" name="supplyWarehouseId" :extra="$t('dist.requisition.supplyTip')">
            <WarehouseSelect v-model:value="form.supplyWarehouseId" all :placeholder="$t('dist.requisition.supplyTip')" @update:value="refreshSuggest" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="4">
          <a-form-item :label="$t('dist.common.bizDate')" name="bizDate">
            <a-date-picker v-model:value="form.bizDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="4">
          <a-form-item :label="$t('dist.common.expectedDate')" name="expectedDate">
            <a-date-picker v-model:value="form.expectedDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>
      <div class="items-head">
        <span class="section-title">{{ $t('dist.common.items') }}</span>
        <a-space>
          <a-tooltip :title="$t('dist.requisition.suggestTip')">
            <a-button size="small" :disabled="!form.warehouseId" @click="refreshSuggest">{{ $t('dist.requisition.calcSuggest') }}</a-button>
          </a-tooltip>
          <a-button size="small" @click="useSuggest">{{ $t('dist.requisition.useSuggest') }}</a-button>
          <a-button size="small" @click="lines.addRow()"><template #icon><PlusOutlined /></template>{{ $t('common.action.addRow') }}</a-button>
        </a-space>
      </div>
      <LineItemsTable :items="form.items" :columns="itemColumns" :min-rows="1" :pagination="form.items.length > 50 ? { pageSize: 50 } : false">
        <template #cell="{ column, record }">
          <template v-if="column.key === 'material'">
            <a-form-item :name="lines.itemName(record, 'materialId')" :rules="[{ required: true, message: $t('dist.common.selectMaterial') }]" class="cell-item">
              <MaterialSelect v-model:value="record.materialId" :label="record.materialLabel" @select="(m) => applyMaterial(record, m)" />
            </a-form-item>
          </template>
          <template v-else-if="column.key === 'unit'">{{ record.unit }}</template>
          <template v-else-if="column.key === 'stockQty'">{{ qtyText(record.stockQty) }}</template>
          <template v-else-if="column.key === 'transitQty'">{{ qtyText(record.transitQty) }}</template>
          <template v-else-if="column.key === 'supplyAvailable'">{{ qtyText(record.supplyAvailable) }}</template>
          <template v-else-if="column.key === 'suggestQty'">{{ qtyText(record.suggestQty) }}</template>
          <template v-else-if="column.key === 'qty'">
            <a-form-item :name="lines.itemName(record, 'qty')" :rules="[{ required: true, message: $t('dist.common.inputQty') }]" class="cell-item">
              <a-input-number v-model:value="record.qty" :min="0.0001" :precision="4" class="full" />
            </a-form-item>
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
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import { PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import LineItemsTable from '@/components/common/LineItemsTable.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { addRequisition, getRequisition, requisitionSuggest, updateRequisition } from '@/api/dist'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { lineKey, rowKey, useLineItems } from '@/composables/useLineItems'
import { qty } from '@/utils/number'

/**
 * 要货单的新增、修改（草稿）：收货仓库（门店由仓库决定）、供货仓库（为空取上级配送门店的默认仓库）、要货 / 期望到货日期、明细。
 * 明细显示本店现存、在途、供货仓库可用和建议量（安全库存 − 现存 − 在途），可一键按建议量填写
 */
const emit = defineEmits(['saved'])
const { t } = useI18n()
const qtyText = (v) => (v == null ? '-' : qty(v))

const emptyItem = () => ({ key: rowKey(), materialId: undefined, materialLabel: undefined, unit: '', qty: undefined, suggestQty: undefined, stockQty: undefined, transitQty: undefined, supplyAvailable: undefined, remark: '' })
const itemColumns = computed(() => [
  { title: t('dist.common.material'), key: 'material', width: 300 },
  { title: t('common.field.unit'), key: 'unit', width: 70 },
  { title: t('dist.requisition.stockQty'), key: 'stockQty', width: 100, align: 'right' },
  { title: t('dist.requisition.transitQty'), key: 'transitQty', width: 90, align: 'right' },
  { title: t('dist.requisition.supplyAvailable'), key: 'supplyAvailable', width: 100, align: 'right' },
  { title: t('dist.requisition.suggestQty'), key: 'suggestQty', width: 90, align: 'right' },
  { title: t('dist.requisition.qty'), key: 'qty', width: 140 },
  { title: t('common.field.remark'), key: 'remark', width: 180 },
  { title: '', key: 'action', width: 50 }
])
const rules = computed(() => ({
  warehouseId: [{ required: true, message: t('dist.common.selectWarehouse') }],
  bizDate: [{ required: true, message: t('dist.common.selectDate') }]
}))

const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, warehouseId: undefined, supplyWarehouseId: undefined, bizDate: dayjs().format('YYYY-MM-DD'), expectedDate: undefined, remark: '', items: [emptyItem()] }),
  fetch: async (record) => {
    const d = await getRequisition(record.id)
    return {
      id: d.id,
      warehouseId: d.warehouseId,
      supplyWarehouseId: d.supplyWarehouseId,
      bizDate: d.bizDate,
      expectedDate: d.expectedDate || undefined,
      remark: d.remark || '',
      items: d.items.map((i) => ({
        key: lineKey(i),
        materialId: i.materialId,
        materialLabel: `${i.materialCode} ${i.materialName}`,
        unit: i.unit,
        qty: Number(i.qty),
        suggestQty: i.suggestQty,
        stockQty: i.stockQty,
        transitQty: i.transitQty,
        supplyAvailable: i.supplyAvailable,
        remark: i.remark || ''
      }))
    }
  },
  create: async (data) => emit('saved', await addRequisition(data)),
  update: async (id, data) => {
    await updateRequisition(id, data)
    emit('saved', id)
  },
  toPayload: (f) => ({
    warehouseId: f.warehouseId,
    supplyWarehouseId: f.supplyWarehouseId || null,
    bizDate: f.bizDate,
    expectedDate: f.expectedDate || null,
    remark: f.remark || null,
    items: f.items.filter((i) => i.materialId).map((i) => ({ materialId: i.materialId, qty: i.qty, suggestQty: i.suggestQty ?? null, remark: i.remark || null }))
  })
})
const { formRef, form } = drawer
const lines = useLineItems(() => form.value.items, { newRow: emptyItem })

/** 刷新各行的现存、在途、供货可用和建议量 */
async function fillSuggest(rows) {
  const targets = rows.filter((r) => r.materialId)
  if (!form.value.warehouseId || !targets.length) return
  const list = await requisitionSuggest(form.value.warehouseId, form.value.supplyWarehouseId, targets.map((r) => r.materialId)).catch(() => [])
  const map = Object.fromEntries((list || []).map((s) => [s.materialId, s]))
  targets.forEach((r) => {
    const s = map[r.materialId]
    Object.assign(r, { stockQty: s?.stockQty, transitQty: s?.transitQty, supplyAvailable: s?.supplyAvailable, suggestQty: s?.suggestQty })
  })
}
const refreshSuggest = () => fillSuggest(form.value.items)

function useSuggest() {
  form.value.items.forEach((r) => {
    if (r.materialId && Number(r.suggestQty) > 0) r.qty = Number(r.suggestQty)
  })
}

function applyMaterial(row, m) {
  Object.assign(row, { materialLabel: m.label, unit: m.unit })
  fillSuggest([row])
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
</style>
