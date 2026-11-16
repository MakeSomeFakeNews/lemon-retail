<template>
  <a-drawer v-model:open="visible" :title="title" width="min(1100px, 100vw)" destroy-on-close>
    <a-spin :spinning="loading">
      <a-table row-key="period" size="small" :columns="monthColumns" :data-source="data?.months || []" :pagination="false" :scroll="{ x: 1600 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.fmt">{{ column.fmt(record[column.dataIndex]) }}</template>
        </template>
      </a-table>
      <a-row :gutter="16" class="mt16">
        <a-col :xs="24" :md="12">
          <div class="section">{{ $t('chain.pnl.expenseByCategory') }}</div>
          <a-table row-key="categoryId" size="small" :columns="categoryColumns" :data-source="data?.expenses || []" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'amount'">{{ money(record.amount) }}</template>
            </template>
          </a-table>
        </a-col>
        <a-col :xs="24" :md="12">
          <div class="section">{{ $t('chain.pnl.incomeByCategory') }}</div>
          <a-table row-key="categoryId" size="small" :columns="categoryColumns" :data-source="data?.incomes || []" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'amount'">{{ money(record.amount) }}</template>
            </template>
          </a-table>
        </a-col>
      </a-row>
    </a-spin>
  </a-drawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { storePnlDetail } from '@/api/chain'
import { money } from '@/utils/number'
import { PNL_METRICS, pct } from './pnl'

/** 门店损益穿透：门店在日期范围内的各月损益，费用支出、其他收入按收支类别 */
const { t } = useI18n()
const visible = ref(false)
const loading = ref(false)
const data = ref()
const range = ref([])
const title = computed(() => (data.value?.months?.[0] ? t('chain.pnl.detailTitle', { name: data.value.months[0].storeName, begin: range.value[0], end: range.value[1] }) : ''))

const monthColumns = [
  { title: t('chain.pnl.month'), dataIndex: 'period', width: 90, fixed: 'left' },
  ...PNL_METRICS.map((m) => ({ title: t(m.label), dataIndex: m.key, width: m.pct ? 90 : 110, align: 'right', fmt: m.pct ? pct : (v) => money(v) }))
]
const categoryColumns = [
  { title: t('chain.pnl.category'), dataIndex: 'categoryName' },
  { title: t('chain.pnl.docCount'), dataIndex: 'docCount', width: 80, align: 'right' },
  { title: t('chain.common.amount'), key: 'amount', width: 120, align: 'right' }
]

async function open(storeId, begin, end) {
  range.value = [begin, end]
  data.value = undefined
  visible.value = true
  loading.value = true
  try {
    data.value = await storePnlDetail({ storeId, begin, end })
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.mt16 {
  margin-top: 16px;
}
.section {
  margin-bottom: 8px;
  font-weight: 500;
}
</style>
