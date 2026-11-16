<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('pos.ledger.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('pos.topup.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('store.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('pos.ledger.businessDate')" name="range">
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :exporter="exportAll" :columns="columns" @refresh="load">
        <a-button v-perm="'pos:topup:add'" type="primary" @click="adminRef.open()">
          <template #icon><PlusOutlined /></template>{{ $t('pos.topup.adminTopup') }}
        </a-button>
      </TableToolbar>
      <a-table row-key="id" :size="size" :columns="visibleColumns" :data-source="list" :loading="loading" :pagination="pagination" :scroll="{ x: scrollX, y: '100%' }" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'topupNo'"><a @click="view(record)">{{ record.topupNo }}</a></template>
          <template v-else-if="column.key === 'member'">{{ record.memberName || record.memberCode }}<span class="sub-text"> {{ record.memberPhone }}</span></template>
          <template v-else-if="column.key === 'terminal'">{{ record.terminalName || $t('pos.topup.admin') }}</template>
          <template v-else-if="['amount', 'bonusAmount', 'balanceAfter'].includes(column.key)">{{ money(record[column.key]) }}</template>
          <template v-else-if="column.key === 'payMethods'">{{ payText(record.payMethods) }}</template>
          <template v-else-if="column.key === 'action'"><TableActions :actions="[{ type: 'view', onClick: () => view(record) }]" /></template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="viewOpen" :title="viewing?.topupNo" width="760px" :footer="null">
      <template v-if="viewing">
        <a-descriptions :column="2" size="small" bordered>
          <a-descriptions-item :label="$t('store.common.store')">{{ viewing.storeName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('pos.topup.terminal')">{{ viewing.terminalName || $t('pos.topup.admin') }}</a-descriptions-item>
          <a-descriptions-item :label="$t('pos.marketing.member')">{{ viewing.memberName || '' }} {{ viewing.memberCode }} {{ viewing.memberPhone }}</a-descriptions-item>
          <a-descriptions-item :label="$t('pos.ledger.businessDate')">{{ viewing.businessDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('pos.topup.amount')">{{ money(viewing.amount) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('pos.topup.bonusAmount')">{{ money(viewing.bonusAmount) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('pos.topup.balanceAfter')">{{ money(viewing.balanceAfter) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('pos.ledger.operator')">{{ viewing.createByName }} {{ viewing.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="viewing.remark" :label="$t('common.field.remark')" :span="2">{{ viewing.remark }}</a-descriptions-item>
        </a-descriptions>
        <a-table row-key="id" size="small" :columns="payColumns" :data-source="viewing.payments" :pagination="false" class="gap">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'payMethod'"><DictTag type="pos_pay_method" :value="record.payMethod" /></template>
            <template v-else-if="column.key === 'amount'">{{ money(record.amount) }}</template>
            <template v-else-if="column.key === 'tendered'">{{ record.tendered == null ? '-' : `${money(record.tendered)} / ${money(record.changeAmount)}` }}</template>
          </template>
        </a-table>
      </template>
    </a-modal>

    <AdminTopupModal ref="adminRef" @saved="load" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import StoreSelect from '@/components/store/StoreSelect.vue'
import { getTopup, pageTopup } from '@/api/pos'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { money } from '@/utils/number'
import { dictLabel } from '../pos'
import AdminTopupModal from './AdminTopupModal.vue'

/** 储值充值单：收银台充值与后台充值（pos:topup:add），收款记入门店的结算账户，门店日结生成其他收入单（会员储值） */
const { t } = useI18n()
useDict('pos_pay_method')
const payText = (v) => (v ? v.split(',').map((m) => dictLabel('pos_pay_method', m)).join(' / ') : '-')

const columns = [
  { title: t('pos.topup.topupNo'), key: 'topupNo', dataIndex: 'topupNo', width: 180 },
  { title: t('pos.ledger.businessDate'), dataIndex: 'businessDate', width: 110 },
  { title: t('store.common.store'), dataIndex: 'storeName', width: 130, ellipsis: true },
  { title: t('pos.topup.terminal'), key: 'terminal', width: 110 },
  { title: t('pos.marketing.member'), key: 'member', width: 180, exportValue: (r) => r.memberCode },
  { title: t('pos.topup.amount'), key: 'amount', dataIndex: 'amount', width: 100, align: 'right' },
  { title: t('pos.topup.bonusAmount'), key: 'bonusAmount', dataIndex: 'bonusAmount', width: 100, align: 'right' },
  { title: t('pos.topup.balanceAfter'), key: 'balanceAfter', dataIndex: 'balanceAfter', width: 110, align: 'right' },
  { title: t('pos.topup.payMethods'), key: 'payMethods', width: 130, exportValue: (r) => payText(r.payMethods) },
  { title: t('pos.ledger.operator'), dataIndex: 'createByName', width: 100 },
  { title: t('pos.ledger.time'), dataIndex: 'createTime', width: 160 },
  actionColumn(1)
]
const payColumns = computed(() => [
  { title: t('pos.sale.payMethod'), key: 'payMethod', width: 100 },
  { title: t('common.field.amount'), key: 'amount', width: 100, align: 'right' },
  { title: t('pos.topup.tendered'), key: 'tendered', width: 140, align: 'right' },
  { title: t('pos.topup.account'), dataIndex: 'accountName', ellipsis: true },
  { title: t('pos.topup.refNo'), dataIndex: 'refNo', width: 140 }
])
const { checked, visibleColumns, scrollX } = useColumns(columns, 'pos-topup')
const showSearch = ref(true)
const emptyQuery = () => ({ keyword: undefined, storeId: undefined, range: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageTopup, query, {
  params: ({ range, ...q }) => ({ ...q, begin: range?.[0], end: range?.[1] })
})
onMounted(load)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const adminRef = ref()
const viewOpen = ref(false)
const viewing = ref()
async function view(record) {
  viewing.value = await getTopup(record.id)
  viewOpen.value = true
}
</script>

<style scoped>
.gap {
  margin-top: 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
