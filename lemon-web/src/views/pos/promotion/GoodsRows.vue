<template>
  <a-table row-key="key" size="small" :columns="columns" :data-source="rows" :pagination="false">
    <template #bodyCell="{ column, record, index }">
      <template v-if="column.key === 'type'">
        <a-select v-model:value="record.category" :options="typeOptions" class="full" @change="clear(record)" />
      </template>
      <template v-else-if="column.key === 'target'">
        <MaterialCategorySelect v-if="record.category" v-model="record.categoryCode" class="full" />
        <MaterialSelect v-else v-model:value="record.materialId" :label="record.materialLabel" @select="(m) => (record.materialLabel = m.label)" />
      </template>
      <template v-else-if="column.key === 'action'">
        <a-button type="text" danger size="small" @click="rows.splice(index, 1)"><DeleteOutlined /></a-button>
      </template>
    </template>
  </a-table>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined } from '@ant-design/icons-vue'
import MaterialCategorySelect from '@/components/wms/MaterialCategorySelect.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { dictLabel } from '../pos'
import { GoodsTargetType } from '@/constants/enums'

/**
 * 商品范围编辑（不积分的商品 / 分类、满减活动的参与商品）：每行一个商品或一个分类（含下级分类）。
 * fixed 为 'material' / 'category' 时所有行同一类型、不显示类型列
 */
const props = defineProps({ rows: { type: Array, required: true }, fixed: { type: String, default: undefined } })
const { t } = useI18n()

const typeOptions = computed(() => [
  { value: false, label: dictLabel('pos_goods_target_type', GoodsTargetType.MATERIAL) },
  { value: true, label: dictLabel('pos_goods_target_type', GoodsTargetType.CATEGORY) }
])
const columns = computed(() => [
  ...(props.fixed ? [] : [{ title: t('pos.rule.targetType'), key: 'type', width: 120 }]),
  { title: t('pos.rule.target'), key: 'target' },
  { title: '', key: 'action', width: 50 }
])

function clear(r) {
  r.materialId = undefined
  r.materialLabel = undefined
  r.categoryCode = undefined
}
</script>

<style scoped>
.full {
  width: 100%;
}
</style>
