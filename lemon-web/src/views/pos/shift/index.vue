<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('store.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.pos_shift_status)" />
        </a-form-item>
        <a-form-item :label="$t('pos.shift.businessDate')" name="range">
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :exporter="exportAll" :columns="columns" @refresh="load" />
      <a-table row-key="id" :size="size" :columns="visibleColumns" :data-source="list" :loading="loading" :pagination="pagination" :scroll="{ x: scrollX, y: '100%' }" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'shiftNo'"><a @click="show(record)">{{ record.shiftNo }}</a></template>
          <template v-else-if="column.key === 'status'"><DictTag type="pos_shift_status" :value="record.status" /></template>
          <template v-else-if="['openingCash', 'saleAmount', 'returnAmount', 'expectedCash', 'countedCash'].includes(column.key)">
            {{ record.status === ShiftStatus.OPEN && column.key !== 'openingCash' ? '-' : money(record[column.key]) }}
          </template>
          <template v-else-if="column.key === 'cashDiff'">
            <span :class="{ neg: record.cashDiff < 0 }">{{ record.cashDiff == null ? '-' : money(record.cashDiff) }}</span>
          </template>
          <template v-else-if="column.key === 'action'"><TableActions :actions="rowActions(record)" /></template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="detailOpen" :title="report ? $t('pos.shift.reportTitle', { no: report.shift.shiftNo }) : ''" width="720px" :footer="null">
      <ShiftReportView v-if="report" :report="report" />
    </a-modal>
    <a-modal v-model:open="closeOpen" :title="$t('pos.shift.forceClose')" :confirm-loading="saving" @ok="forceClose">
      <a-form layout="vertical">
        <a-form-item :label="$t('pos.shift.countedCash')">
          <a-input-number v-model:value="counted" :min="0" :precision="2" class="full" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import StoreSelect from '@/components/store/StoreSelect.vue'
import { closeShift, pageShift, shiftReport } from '@/api/pos'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { money } from '@/utils/number'
import { ShiftStatus } from '@/constants/enums'
import { printShift } from '../printing'
import ShiftReportView from './ShiftReportView.vue'

/** 班次记录：开班、交班时间，系统金额与实点现金、差额；查看、打印交班单，代交班（pos:shift:close） */
const { t } = useI18n()
const dict = useDict('pos_shift_status')

const columns = [
  { title: t('pos.shift.shiftNo'), key: 'shiftNo', dataIndex: 'shiftNo', width: 150 },
  { title: t('store.common.store'), dataIndex: 'storeName', width: 140, ellipsis: true },
  { title: t('pos.shift.terminal'), dataIndex: 'terminalName', width: 120, ellipsis: true },
  { title: t('pos.shift.cashier'), dataIndex: 'cashierName', width: 100 },
  { title: t('pos.shift.openTime'), dataIndex: 'openTime', width: 160 },
  { title: t('pos.shift.closeTime'), dataIndex: 'closeTime', width: 160 },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 80, dict: 'pos_shift_status' },
  { title: t('pos.shift.openingCash'), key: 'openingCash', dataIndex: 'openingCash', width: 100, align: 'right' },
  { title: t('pos.shift.saleAmount'), key: 'saleAmount', dataIndex: 'saleAmount', width: 110, align: 'right' },
  { title: t('pos.shift.returnAmount'), key: 'returnAmount', dataIndex: 'returnAmount', width: 110, align: 'right' },
  { title: t('pos.shift.expectedCash'), key: 'expectedCash', dataIndex: 'expectedCash', width: 110, align: 'right' },
  { title: t('pos.shift.countedCash'), key: 'countedCash', dataIndex: 'countedCash', width: 110, align: 'right' },
  { title: t('pos.shift.cashDiff'), key: 'cashDiff', dataIndex: 'cashDiff', width: 100, align: 'right' },
  actionColumn(3)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'pos-shift')
const showSearch = ref(true)
const emptyQuery = () => ({ storeId: undefined, status: undefined, range: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageShift, query, {
  params: ({ range, ...q }) => ({ ...q, begin: range?.[0], end: range?.[1] })
})
onMounted(load)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const detailOpen = ref(false)
const report = ref()
async function show(record) {
  report.value = await shiftReport(record.id)
  detailOpen.value = true
}
async function print(record) {
  printShift(await shiftReport(record.id))
}

const closeOpen = ref(false)
const saving = ref(false)
const counted = ref()
const target = ref()
function openForceClose(record) {
  target.value = record
  counted.value = undefined
  closeOpen.value = true
}
async function forceClose() {
  if (counted.value == null) return
  saving.value = true
  try {
    await closeShift(target.value.id, { countedCash: counted.value })
    message.success(t('pos.shift.closed'))
    closeOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

const rowActions = (record) => [
  { type: 'view', onClick: () => show(record) },
  { type: 'print', onClick: () => print(record) },
  { type: 'confirm', label: t('pos.shift.forceClose'), perm: 'pos:shift:close', show: record.status === ShiftStatus.OPEN, onClick: () => openForceClose(record) }
]
</script>

<style scoped>
.full {
  width: 100%;
}
.neg {
  color: #d4380d;
}
</style>
