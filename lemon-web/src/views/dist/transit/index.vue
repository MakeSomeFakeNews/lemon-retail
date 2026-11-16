<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="doSearch" @reset="resetQuery">
        <a-form-item :label="$t('dist.common.transferNo')" name="keyword">
          <a-input v-model:value="query.keyword" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('dist.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" />
        </a-form-item>
        <a-form-item :label="$t('dist.common.toStore')" name="toStoreId">
          <StoreSelect v-model:value="query.toStoreId" all />
        </a-form-item>
        <a-form-item :label="$t('dist.common.material')" name="materialId">
          <MaterialSelect v-model:value="query.materialId" />
        </a-form-item>
        <a-form-item :label="$t('dist.common.dateRange')" name="range">
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :exporter="exportAll" :columns="columns" @refresh="reload">
        <span class="sub-text">
          <a-tooltip :title="$t('dist.transit.tip')"><InfoCircleOutlined /></a-tooltip>
          {{ totalText }}
        </span>
      </TableToolbar>
      <a-table
        row-key="transferId"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: scrollX, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'totalQty'">{{ qty(record.totalQty) }}</template>
          <template v-else-if="column.key === 'transferAmount'">{{ record.transferAmount == null ? '-' : money(record.transferAmount) }}</template>
          <template v-else-if="column.key === 'days'">
            <a-tag :color="record.days > 3 ? 'red' : record.days > 1 ? 'orange' : 'green'">{{ record.days }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { InfoCircleOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import StoreSelect from '@/components/store/StoreSelect.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { pageTransit, transitTotal } from '@/api/dist'
import { useColumns } from '@/composables/useColumns'
import { useTable } from '@/composables/useTable'
import { money, qty } from '@/utils/number'

/** 在途查询：已调出未收货的调拨单（配送、退仓、收货差异、手工在途调拨），按调出 / 调入门店、在途天数，合计用于在途对账 */
const { t } = useI18n()
const columns = [
  { title: t('dist.common.transferNo'), dataIndex: 'transferNo', width: 160 },
  { title: t('dist.transit.sourceNo'), dataIndex: 'sourceNo', width: 160 },
  { title: t('dist.common.bizDate'), dataIndex: 'bizDate', width: 110 },
  { title: t('dist.transit.days'), key: 'days', dataIndex: 'days', width: 90, align: 'center' },
  { title: t('dist.common.fromStore'), dataIndex: 'fromStoreName', width: 130, ellipsis: true },
  { title: t('dist.common.fromWarehouse'), dataIndex: 'fromWarehouseName', width: 130, ellipsis: true },
  { title: t('dist.common.toStore'), dataIndex: 'toStoreName', width: 130, ellipsis: true },
  { title: t('dist.common.toWarehouse'), dataIndex: 'toWarehouseName', width: 130, ellipsis: true },
  { title: t('dist.common.totalQty'), key: 'totalQty', dataIndex: 'totalQty', width: 100, align: 'right' },
  { title: t('dist.transit.transferAmount'), key: 'transferAmount', dataIndex: 'transferAmount', width: 120, align: 'right' }
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'dist-transit')
const showSearch = ref(true)
const query = reactive({ keyword: undefined, storeId: undefined, toStoreId: undefined, materialId: undefined, range: undefined })
const toParams = ({ range, ...q }) => ({ ...q, begin: range?.[0], end: range?.[1] })
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageTransit, query, { params: toParams })

const total = ref()
const totalText = computed(() => {
  if (!total.value) return ''
  const base = t('dist.transit.summary', { count: total.value.docCount, qty: qty(total.value.totalQty) })
  return total.value.transferAmount == null ? base : base + t('dist.transit.amountSummary', { amount: money(total.value.transferAmount) })
})
async function loadTotal() {
  total.value = await transitTotal(toParams({ ...query })).catch(() => null)
}
function reload() {
  load()
  loadTotal()
}
function doSearch() {
  search()
  loadTotal()
}
onMounted(reload)

function resetQuery() {
  Object.assign(query, { keyword: undefined, storeId: undefined, toStoreId: undefined, materialId: undefined, range: undefined })
  doSearch()
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
