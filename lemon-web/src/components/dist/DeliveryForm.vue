<template>
  <FormDrawer page :drawer="drawer" :create-title="isReturn ? $t('dist.delivery.addReturn') : $t('dist.delivery.addDelivery')" :edit-title="isReturn ? $t('dist.delivery.editReturn') : $t('dist.delivery.editDelivery')" width="min(1200px, 100vw)">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('dist.common.fromWarehouse')" name="fromWarehouseId">
            <WarehouseSelect v-model:value="form.fromWarehouseId" :allow-clear="false" @update:value="onFromChange" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('dist.common.toWarehouse')" name="toWarehouseId" :extra="isReturn ? $t('dist.delivery.toReturnTip') : undefined">
            <WarehouseSelect v-model:value="form.toWarehouseId" all :allow-clear="isReturn" @update:value="refreshPrices" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('dist.common.bizDate')" name="bizDate">
            <a-date-picker v-model:value="form.bizDate" value-format="YYYY-MM-DD" class="full" />
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
          <span v-if="!isReturn" class="sub-text">{{ $t('dist.delivery.priceTip') }}</span>
          <a-button size="small" @click="lines.addRow()"><template #icon><PlusOutlined /></template>{{ $t('common.action.addRow') }}</a-button>
        </a-space>
      </div>
      <LineItemsTable :items="form.items" :columns="itemColumns" :min-rows="1" :pagination="form.items.length > 50 ? { pageSize: 50 } : false">
        <template #cell="{ column, record }">
          <template v-if="column.key === 'material'">
            <a-form-item :name="lines.itemName(record, 'materialId')" :rules="[{ required: true, message: $t('dist.common.selectMaterial') }]" class="cell-item">
              <MaterialSelect v-model:value="record.materialId" :label="record.materialLabel" :disabled="!!record.requisitionItemId" @select="(m) => applyMaterial(record, m)" />
            </a-form-item>
            <div v-if="record.requisitionNo" class="sub-text">{{ $t('dist.delivery.requisitionNo') }}：{{ record.requisitionNo }}</div>
          </template>
          <template v-else-if="column.key === 'unit'">{{ record.unit }}</template>
          <template v-else-if="column.key === 'location'">
            <LocationSelect v-model:value="record.locationId" :warehouse-id="form.fromWarehouseId" />
          </template>
          <template v-else-if="column.key === 'batch'">
            <a-input v-model:value="record.batchNo" :maxlength="64" />
          </template>
          <template v-else-if="column.key === 'qty'">
            <a-form-item :name="lines.itemName(record, 'qty')" :rules="[{ required: true, message: $t('dist.common.inputQty') }]" class="cell-item">
              <a-input-number v-model:value="record.qty" :min="0.0001" :precision="4" class="full" />
            </a-form-item>
          </template>
          <template v-else-if="column.key === 'price'">
            <a-input-number v-model:value="record.price" :min="0" :precision="4" :placeholder="record.schemePrice == null ? '' : String(record.schemePrice)" class="full" />
          </template>
          <template v-else-if="column.key === 'serialNos'">
            <a-input v-model:value="record.serialNos" />
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
import LocationSelect from '@/components/wms/LocationSelect.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { addDelivery, deliveryPrices, getDelivery, updateDelivery } from '@/api/dist'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { lineKey, rowKey, useLineItems } from '@/composables/useLineItems'
import { useUserStore } from '@/store/user'

/**
 * 配送单 / 退仓单的新增、修改（草稿）。配送：发货仓库为配送中心的仓库，可指定配送价（需要「修改配送价」权限，不填按规则）；
 * 退仓：发货仓库为门店的仓库，收货仓库为空时取上级配送门店的默认仓库。由要货单生成的行不能改物料
 */
const props = defineProps({ kind: { type: String, required: true } })
const emit = defineEmits(['saved'])
const { t } = useI18n()
const isReturn = computed(() => props.kind === 'return')
const userStore = useUserStore()
const canPrice = computed(() => !isReturn.value && userStore.hasPerm('dist:delivery:price'))

const emptyItem = () => ({ key: rowKey(), requisitionItemId: undefined, requisitionNo: undefined, materialId: undefined, materialLabel: undefined, unit: '', locationId: undefined, batchNo: '', serialNos: '', qty: undefined, price: undefined, schemePrice: undefined, remark: '' })
const itemColumns = computed(() => [
  { title: t('dist.common.material'), key: 'material', width: 300 },
  { title: t('common.field.unit'), key: 'unit', width: 60 },
  { title: t('dist.delivery.location'), key: 'location', width: 150 },
  { title: t('dist.delivery.batch'), key: 'batch', width: 130 },
  { title: t('common.field.quantity'), key: 'qty', width: 130 },
  ...(canPrice.value ? [{ title: t('dist.delivery.price'), key: 'price', width: 120 }] : []),
  { title: t('dist.delivery.serialNos'), key: 'serialNos', width: 160 },
  { title: t('common.field.remark'), key: 'remark', width: 160 },
  { title: '', key: 'action', width: 50 }
])
const rules = computed(() => ({
  fromWarehouseId: [{ required: true, message: t('dist.common.selectWarehouse') }],
  toWarehouseId: [{ required: !isReturn.value, message: t('dist.common.selectWarehouse') }],
  bizDate: [{ required: true, message: t('dist.common.selectDate') }]
}))

const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, fromWarehouseId: undefined, toWarehouseId: undefined, bizDate: dayjs().format('YYYY-MM-DD'), remark: '', items: [emptyItem()] }),
  fetch: async (record) => {
    const d = await getDelivery(props.kind, record.id)
    const f = {
      id: d.id,
      fromWarehouseId: d.fromWarehouseId,
      toWarehouseId: d.toWarehouseId,
      bizDate: d.bizDate,
      remark: d.remark || '',
      items: d.items.map((i) => ({
        key: lineKey(i),
        requisitionItemId: i.requisitionItemId || undefined,
        requisitionNo: i.requisitionNo,
        materialId: i.materialId,
        materialLabel: `${i.materialCode} ${i.materialName}`,
        unit: i.unit,
        locationId: i.locationId || undefined,
        batchNo: i.batchNo || '',
        serialNos: i.serialNos || '',
        qty: Number(i.qty),
        price: i.price == null ? undefined : Number(i.price),
        schemePrice: undefined,
        remark: i.remark || ''
      }))
    }
    await fillPrices(f, f.items)
    return f
  },
  create: async (data) => emit('saved', await addDelivery(props.kind, data)),
  update: async (id, data) => {
    await updateDelivery(props.kind, id, data)
    emit('saved', id)
  },
  toPayload: (f) => ({
    fromWarehouseId: f.fromWarehouseId,
    toWarehouseId: f.toWarehouseId || null,
    bizDate: f.bizDate,
    remark: f.remark || null,
    items: f.items.filter((i) => i.materialId).map((i) => ({
      requisitionItemId: i.requisitionItemId ?? null,
      materialId: i.materialId,
      locationId: i.locationId ?? null,
      batchNo: i.batchNo || null,
      serialNos: i.serialNos || null,
      qty: i.qty,
      price: canPrice.value ? i.price ?? null : null,
      remark: i.remark || null
    }))
  })
})
const { formRef, form } = drawer
const lines = useLineItems(() => form.value.items, { newRow: emptyItem })

/** 加盟店的配送价方案价（作为配送价的占位提示） */
async function fillPrices(f, rows) {
  if (isReturn.value || !canPrice.value) return
  const targets = rows.filter((r) => r.materialId)
  if (!f.toWarehouseId || !targets.length) return
  const map = await deliveryPrices(f.toWarehouseId, targets.map((r) => r.materialId)).catch(() => ({}))
  targets.forEach((r) => (r.schemePrice = map?.[r.materialId] ?? undefined))
}
const refreshPrices = () => fillPrices(form.value, form.value.items)

function onFromChange() {
  form.value.items.forEach((r) => (r.locationId = undefined))
}

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
  gap: 12px;
}
.section-title {
  font-weight: 500;
  white-space: nowrap;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
