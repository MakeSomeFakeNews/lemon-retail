<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card">
      <a-space wrap>
        <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" />
        <a-tree-select
          v-model:value="regionId"
          :tree-data="regions"
          :field-names="{ label: 'name', value: 'id', children: 'children' }"
          allow-clear
          tree-default-expand-all
          :placeholder="$t('dist.compare.region')"
          class="w200"
        />
        <StoreSelect v-model:value="storeId" class="w200" />
        <a-button type="primary" :loading="loading" @click="load">{{ $t('common.action.query') }}</a-button>
        <a-button :disabled="!rows.length" @click="exportRows">{{ $t('common.action.export') }}</a-button>
        <a-tooltip :title="$t('dist.compare.tip')"><InfoCircleOutlined class="sub-text" /></a-tooltip>
      </a-space>
    </a-card>
    <a-card :bordered="false" class="page-card table-card">
      <a-table row-key="storeId" size="middle" :columns="columns" :data-source="rows" :loading="loading" :pagination="false" :scroll="{ x: 2000, y: '100%' }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'storeType'"><DictTag type="store_type" :value="record.storeType" /></template>
          <template v-else-if="column.fmt">{{ column.fmt(record[column.dataIndex]) }}</template>
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
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import { InfoCircleOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import StoreSelect from '@/components/store/StoreSelect.vue'
import { storeCompare } from '@/api/dist'
import { regionTree } from '@/api/store/region'
import { useDict } from '@/composables/useDict'
import { writeExcel } from '@/utils/excel'
import { money, qty } from '@/utils/number'

/** 门店对比：按门店（可按区域、门店筛选）对比销售、毛利、采购 / 配送入库、库存金额与周转、要货满足率、配送及时率 */
const { t } = useI18n()
useDict('store_type')
const range = ref([dayjs().startOf('month').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])
const regionId = ref()
const storeId = ref()
const regions = ref([])
const rows = ref([])
const loading = ref(false)

const pct = (v) => (v == null ? '-' : `${Number(v).toFixed(2)}%`)
const m = (v) => money(v)
const SUM_KEYS = ['salesAmount', 'returnAmount', 'retailAmount', 'retailReturnAmount', 'netSales', 'cost', 'grossProfit', 'purchaseAmount', 'deliveryAmount', 'stockAmount', 'approvedQty', 'deliveredQty']
const columns = [
  { title: t('dist.common.store'), dataIndex: 'storeName', width: 160, fixed: 'left' },
  { title: t('dist.compare.storeType'), key: 'storeType', dataIndex: 'storeType', width: 100 },
  { title: t('dist.compare.region'), dataIndex: 'regionName', width: 110 },
  { title: t('dist.compare.salesAmount'), dataIndex: 'salesAmount', width: 120, align: 'right', fmt: m },
  { title: t('dist.compare.returnAmount'), dataIndex: 'returnAmount', width: 110, align: 'right', fmt: m },
  { title: t('dist.compare.retailAmount'), dataIndex: 'retailAmount', width: 120, align: 'right', fmt: m },
  { title: t('dist.compare.retailReturnAmount'), dataIndex: 'retailReturnAmount', width: 120, align: 'right', fmt: m },
  { title: t('dist.compare.netSales'), dataIndex: 'netSales', width: 120, align: 'right', fmt: m },
  { title: t('dist.compare.cost'), dataIndex: 'cost', width: 120, align: 'right', fmt: m },
  { title: t('dist.compare.grossProfit'), dataIndex: 'grossProfit', width: 120, align: 'right', fmt: m },
  { title: t('dist.compare.grossMargin'), dataIndex: 'grossMargin', width: 90, align: 'right', fmt: pct },
  { title: t('dist.compare.purchaseAmount'), dataIndex: 'purchaseAmount', width: 120, align: 'right', fmt: m },
  { title: t('dist.compare.deliveryAmount'), dataIndex: 'deliveryAmount', width: 120, align: 'right', fmt: m },
  { title: t('dist.compare.stockAmount'), dataIndex: 'stockAmount', width: 120, align: 'right', fmt: m },
  { title: t('dist.compare.turnover'), dataIndex: 'turnover', width: 90, align: 'right', fmt: (v) => (v == null ? '-' : v) },
  { title: t('dist.compare.approvedQty'), dataIndex: 'approvedQty', width: 100, align: 'right', fmt: (v) => qty(v) },
  { title: t('dist.compare.deliveredQty'), dataIndex: 'deliveredQty', width: 100, align: 'right', fmt: (v) => qty(v) },
  { title: t('dist.compare.fillRate'), dataIndex: 'fillRate', width: 100, align: 'right', fmt: pct },
  { title: t('dist.compare.onTimeRate'), dataIndex: 'onTimeRate', width: 100, align: 'right', fmt: pct }
]

function totalCell(c, i) {
  if (i === 0) return t('dist.compare.total')
  if (!SUM_KEYS.includes(c.dataIndex)) return ''
  return c.fmt(rows.value.reduce((s, r) => s + Number(r[c.dataIndex] || 0), 0))
}

async function load() {
  loading.value = true
  try {
    rows.value = await storeCompare({ begin: range.value[0], end: range.value[1], regionId: regionId.value, storeId: storeId.value })
  } finally {
    loading.value = false
  }
}

function exportRows() {
  const headers = columns.map((c) => ({ title: c.title, width: Math.round(c.width / 8) }))
  writeExcel(t('dist.compare.title'), headers, rows.value.map((r) => columns.map((c) => r[c.dataIndex] ?? '')))
}

onMounted(async () => {
  regions.value = await regionTree().catch(() => [])
  load()
})
</script>

<style scoped>
.w200 {
  width: 200px;
}
.sub-text {
  color: var(--app-text-3);
}
</style>
