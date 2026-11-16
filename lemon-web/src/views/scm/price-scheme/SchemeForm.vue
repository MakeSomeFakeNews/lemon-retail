<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('scm.priceScheme.add')" :edit-title="$t('scm.priceScheme.editTitle')" width="min(1000px, 100vw)">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('common.field.code')" name="code">
            <a-input v-model:value="form.code" :maxlength="32" :placeholder="$t('scm.priceScheme.codePh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="10">
          <a-form-item :label="$t('common.field.name')" name="name">
            <a-input v-model:value="form.name" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="6">
          <a-form-item :label="$t('common.field.status')" name="status">
            <a-radio-group v-model:value="form.status" :options="toOptions(dict.sys_common_status, true)" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>
      <div class="items-head">
        <span class="section-title">{{ $t('scm.priceScheme.items') }}</span>
        <a-space>
          <span class="sub-text">{{ $t('scm.priceScheme.itemsTip') }}</span>
          <a-button size="small" @click="lines.addRow()"><template #icon><PlusOutlined /></template>{{ $t('common.action.addRow') }}</a-button>
        </a-space>
      </div>
      <LineItemsTable :items="form.items" :columns="itemColumns" :pagination="form.items.length > 50 ? { pageSize: 50 } : false">
        <template #cell="{ column, record }">
          <template v-if="column.key === 'material'">
            <a-form-item :name="lines.itemName(record, 'materialId')" :rules="[{ required: true, message: $t('scm.priceScheme.materialRequired') }]" class="cell-item">
              <MaterialSelect v-model:value="record.materialId" :label="record.materialLabel" @select="(m) => applyMaterial(record, m)" />
            </a-form-item>
          </template>
          <template v-else-if="column.key === 'unit'">{{ record.unit }}</template>
          <template v-else-if="column.key === 'hqRetailPrice'">{{ record.hqRetailPrice == null ? '-' : price(record.hqRetailPrice) }}</template>
          <template v-else-if="column.key === 'retailPrice'">
            <a-input-number v-model:value="record.retailPrice" :min="0" :precision="4" :placeholder="$t('scm.priceScheme.notOverride')" class="full" />
          </template>
          <template v-else-if="column.key === 'memberPrice'">
            <a-input-number v-model:value="record.memberPrice" :min="0" :precision="4" :placeholder="$t('scm.priceScheme.notOverride')" class="full" />
          </template>
        </template>
      </LineItemsTable>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import LineItemsTable from '@/components/common/LineItemsTable.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { addPriceScheme, getPriceScheme, updatePriceScheme } from '@/api/scm/priceScheme'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { lineKey, rowKey, useLineItems } from '@/composables/useLineItems'
import { toOptions } from '@/utils/dict'
import { price } from '@/utils/number'
import { STATUS_ENABLED } from '@/utils/constants'

/** 价格方案的新增、修改：表头 + 商品价格（零售价、会员价，留空表示不覆盖、按总部价） */
const emit = defineEmits(['saved'])
const { t } = useI18n()
const dict = useDict('sys_common_status')

const emptyItem = () => ({ key: rowKey(), materialId: undefined, materialLabel: undefined, unit: '', hqRetailPrice: undefined, retailPrice: undefined, memberPrice: undefined })
const itemColumns = [
  { title: t('scm.priceScheme.material'), key: 'material', width: 320 },
  { title: t('common.field.unit'), key: 'unit', width: 70 },
  { title: t('scm.priceScheme.hqRetailPrice'), key: 'hqRetailPrice', width: 120, align: 'right' },
  { title: t('scm.priceScheme.retailPrice'), key: 'retailPrice', width: 150 },
  { title: t('scm.priceScheme.memberPrice'), key: 'memberPrice', width: 150 },
  { title: '', key: 'action', width: 50 }
]
const rules = {
  name: [{ required: true, message: t('scm.priceScheme.nameRequired') }]
}

const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, code: '', name: '', status: STATUS_ENABLED, remark: '', items: [] }),
  fetch: async (record) => {
    const d = await getPriceScheme(record.id)
    return {
      id: d.id,
      code: d.code,
      name: d.name,
      status: d.status,
      remark: d.remark || '',
      items: d.items.map((i) => ({
        key: lineKey(i),
        materialId: i.materialId,
        materialLabel: `${i.materialCode} ${i.materialName}`,
        unit: i.unit,
        hqRetailPrice: i.hqRetailPrice,
        retailPrice: i.retailPrice == null ? undefined : Number(i.retailPrice),
        memberPrice: i.memberPrice == null ? undefined : Number(i.memberPrice)
      }))
    }
  },
  create: async (data) => emit('saved', await addPriceScheme(data)),
  update: async (id, data) => {
    await updatePriceScheme(id, data)
    emit('saved', id)
  },
  toPayload: (f) => ({
    code: f.code?.trim() || null,
    name: f.name,
    status: f.status,
    remark: f.remark || null,
    items: f.items.filter((i) => i.materialId).map((i) => ({
      materialId: i.materialId,
      retailPrice: i.retailPrice ?? null,
      memberPrice: i.memberPrice ?? null
    }))
  })
})
const { formRef, form } = drawer
const lines = useLineItems(() => form.value.items, { newRow: emptyItem })

function applyMaterial(row, m) {
  Object.assign(row, { materialLabel: m.label, unit: m.unit, hqRetailPrice: undefined })
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
