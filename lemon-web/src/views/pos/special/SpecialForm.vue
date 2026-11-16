<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('pos.special.add')" :edit-title="$t('pos.special.edit')">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('pos.special.name')" name="name">
            <a-input v-model:value="form.name" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="10">
          <a-form-item :label="$t('pos.special.period')" name="period">
            <a-range-picker v-model:value="form.period" show-time value-format="YYYY-MM-DD HH:mm:ss" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="6">
          <a-form-item :label="$t('pos.special.channel')" name="channel">
            <a-select v-model:value="form.channel" :options="toOptions(dict.pos_sale_channel)" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('pos.special.storeScope')" name="storeScope">
            <a-radio-group v-model:value="form.storeScope" :options="toOptions(dict.pos_special_scope)" @change="form.targetIds = []" />
          </a-form-item>
        </a-col>
        <a-col v-if="form.storeScope === SpecialScope.STORES" :xs="24" :sm="16">
          <a-form-item :label="$t('pos.special.stores')" name="targetIds">
            <StoreSelect v-model:value="form.targetIds" multiple all />
          </a-form-item>
        </a-col>
        <a-col v-if="form.storeScope === SpecialScope.REGIONS" :xs="24" :sm="16">
          <a-form-item :label="$t('pos.special.regions')" name="targetIds" :extra="$t('pos.special.regionsTip')">
            <a-tree-select
              v-model:value="form.targetIds"
              :tree-data="regions"
              :field-names="{ label: 'name', value: 'id', children: 'children' }"
              multiple
              tree-default-expand-all
              show-search
              tree-node-filter-prop="name"
              allow-clear
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('pos.special.memberOnly')" name="memberOnly">
            <a-switch v-model:checked="form.memberOnly" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="16">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>
      <div class="items-head">
        <span class="section-title">{{ $t('pos.special.items') }}</span>
        <a-space>
          <span class="sub-text">{{ $t('pos.special.itemsTip') }}</span>
          <a-button size="small" @click="lines.addRow()"><template #icon><PlusOutlined /></template>{{ $t('common.action.addRow') }}</a-button>
        </a-space>
      </div>
      <LineItemsTable :items="form.items" :columns="itemColumns" :min-rows="1">
        <template #cell="{ column, record }">
          <template v-if="column.key === 'material'">
            <a-form-item :name="lines.itemName(record, 'materialId')" :rules="[{ required: true, message: $t('pos.special.materialRequired') }]" class="cell-item">
              <MaterialSelect v-model:value="record.materialId" :label="record.materialLabel" @select="(m) => Object.assign(record, { materialLabel: m.label, unit: m.unit, retailPrice: m.retailPrice })" />
            </a-form-item>
          </template>
          <template v-else-if="column.key === 'unit'">{{ record.unit }}</template>
          <template v-else-if="column.key === 'retailPrice'">{{ record.retailPrice == null ? '-' : price(record.retailPrice) }}</template>
          <template v-else-if="column.key === 'specialPrice'">
            <a-input-number v-model:value="record.specialPrice" :min="0" :precision="2" class="full" :disabled="record.discountRate != null" />
          </template>
          <template v-else-if="column.key === 'discountRate'">
            <a-input-number v-model:value="record.discountRate" :min="1" :max="100" :precision="2" addon-after="%" class="full" :disabled="record.specialPrice != null" />
          </template>
          <template v-else-if="column.key === 'limitQty'">
            <a-input-number v-model:value="record.limitQty" :min="0.0001" :precision="4" class="full" :placeholder="$t('pos.special.unlimited')" />
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
import StoreSelect from '@/components/store/StoreSelect.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { addSpecial, getSpecial, updateSpecial } from '@/api/pos'
import { regionTree } from '@/api/store/region'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { lineKey, rowKey, useLineItems } from '@/composables/useLineItems'
import { toOptions } from '@/utils/dict'
import { price } from '@/utils/number'
import { SaleChannel, SpecialScope } from '@/constants/enums'

/**
 * 特价活动的新增、修改（草稿）：活动时间、渠道、适用门店（全部 / 指定门店 / 指定区域含下级）、仅限会员，商品的特价或折扣、每单限购。
 * 价格为基本单位的含税价；折扣按门店价（门店价格方案价或总部零售价）计算
 */
const emit = defineEmits(['saved'])
const { t } = useI18n()
const dict = useDict('pos_sale_channel', 'pos_special_scope')
const regions = ref([])

const emptyItem = () => ({ key: rowKey(), materialId: undefined, materialLabel: undefined, unit: '', retailPrice: undefined, specialPrice: undefined, discountRate: undefined, limitQty: undefined })
const itemColumns = computed(() => [
  { title: t('pos.special.material'), key: 'material', width: 300 },
  { title: t('common.field.unit'), key: 'unit', width: 70 },
  { title: t('pos.special.retailPrice'), key: 'retailPrice', width: 100, align: 'right' },
  { title: t('pos.special.specialPrice'), key: 'specialPrice', width: 130 },
  { title: t('pos.special.discountRate'), key: 'discountRate', width: 130 },
  { title: t('pos.special.limitQty'), key: 'limitQty', width: 130 },
  { title: '', key: 'action', width: 50 }
])
const rules = computed(() => ({
  name: [{ required: true, message: t('pos.special.nameRequired') }],
  period: [{ required: true, type: 'array', len: 2, message: t('pos.special.periodRequired') }],
  targetIds: [{ required: form.value.storeScope !== SpecialScope.ALL, type: 'array', min: 1, message: t('pos.special.targetsRequired') }]
}))

const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, name: '', period: [], channel: SaleChannel.POS, storeScope: SpecialScope.ALL, targetIds: [], memberOnly: false, remark: '', items: [emptyItem()] }),
  beforeOpen: async () => {
    if (!regions.value.length) regions.value = await regionTree().catch(() => [])
  },
  fetch: async (record) => {
    const d = await getSpecial(record.id)
    return {
      id: d.id,
      name: d.name,
      period: [d.beginTime, d.endTime],
      channel: d.channel,
      storeScope: d.storeScope,
      targetIds: d.targetIds || [],
      memberOnly: !!d.memberOnly,
      remark: d.remark || '',
      items: d.items.map((i) => ({
        key: lineKey(i),
        materialId: i.materialId,
        materialLabel: `${i.materialCode} ${i.materialName}`,
        unit: i.unit,
        retailPrice: i.retailPrice,
        specialPrice: i.specialPrice == null ? undefined : Number(i.specialPrice),
        discountRate: i.discountRate == null ? undefined : Number(i.discountRate),
        limitQty: i.limitQty == null ? undefined : Number(i.limitQty)
      }))
    }
  },
  create: async (data) => emit('saved', await addSpecial(data)),
  update: async (id, data) => {
    await updateSpecial(id, data)
    emit('saved', id)
  },
  toPayload: (f) => ({
    name: f.name,
    beginTime: f.period[0],
    endTime: f.period[1],
    channel: f.channel,
    storeScope: f.storeScope,
    targetIds: f.storeScope === SpecialScope.ALL ? [] : f.targetIds,
    memberOnly: f.memberOnly,
    remark: f.remark || null,
    items: f.items.filter((i) => i.materialId).map((i) => ({ materialId: i.materialId, specialPrice: i.specialPrice ?? null, discountRate: i.discountRate ?? null, limitQty: i.limitQty ?? null }))
  })
})
const { formRef, form } = drawer
const lines = useLineItems(() => form.value.items, { newRow: emptyItem })

defineExpose({ openCreate: () => drawer.openCreate(), openEdit: (r) => drawer.openEdit(r) })
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
