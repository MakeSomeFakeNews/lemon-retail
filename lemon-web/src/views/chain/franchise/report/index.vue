<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card">
      <a-space wrap>
        <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" />
        <a-select v-model:value="settleMode" :options="settleModes" :placeholder="$t('chain.common.settleMode')" allow-clear class="w160" />
        <a-button type="primary" :loading="loading" @click="load">{{ $t('common.action.query') }}</a-button>
        <a-button :disabled="!rows.length" @click="exportRows">{{ $t('common.action.export') }}</a-button>
        <a-tooltip :title="$t('chain.report.tip')"><InfoCircleOutlined class="sub-text" /></a-tooltip>
      </a-space>
    </a-card>
    <a-card :bordered="false" class="page-card table-card">
      <a-table row-key="storeId" size="middle" :columns="columns" :data-source="rows" :loading="loading" :pagination="false" :scroll="{ x: 1300, y: '100%' }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'store'"><a @click="detailRef.open(record)">{{ record.storeName }}</a></template>
          <template v-else-if="column.key === 'settleMode'"><DictTag type="store_settle_mode" :value="record.settleMode" /></template>
          <template v-else-if="column.money">{{ money(record[column.dataIndex]) }}</template>
        </template>
        <template #summary>
          <a-table-summary fixed>
            <a-table-summary-row>
              <a-table-summary-cell v-for="(c, i) in columns" :key="i" :align="c.align">{{ totalCell(c, i) }}</a-table-summary-cell>
            </a-table-summary-row>
          </a-table-summary>
        </template>
      </a-table>
    </a-card>
    <AccountDetail ref="detailRef" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import { InfoCircleOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import { franchiseSummary } from '@/api/chain'
import { useDict } from '@/composables/useDict'
import { writeExcel } from '@/utils/excel'
import { money } from '@/utils/number'
import AccountDetail from '../account/AccountDetail.vue'

/** 加盟结算汇总：每个加盟店在日期范围内的期初、配送、退货、收款、其他、期末与保证金余额；点击门店看往来明细 */
const { t } = useI18n()
const dict = useDict('store_settle_mode')
const settleModes = computed(() => (dict.store_settle_mode || []).map((d) => ({ value: d.value, label: d.label })))
const range = ref([dayjs().startOf('month').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])
const settleMode = ref()
const rows = ref([])
const loading = ref(false)
const detailRef = ref()

const SUM_KEYS = ['opening', 'deliveryAmount', 'returnAmount', 'paymentAmount', 'otherAmount', 'closing', 'depositBalance']
const columns = [
  { title: t('chain.common.franchiseStore'), key: 'store', dataIndex: 'storeName', width: 180, fixed: 'left' },
  { title: t('chain.common.franchisee'), dataIndex: 'franchisee', width: 140 },
  { title: t('chain.common.settleMode'), key: 'settleMode', dataIndex: 'settleMode', width: 100 },
  { title: t('chain.statement.opening'), dataIndex: 'opening', width: 120, align: 'right', money: true },
  { title: t('chain.statement.delivery'), dataIndex: 'deliveryAmount', width: 120, align: 'right', money: true },
  { title: t('chain.statement.return'), dataIndex: 'returnAmount', width: 120, align: 'right', money: true },
  { title: t('chain.statement.payment'), dataIndex: 'paymentAmount', width: 120, align: 'right', money: true },
  { title: t('chain.statement.other'), dataIndex: 'otherAmount', width: 120, align: 'right', money: true },
  { title: t('chain.statement.closing'), dataIndex: 'closing', width: 120, align: 'right', money: true },
  { title: t('chain.account.depositBalance'), dataIndex: 'depositBalance', width: 120, align: 'right', money: true }
]

function totalCell(c, i) {
  if (i === 0) return t('chain.common.total')
  if (!SUM_KEYS.includes(c.dataIndex)) return ''
  return money(rows.value.reduce((s, r) => s + Number(r[c.dataIndex] || 0), 0))
}

async function load() {
  loading.value = true
  try {
    rows.value = await franchiseSummary({ begin: range.value[0], end: range.value[1], settleMode: settleMode.value })
  } finally {
    loading.value = false
  }
}

function exportRows() {
  const headers = columns.map((c) => ({ title: c.title, width: Math.round(c.width / 8) }))
  writeExcel(t('chain.report.title'), headers, rows.value.map((r) => columns.map((c) => r[c.dataIndex] ?? '')))
}

onMounted(load)
</script>

<style scoped>
.w160 {
  width: 160px;
}
.sub-text {
  color: var(--app-text-3);
}
</style>
