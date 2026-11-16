<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('pos.report.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('pos.report.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('pos.report.dateRange')" name="range">
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" />
        </a-form-item>
        <a-form-item :label="$t('store.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('pos.report.terminal')" name="terminalId">
          <a-select v-model:value="query.terminalId" allow-clear :placeholder="$t('common.placeholder.all')" :options="terminalOptions" />
        </a-form-item>
        <a-form-item :label="$t('pos.report.material')" name="materialId">
          <MaterialSelect v-model:value="query.materialId" />
        </a-form-item>
        <a-form-item :label="$t('pos.sale.kind')" name="kind">
          <a-select v-model:value="query.kind" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.pos_sale_kind)" />
        </a-form-item>
      </SearchForm>
    </a-card>
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :exporter="exportAll" :columns="columns" @refresh="load" />
      <a-table :row-key="(r, i) => `${r.saleId}-${i}`" :size="size" :columns="visibleColumns" :data-source="list" :loading="loading" :pagination="pagination" :scroll="{ x: scrollX, y: '100%' }" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'saleNo'">
            <router-link :to="{ path: '/pos/sale', query: { id: record.saleId } }">{{ record.saleNo }}</router-link>
          </template>
          <template v-else-if="column.key === 'kind'"><DictTag type="pos_sale_kind" :value="record.kind" /></template>
          <template v-else-if="column.key === 'material'">
            <div>{{ record.materialName }}</div>
            <div class="sub-text">{{ record.materialCode }} {{ record.spec || '' }}</div>
          </template>
          <template v-else-if="column.key === 'qty'">{{ qty(record.qty) }}</template>
          <template v-else-if="column.key === 'priceSource'"><DictTag type="pos_price_source" :value="record.priceSource" /></template>
          <template v-else-if="['listPrice', 'salePrice', 'amount', 'finalAmount'].includes(column.key)">{{ money(record[column.key]) }}</template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import StoreSelect from '@/components/store/StoreSelect.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { saleDetailReport, terminalOptions as fetchTerminals } from '@/api/pos'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { money, qty } from '@/utils/number'

/** POS 销售明细（按行）：退货行的数量、金额为负；按门店、收银台、收银员、会员、商品、日期筛选，按门店范围过滤 */
const { t } = useI18n()
const dict = useDict('pos_sale_kind', 'pos_price_source')
const terminals = ref([])
const terminalOptions = computed(() => terminals.value.map((x) => ({ value: x.id, label: `${x.name}（${x.storeName}）` })))

const columns = [
  { title: t('pos.sale.saleNo'), key: 'saleNo', dataIndex: 'saleNo', width: 190 },
  { title: t('pos.sale.kind'), key: 'kind', dataIndex: 'kind', width: 70, dict: 'pos_sale_kind' },
  { title: t('pos.sale.saleTime'), dataIndex: 'saleTime', width: 160 },
  { title: t('store.common.store'), dataIndex: 'storeName', width: 130, ellipsis: true },
  { title: t('pos.sale.terminal'), dataIndex: 'terminalName', width: 110, ellipsis: true },
  { title: t('pos.sale.cashier'), dataIndex: 'cashierName', width: 90 },
  { title: t('pos.sale.member'), dataIndex: 'memberName', width: 100 },
  { title: t('pos.sale.material'), key: 'material', width: 220, exportColumns: [
    { title: t('pos.report.materialCode'), dataIndex: 'materialCode' },
    { title: t('pos.report.materialName'), dataIndex: 'materialName' }
  ] },
  { title: t('common.field.unit'), dataIndex: 'unit', width: 60 },
  { title: t('common.field.quantity'), key: 'qty', dataIndex: 'qty', width: 90, align: 'right' },
  { title: t('pos.sale.listPrice'), key: 'listPrice', dataIndex: 'listPrice', width: 90, align: 'right' },
  { title: t('pos.sale.salePrice'), key: 'salePrice', dataIndex: 'salePrice', width: 90, align: 'right' },
  { title: t('pos.sale.priceSource'), key: 'priceSource', dataIndex: 'priceSource', width: 90, dict: 'pos_price_source' },
  { title: t('pos.sale.lineAmount'), key: 'amount', dataIndex: 'amount', width: 100, align: 'right' },
  { title: t('pos.sale.finalAmount'), key: 'finalAmount', dataIndex: 'finalAmount', width: 100, align: 'right' }
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'pos-sale-detail')
const showSearch = ref(true)
const emptyQuery = () => ({ keyword: undefined, range: undefined, storeId: undefined, terminalId: undefined, materialId: undefined, kind: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(saleDetailReport, query, {
  params: ({ range, ...q }) => ({ ...q, begin: range?.[0], end: range?.[1] })
})
onMounted(async () => {
  load()
  terminals.value = await fetchTerminals().catch(() => [])
})

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
