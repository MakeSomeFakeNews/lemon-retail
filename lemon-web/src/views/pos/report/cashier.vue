<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card">
      <SearchForm :model="query" @search="load" @reset="resetQuery">
        <a-form-item :label="$t('pos.report.groupBy')" name="groupBy">
          <a-select v-model:value="query.groupBy" :options="toOptions(dict.pos_report_dimension)" class="sel" />
        </a-form-item>
        <a-form-item :label="$t('pos.report.dateRange')" name="range">
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" :allow-clear="false" />
        </a-form-item>
        <a-form-item :label="$t('store.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('pos.report.terminal')" name="terminalId">
          <a-select v-model:value="query.terminalId" allow-clear :placeholder="$t('common.placeholder.all')" :options="terminalOptions" />
        </a-form-item>
      </SearchForm>
    </a-card>
    <a-card :bordered="false" class="page-card table-card">
      <a-table row-key="groupKey" size="middle" :columns="columns" :data-source="rows" :loading="loading" :pagination="false" :scroll="{ x: 1200, y: '100%' }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'group'">{{ record.groupName || record.groupKey || $t('pos.report.total') }}</template>
          <template v-else-if="column.money">{{ money(column.pay ? record.pays?.[column.pay] : record[column.key]) }}</template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import SearchForm from '@/components/SearchForm.vue'
import StoreSelect from '@/components/store/StoreSelect.vue'
import { cashierReport, terminalOptions as fetchTerminals } from '@/api/pos'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { money } from '@/utils/number'
import { PayMethod, PosReportDimension } from '@/constants/enums'
import { dictLabel } from '../pos'

/** 收银日报 / 收银员报表：按日期、门店、收银台、收银员或班次汇总销售、退货和各支付方式的净收款（收款 - 退款），最后一行为合计 */
const { t } = useI18n()
const dict = useDict('pos_report_dimension', 'pos_pay_method')
const today = () => new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10)
const emptyQuery = () => ({ groupBy: PosReportDimension.DATE, range: [today(), today()], storeId: undefined, terminalId: undefined })
const query = reactive(emptyQuery())
const rows = ref([])
const loading = ref(false)
const terminals = ref([])
const terminalOptions = computed(() => terminals.value.map((x) => ({ value: x.id, label: `${x.name}（${x.storeName}）` })))

const columns = computed(() => [
  { title: dictLabel('pos_report_dimension', query.groupBy), key: 'group', width: 180, fixed: 'left' },
  { title: t('pos.report.saleCount'), dataIndex: 'saleCount', width: 90, align: 'right' },
  { title: t('pos.report.saleAmount'), key: 'saleAmount', width: 120, align: 'right', money: true },
  { title: t('pos.report.returnCount'), dataIndex: 'returnCount', width: 90, align: 'right' },
  { title: t('pos.report.returnAmount'), key: 'returnAmount', width: 120, align: 'right', money: true },
  { title: t('pos.report.netAmount'), key: 'netAmount', width: 120, align: 'right', money: true },
  ...Object.values(PayMethod).map((m) => ({ title: dictLabel('pos_pay_method', m), key: `pay-${m}`, pay: m, width: 110, align: 'right', money: true })),
  { title: t('pos.report.payTotal'), key: 'payTotal', width: 120, align: 'right', money: true }
])

async function load() {
  loading.value = true
  try {
    const r = await cashierReport({ groupBy: query.groupBy, storeId: query.storeId, terminalId: query.terminalId, begin: query.range?.[0], end: query.range?.[1] })
    rows.value = [...r.rows, { ...r.total, groupKey: '__total', groupName: t('pos.report.total') }]
  } finally {
    loading.value = false
  }
}
function resetQuery() {
  Object.assign(query, emptyQuery())
  load()
}
onMounted(async () => {
  load()
  terminals.value = await fetchTerminals().catch(() => [])
})
</script>

<style scoped>
.sel {
  width: 140px;
}
</style>
