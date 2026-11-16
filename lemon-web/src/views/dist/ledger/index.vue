<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card">
      <a-space wrap>
        <StoreSelect v-model:value="query.storeId" class="w200" @update:value="refresh" />
        <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" @change="refresh" />
        <a-input-search v-if="tab === 'detail'" v-model:value="query.keyword" allow-clear :placeholder="$t('dist.common.transferNo')" class="w200" @search="refresh" />
        <a-tooltip :title="$t('dist.ledger.tip')"><InfoCircleOutlined class="sub-text" /></a-tooltip>
      </a-space>
    </a-card>
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" @change="refresh">
        <a-tab-pane key="summary" :tab="$t('dist.ledger.summaryTab')" />
        <a-tab-pane key="detail" :tab="$t('dist.ledger.detailTab')" />
      </a-tabs>
      <a-table
        v-if="tab === 'summary'"
        :row-key="(r) => `${r.fromStoreId}-${r.toStoreId}`"
        size="middle"
        :columns="summaryColumns"
        :data-source="summary"
        :loading="summaryLoading"
        :pagination="false"
        :scroll="{ x: 800, y: '100%' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.money">{{ money(record[column.dataIndex]) }}</template>
          <template v-else-if="column.key === 'diff'">{{ money(Number(record.amount || 0) - Number(record.costAmount || 0)) }}</template>
        </template>
      </a-table>
      <a-table
        v-else
        row-key="id"
        size="middle"
        :columns="detailColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1100, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.money">{{ money(record[column.dataIndex]) }}</template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { InfoCircleOutlined } from '@ant-design/icons-vue'
import StoreSelect from '@/components/store/StoreSelect.vue'
import { ledgerSummary, pageLedger } from '@/api/dist'
import { useTable } from '@/composables/useTable'
import { money } from '@/utils/number'

/** 门店往来：跨门店差价调拨（含加盟配送）的内部结算，按 调出门店 → 调入门店 汇总或查看明细 */
const { t } = useI18n()
const tab = ref('summary')
const query = reactive({ storeId: undefined, range: undefined, keyword: undefined })
const toParams = ({ range, ...q }) => ({ ...q, begin: range?.[0], end: range?.[1] })

const summaryColumns = [
  { title: t('dist.ledger.fromStore'), dataIndex: 'fromStoreName', width: 180 },
  { title: t('dist.ledger.toStore'), dataIndex: 'toStoreName', width: 180 },
  { title: t('dist.ledger.entryCount'), dataIndex: 'entryCount', width: 80, align: 'right' },
  { title: t('dist.ledger.costAmount'), dataIndex: 'costAmount', width: 130, align: 'right', money: true },
  { title: t('dist.ledger.amount'), dataIndex: 'amount', width: 130, align: 'right', money: true },
  { title: t('dist.ledger.diff'), key: 'diff', width: 120, align: 'right' }
]
const detailColumns = [
  { title: t('dist.common.bizDate'), dataIndex: 'bizDate', width: 110 },
  { title: t('dist.common.transferNo'), dataIndex: 'transferNo', width: 160 },
  { title: t('dist.transit.sourceNo'), dataIndex: 'sourceNo', width: 160 },
  { title: t('dist.ledger.fromStore'), dataIndex: 'fromStoreName', width: 150 },
  { title: t('dist.common.fromWarehouse'), dataIndex: 'fromWarehouseName', width: 130 },
  { title: t('dist.ledger.toStore'), dataIndex: 'toStoreName', width: 150 },
  { title: t('dist.common.toWarehouse'), dataIndex: 'toWarehouseName', width: 130 },
  { title: t('dist.ledger.costAmount'), dataIndex: 'costAmount', width: 120, align: 'right', money: true },
  { title: t('dist.ledger.amount'), dataIndex: 'amount', width: 120, align: 'right', money: true }
]

const { list, loading, pagination, search, onTableChange } = useTable(pageLedger, query, { params: toParams })
const summary = ref([])
const summaryLoading = ref(false)
async function loadSummary() {
  summaryLoading.value = true
  try {
    const { keyword, ...q } = toParams({ ...query })
    summary.value = await ledgerSummary(q)
  } finally {
    summaryLoading.value = false
  }
}
const refresh = () => (tab.value === 'summary' ? loadSummary() : search())
onMounted(loadSummary)
</script>

<style scoped>
.w200 {
  width: 200px;
}
.sub-text {
  color: var(--app-text-3);
}
</style>
