<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('pos.sale.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('pos.sale.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('pos.sale.kind')" name="kind">
          <a-select v-model:value="query.kind" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.pos_sale_kind)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.pos_sale_status)" />
        </a-form-item>
        <a-form-item :label="$t('store.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('pos.sale.businessDate')" name="range">
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :exporter="exportAll" :columns="columns" @refresh="load" />
      <a-table row-key="id" :size="size" :columns="visibleColumns" :data-source="list" :loading="loading" :pagination="pagination" :scroll="{ x: scrollX, y: '100%' }" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'saleNo'">
            <a @click="detailRef.open(record.id)">{{ record.saleNo }}</a>
            <div v-if="record.origSaleNo" class="sub-text">{{ $t('pos.sale.origNo', { no: record.origSaleNo }) }}</div>
          </template>
          <template v-else-if="column.key === 'kind'"><DictTag type="pos_sale_kind" :value="record.kind" /></template>
          <template v-else-if="column.key === 'status'"><DictTag type="pos_sale_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'member'">
            <span v-if="record.memberCode">{{ record.memberName || record.memberCode }}<span class="sub-text"> {{ record.memberPhone }}</span></span>
            <span v-else class="sub-text">{{ $t('pos.sale.walkIn') }}</span>
          </template>
          <template v-else-if="column.key === 'payMethods'">{{ payText(record.payMethods) }}</template>
          <template v-else-if="['amount', 'orderDiscount', 'payableAmount'].includes(column.key)">{{ money(record[column.key]) }}</template>
          <template v-else-if="column.key === 'action'"><TableActions :actions="rowActions(record)" /></template>
        </template>
      </a-table>
    </a-card>

    <SaleDetail ref="detailRef" @changed="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import StoreSelect from '@/components/store/StoreSelect.vue'
import { getSale, pageSale } from '@/api/pos'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useRouteOpen } from '@/composables/useRouteOpen'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { money } from '@/utils/number'
import { POS_DICTS, dictLabel } from '../pos'
import { printReceipt } from '../printing'
import SaleDetail from './SaleDetail.vue'

/** POS 销售单 / 退货单：按门店范围过滤；详情中可作废当天的销售单、重试失败的退款、补打小票 */
const { t } = useI18n()
const dict = useDict(...POS_DICTS)
const payText = (v) => (v ? v.split(',').map((m) => dictLabel('pos_pay_method', m)).join(' / ') : '-')

const columns = [
  { title: t('pos.sale.saleNo'), key: 'saleNo', dataIndex: 'saleNo', width: 190 },
  { title: t('pos.sale.kind'), key: 'kind', dataIndex: 'kind', width: 70, dict: 'pos_sale_kind' },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 80, dict: 'pos_sale_status' },
  { title: t('pos.sale.saleTime'), dataIndex: 'saleTime', width: 160 },
  { title: t('store.common.store'), dataIndex: 'storeName', width: 130, ellipsis: true },
  { title: t('pos.sale.terminal'), dataIndex: 'terminalName', width: 110, ellipsis: true },
  { title: t('pos.sale.cashier'), dataIndex: 'cashierName', width: 90 },
  { title: t('pos.sale.member'), key: 'member', width: 160, exportValue: (r) => r.memberCode || '' },
  { title: t('pos.sale.amount'), key: 'amount', dataIndex: 'amount', width: 100, align: 'right' },
  { title: t('pos.sale.orderDiscount'), key: 'orderDiscount', dataIndex: 'orderDiscount', width: 90, align: 'right' },
  { title: t('pos.sale.payable'), key: 'payableAmount', dataIndex: 'payableAmount', width: 100, align: 'right' },
  { title: t('pos.sale.payMethods'), key: 'payMethods', width: 140, exportValue: (r) => payText(r.payMethods) },
  actionColumn(2)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'pos-sale')
const showSearch = ref(true)
const emptyQuery = () => ({ keyword: undefined, kind: undefined, status: undefined, storeId: undefined, range: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageSale, query, {
  params: ({ range, ...q }) => ({ ...q, begin: range?.[0], end: range?.[1] })
})
const detailRef = ref()
useRouteOpen({ id: (id) => detailRef.value?.open(id), keyword: (kw) => (query.keyword = kw), reload: () => search() })
onMounted(load)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const rowActions = (record) => [
  { type: 'view', onClick: () => detailRef.value.open(record.id) },
  { type: 'print', onClick: async () => printReceipt(await getSale(record.id)) }
]
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
