<template>
  <a-table
    row-key="id"
    size="small"
    :columns="columns"
    :data-source="list"
    :loading="loading"
    :pagination="pagination"
    :scroll="{ x: scrollX }"
    @change="onChange"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'material'">
        <div>{{ record.materialCode }} {{ record.materialName }}</div>
        <div class="sub-text">{{ record.spec }}</div>
      </template>
      <template v-else-if="column.key === 'target'">
        <span v-if="record.target === PriceAdjustTarget.SCHEME">{{ record.schemeCode }} {{ record.schemeName }}</span>
        <DictTag v-else type="scm_price_adjust_target" :value="record.target" />
      </template>
      <template v-else-if="column.key === 'retail'">
        <PriceChange :old="record.oldRetailPrice" :new="record.newRetailPrice" />
      </template>
      <template v-else-if="column.key === 'member'">
        <PriceChange :old="record.oldMemberPrice" :new="record.newMemberPrice" />
      </template>
    </template>
  </a-table>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import { priceHistory } from '@/api/scm/priceAdjust'
import { useDict } from '@/composables/useDict'
import { PriceAdjustTarget } from '@/constants/enums'
import PriceChange from './PriceChange.vue'

/** 调价历史表格：按调价单或按物料（可再按方案、对象筛选），按生效时间倒序分页 */
const props = defineProps({
  adjustId: Number,
  materialId: Number,
  schemeId: Number,
  target: String
})
const { t } = useI18n()
useDict('scm_price_adjust_target')

const columns = computed(() => [
  { title: t('scm.priceAdjust.effectTime'), dataIndex: 'effectTime', width: 170 },
  ...(props.adjustId ? [] : [{ title: t('scm.priceAdjust.adjustNo'), dataIndex: 'adjustNo', width: 160 }]),
  ...(props.materialId ? [] : [{ title: t('scm.priceAdjust.material'), key: 'material', width: 240 }]),
  { title: t('scm.priceAdjust.targetOrScheme'), key: 'target', width: 180 },
  { title: t('scm.priceAdjust.retailChange'), key: 'retail', width: 180 },
  { title: t('scm.priceAdjust.memberChange'), key: 'member', width: 180 }
])
const scrollX = computed(() => columns.value.reduce((s, c) => s + c.width, 0))

const list = ref([])
const loading = ref(false)
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: false })
async function load() {
  if (!props.adjustId && !props.materialId) {
    list.value = []
    pagination.total = 0
    return
  }
  loading.value = true
  try {
    const res = await priceHistory({
      current: pagination.current,
      size: pagination.pageSize,
      adjustId: props.adjustId,
      materialId: props.materialId,
      schemeId: props.schemeId,
      target: props.target
    })
    list.value = res.records
    pagination.total = res.total
  } finally {
    loading.value = false
  }
}
function onChange(p) {
  pagination.current = p.current
  load()
}
watch(() => [props.adjustId, props.materialId, props.schemeId, props.target], () => {
  pagination.current = 1
  load()
})
onMounted(load)
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
