<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('store.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.pos_daily_close_status)" />
        </a-form-item>
        <a-form-item :label="$t('pos.daily.businessDate')" name="range">
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :exporter="exportAll" :columns="columns" @refresh="load">
        <a-button v-perm="'pos:dailyClose:close'" type="primary" @click="openClose">
          <template #icon><CalendarOutlined /></template>{{ $t('pos.daily.close') }}
        </a-button>
      </TableToolbar>
      <a-table row-key="id" :size="size" :columns="visibleColumns" :data-source="list" :loading="loading" :pagination="pagination" :scroll="{ x: scrollX, y: '100%' }" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'businessDate'"><a @click="show(record.id)">{{ record.businessDate }}</a></template>
          <template v-else-if="column.key === 'status'"><DictTag type="pos_daily_close_status" :value="record.status" /></template>
          <template v-else-if="['saleAmount', 'returnAmount', 'netAmount', 'expectedCash', 'countedCash'].includes(column.key)">{{ money(record[column.key]) }}</template>
          <template v-else-if="column.key === 'cashDiff'"><span :class="{ neg: record.cashDiff < 0 }">{{ money(record.cashDiff) }}</span></template>
          <template v-else-if="column.key === 'action'"><TableActions :actions="rowActions(record)" /></template>
        </template>
      </a-table>
    </a-card>

    <!-- 日结：选择门店、营业日期，确认汇总后日结 -->
    <a-modal v-model:open="closeOpen" :title="$t('pos.daily.close')" width="720px" :confirm-loading="saving" :ok-button-props="{ disabled: !summary || summary.closed || summary.openShifts > 0 }" @ok="submitClose">
      <a-form layout="inline" class="gap">
        <a-form-item :label="$t('store.common.store')">
          <StoreSelect v-model:value="closeForm.storeId" :allow-clear="false" class="store" @update:value="loadSummary" />
        </a-form-item>
        <a-form-item :label="$t('pos.daily.businessDate')">
          <a-date-picker v-model:value="closeForm.businessDate" value-format="YYYY-MM-DD" :allow-clear="false" @change="loadSummary" />
        </a-form-item>
      </a-form>
      <template v-if="summary">
        <a-alert v-if="summary.closed" type="warning" show-icon :message="$t('pos.daily.alreadyClosed')" class="gap" />
        <a-alert v-else-if="summary.openShifts" type="error" show-icon :message="$t('pos.daily.openShifts', { n: summary.openShifts })" class="gap" />
        <SummaryView :sales="summary.sales" :pays="summary.pays" :net-amount="summary.netAmount" :shift-count="summary.shiftCount"
                     :expected-cash="summary.expectedCash" :counted-cash="summary.countedCash" :cash-diff="summary.cashDiff" />
        <a-input v-model:value="closeForm.remark" :placeholder="$t('common.field.remark')" :maxlength="255" class="gap" />
        <div class="sub-text gap">{{ $t('pos.daily.closeTip') }}</div>
      </template>
    </a-modal>

    <a-modal v-model:open="detailOpen" :title="detail ? `${detail.storeName} ${detail.businessDate}` : ''" width="720px" :footer="null">
      <template v-if="detail">
        <SummaryView :sales="detail" :pays="detail.pays" :net-amount="detail.netAmount" :shift-count="detail.shiftCount"
                     :expected-cash="detail.expectedCash" :counted-cash="detail.countedCash" :cash-diff="detail.cashDiff" />
        <div class="sub-text gap">{{ $t('pos.daily.closedInfo', { by: detail.closedByName || '-', time: detail.closedTime || '-' }) }}</div>
        <div v-if="detail.reopenedTime" class="sub-text">{{ $t('pos.daily.reopenedInfo', { by: detail.reopenedByName || '-', time: detail.reopenedTime }) }}</div>
      </template>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { CalendarOutlined, UndoOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import StoreSelect from '@/components/store/StoreSelect.vue'
import { closeDay, dailySummary, getDailyClose, pageDailyClose, reopenDay } from '@/api/pos'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useRouteOpen } from '@/composables/useRouteOpen'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { money } from '@/utils/number'
import { DailyCloseStatus } from '@/constants/enums'
import SummaryView from './SummaryView.vue'

/**
 * 门店日结：汇总当日销售、退货、各支付方式收款与各班次的现金差额；日结时按结算账户生成待确认的其他收入单（零售收入）/ 费用支出单（零售退款），
 * 之后该门店当天不能再收银、退货、作废、开班。反日结（pos:dailyClose:reopen）删除生成的收支单（须未确认）
 */
const { t } = useI18n()
const dict = useDict('pos_daily_close_status', 'pos_pay_method')
const today = () => new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10)

const columns = [
  { title: t('pos.daily.businessDate'), key: 'businessDate', dataIndex: 'businessDate', width: 120 },
  { title: t('store.common.store'), dataIndex: 'storeName', width: 160, ellipsis: true },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 90, dict: 'pos_daily_close_status' },
  { title: t('pos.daily.saleCount'), dataIndex: 'saleCount', width: 80, align: 'right' },
  { title: t('pos.daily.saleAmount'), key: 'saleAmount', dataIndex: 'saleAmount', width: 110, align: 'right' },
  { title: t('pos.daily.returnAmount'), key: 'returnAmount', dataIndex: 'returnAmount', width: 110, align: 'right' },
  { title: t('pos.daily.netAmount'), key: 'netAmount', dataIndex: 'netAmount', width: 110, align: 'right' },
  { title: t('pos.daily.expectedCash'), key: 'expectedCash', dataIndex: 'expectedCash', width: 110, align: 'right' },
  { title: t('pos.daily.countedCash'), key: 'countedCash', dataIndex: 'countedCash', width: 110, align: 'right' },
  { title: t('pos.daily.cashDiff'), key: 'cashDiff', dataIndex: 'cashDiff', width: 100, align: 'right' },
  { title: t('pos.daily.closedBy'), dataIndex: 'closedByName', width: 100 },
  { title: t('pos.daily.closedTime'), dataIndex: 'closedTime', width: 160 },
  actionColumn(2)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'pos-daily-close')
const showSearch = ref(true)
const emptyQuery = () => ({ storeId: undefined, status: undefined, range: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageDailyClose, query, {
  params: ({ range, ...q }) => ({ ...q, begin: range?.[0], end: range?.[1] })
})
useRouteOpen({ id: (id) => show(id) })
onMounted(load)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const closeOpen = ref(false)
const saving = ref(false)
const summary = ref(null)
const closeForm = reactive({ storeId: undefined, businessDate: today(), remark: '' })
function openClose() {
  Object.assign(closeForm, { storeId: query.storeId, businessDate: today(), remark: '' })
  summary.value = null
  closeOpen.value = true
  if (closeForm.storeId) loadSummary()
}
async function loadSummary() {
  if (!closeForm.storeId || !closeForm.businessDate) return
  summary.value = await dailySummary(closeForm.storeId, closeForm.businessDate)
}
async function submitClose() {
  saving.value = true
  try {
    await closeDay({ ...closeForm, remark: closeForm.remark || null })
    message.success(t('pos.daily.closed'))
    closeOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

const detailOpen = ref(false)
const detail = ref()
async function show(id) {
  detail.value = await getDailyClose(id)
  detailOpen.value = true
}

const reopen = useAction((r) => reopenDay(r.id), {
  confirm: (r) => ({ title: t('pos.daily.confirmReopen', { store: r.storeName, date: r.businessDate }), content: t('pos.daily.reopenContent'), okType: 'danger' }),
  reload: () => load()
})
const rowActions = (record) => [
  { type: 'view', onClick: () => show(record.id) },
  { key: 'reopen', icon: UndoOutlined, label: t('pos.daily.reopen'), danger: true, perm: 'pos:dailyClose:reopen', show: record.status === DailyCloseStatus.CLOSED, onClick: () => reopen(record) }
]
</script>

<style scoped>
.gap {
  margin-top: 12px;
}
.store {
  width: 220px;
}
.neg {
  color: #d4380d;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
