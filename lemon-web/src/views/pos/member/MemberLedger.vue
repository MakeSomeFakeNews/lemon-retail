<template>
  <a-table row-key="id" size="small" :columns="columns" :data-source="list" :loading="loading" :pagination="pagination" @change="onChange">
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'changeType'"><DictTag :type="dictType" :value="record.changeType" /></template>
      <template v-else-if="column.key === 'change'">
        <span :class="Number(record[valueKey]) < 0 ? 'neg' : 'pos'">{{ format(record[valueKey], true) }}</span>
      </template>
      <template v-else-if="column.key === 'balanceAfter'">{{ format(record.balanceAfter) }}</template>
    </template>
  </a-table>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import { memberPointsLog, memberStoredValueLog } from '@/api/pos'
import { useDict } from '@/composables/useDict'
import { money, qty } from '@/utils/number'

/** 会员的积分 / 储值流水（会员详情的标签页，全部门店） */
const props = defineProps({ memberId: Number, kind: { type: String, default: 'points' } })
const { t } = useI18n()
useDict('pos_points_type', 'pos_stored_value_type')

const isPoints = computed(() => props.kind === 'points')
const dictType = computed(() => (isPoints.value ? 'pos_points_type' : 'pos_stored_value_type'))
const valueKey = computed(() => (isPoints.value ? 'points' : 'amount'))
const list = ref([])
const loading = ref(false)
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const columns = computed(() => [
  { title: t('pos.ledger.time'), dataIndex: 'createTime', width: 160 },
  { title: t('pos.ledger.changeType'), key: 'changeType', width: 110 },
  { title: isPoints.value ? t('pos.ledger.points') : t('pos.ledger.amount'), key: 'change', width: 110, align: 'right' },
  { title: t('pos.ledger.balanceAfter'), key: 'balanceAfter', width: 110, align: 'right' },
  { title: t('store.common.store'), dataIndex: 'storeName', width: 140, ellipsis: true },
  { title: t('pos.ledger.sourceNo'), dataIndex: 'sourceNo', width: 170 },
  { title: t('pos.ledger.remark'), key: 'remark', dataIndex: isPoints.value ? 'reason' : 'remark', ellipsis: true }
])

const format = (v, signed) => {
  const s = isPoints.value ? qty(v) : money(v)
  return signed && Number(v) > 0 ? `+${s}` : s
}

async function load() {
  if (!props.memberId) return
  loading.value = true
  try {
    const fn = isPoints.value ? memberPointsLog : memberStoredValueLog
    const res = await fn(props.memberId, { current: pagination.current, size: pagination.pageSize })
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

watch(
  () => props.memberId,
  () => {
    pagination.current = 1
    load()
  },
  { immediate: true }
)

defineExpose({ reload: load })
</script>

<style scoped>
.pos {
  color: #389e0d;
}
.neg {
  color: #cf1322;
}
</style>
