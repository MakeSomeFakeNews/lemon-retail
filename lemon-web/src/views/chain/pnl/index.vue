<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card">
      <a-space wrap>
        <a-radio-group v-model:value="mode" button-style="solid" @change="load">
          <a-radio-button value="range">{{ $t('chain.pnl.byRange') }}</a-radio-button>
          <a-radio-button value="grid">{{ $t('chain.pnl.byMonth') }}</a-radio-button>
        </a-radio-group>
        <a-range-picker v-if="mode === 'range'" v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" />
        <a-range-picker v-else v-model:value="months" picker="month" value-format="YYYY-MM" :allow-clear="false" />
        <a-tree-select
          v-model:value="regionId"
          :tree-data="regions"
          :field-names="{ label: 'name', value: 'id', children: 'children' }"
          allow-clear
          tree-default-expand-all
          :placeholder="$t('chain.common.region')"
          class="w180"
        />
        <StoreSelect v-model:value="storeId" class="w180" />
        <a-select v-if="mode === 'grid'" v-model:value="metric" :options="metricOptions" class="w160" />
        <a-button type="primary" :loading="loading" @click="load">{{ $t('common.action.query') }}</a-button>
        <a-button :disabled="!rows.length" @click="exportRows">{{ $t('common.action.export') }}</a-button>
        <a-tooltip :title="$t('chain.pnl.tip')"><InfoCircleOutlined class="sub-text" /></a-tooltip>
      </a-space>
    </a-card>
    <a-card :bordered="false" class="page-card table-card">
      <a-table
        v-if="mode === 'range'"
        :row-key="(r) => r.storeId ?? 'total'"
        size="middle"
        :columns="rangeColumns"
        :data-source="rows"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 2400, y: '100%' }"
        :row-class-name="(r) => (r.storeId == null ? 'total-row' : '')"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'store'">
            <a v-if="record.storeId != null" @click="drill(record.storeId, range[0], range[1])">{{ record.storeName }}</a>
            <b v-else>{{ $t('chain.common.total') }}</b>
          </template>
          <template v-else-if="column.key === 'storeType'"><DictTag type="store_type" :value="record.storeType" /></template>
          <template v-else-if="column.fmt">{{ column.fmt(record[column.dataIndex]) }}</template>
        </template>
      </a-table>
      <a-table
        v-else
        row-key="storeId"
        size="middle"
        :columns="gridColumns"
        :data-source="gridRows"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 300 + monthList.length * 120, y: '100%' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'store'">
            <a @click="drill(record.storeId, monthStart(months[0]), monthEnd(months[1]))">{{ record.storeName }}</a>
          </template>
          <template v-else-if="column.month !== undefined">{{ fmtMetric(record.cells[column.month ?? 'total']) }}</template>
        </template>
      </a-table>
    </a-card>
    <PnlDetail ref="detailRef" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import { InfoCircleOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import StoreSelect from '@/components/store/StoreSelect.vue'
import { storePnl, storePnlGrid } from '@/api/chain'
import { regionTree } from '@/api/store/region'
import { useDict } from '@/composables/useDict'
import { writeExcel } from '@/utils/excel'
import { money } from '@/utils/number'
import PnlDetail from './PnlDetail.vue'
import { PNL_METRICS, pct } from './pnl'

/** 门店损益（RP-10）：按日期范围每个门店一行，或门店 × 月份查看某一项；点击门店穿透到各月与费用类别 */
const { t } = useI18n()
useDict('store_type')
const mode = ref('range')
const range = ref([dayjs().startOf('month').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])
const months = ref([dayjs().subtract(5, 'month').format('YYYY-MM'), dayjs().format('YYYY-MM')])
const regionId = ref()
const storeId = ref()
const regions = ref([])
const rows = ref([])
const loading = ref(false)
const metric = ref('operatingProfit')
const detailRef = ref()

const metricOptions = PNL_METRICS.map((m) => ({ value: m.key, label: t(m.label) }))
const rangeColumns = [
  { title: t('chain.common.store'), key: 'store', dataIndex: 'storeName', width: 170, fixed: 'left' },
  { title: t('chain.pnl.storeType'), key: 'storeType', dataIndex: 'storeType', width: 90 },
  { title: t('chain.common.region'), dataIndex: 'regionName', width: 110 },
  ...PNL_METRICS.map((m) => ({ title: t(m.label), dataIndex: m.key, width: m.pct ? 100 : 120, align: 'right', fmt: m.pct ? pct : (v) => money(v) }))
]

const monthList = computed(() => {
  const list = []
  if (!months.value?.[0]) return list
  for (let m = dayjs(months.value[0]); !m.isAfter(dayjs(months.value[1])); m = m.add(1, 'month')) list.push(m.format('YYYY-MM'))
  return list
})
const gridColumns = computed(() => [
  { title: t('chain.common.store'), key: 'store', dataIndex: 'storeName', width: 180, fixed: 'left' },
  ...monthList.value.map((m) => ({ title: m, key: m, month: m, width: 120, align: 'right' })),
  { title: t('chain.common.total'), key: 'total', month: null, width: 130, align: 'right', fixed: 'right' }
])
/** 按门店合并：cells[月份] = 该月的行，cells.total = 合计行 */
const gridRows = computed(() => {
  const map = new Map()
  for (const r of rows.value) {
    if (!map.has(r.storeId)) map.set(r.storeId, { storeId: r.storeId, storeName: r.storeName, cells: {} })
    map.get(r.storeId).cells[r.period || 'total'] = r
  }
  return [...map.values()]
})
const metricDef = computed(() => PNL_METRICS.find((m) => m.key === metric.value))
const fmtMetric = (cell) => (cell ? (metricDef.value.pct ? pct(cell[metric.value]) : money(cell[metric.value])) : '-')
const monthStart = (m) => dayjs(m).startOf('month').format('YYYY-MM-DD')
const monthEnd = (m) => dayjs(m).endOf('month').format('YYYY-MM-DD')

async function load() {
  loading.value = true
  try {
    rows.value =
      mode.value === 'range'
        ? await storePnl({ begin: range.value[0], end: range.value[1], regionId: regionId.value, storeId: storeId.value })
        : await storePnlGrid({ beginMonth: months.value[0], endMonth: months.value[1], regionId: regionId.value, storeId: storeId.value })
  } finally {
    loading.value = false
  }
}

function drill(id, begin, end) {
  detailRef.value.open(id, begin, end)
}

function exportRows() {
  if (mode.value === 'range') {
    const cols = rangeColumns.filter((c) => c.dataIndex)
    writeExcel(t('chain.pnl.title'), cols.map((c) => ({ title: c.title, width: Math.round(c.width / 8) })),
      rows.value.map((r) => cols.map((c) => (c.dataIndex === 'storeName' && r.storeId == null ? t('chain.common.total') : r[c.dataIndex] ?? ''))))
  } else {
    const headers = [{ title: t('chain.common.store'), width: 22 }, ...monthList.value.map((m) => ({ title: m, width: 14 })), { title: t('chain.common.total'), width: 14 }]
    writeExcel(`${t('chain.pnl.title')}_${metricOptions.find((o) => o.value === metric.value).label}`, headers,
      gridRows.value.map((g) => [g.storeName, ...monthList.value.map((m) => g.cells[m]?.[metric.value] ?? ''), g.cells.total?.[metric.value] ?? '']))
  }
}

onMounted(async () => {
  regions.value = await regionTree().catch(() => [])
  load()
})
</script>

<style scoped>
.w180 {
  width: 180px;
}
.w160 {
  width: 160px;
}
.sub-text {
  color: var(--app-text-3);
}
:deep(.total-row) {
  font-weight: 600;
}
</style>
