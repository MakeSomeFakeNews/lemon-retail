<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" :exporter="exportAll" @refresh="load">
        <StoreSelect v-model:value="query.storeId" :types="[StoreType.FRANCHISE]" class="w200" @update:value="search" />
        <a-date-picker v-model:value="query.period" picker="month" value-format="YYYY-MM" :placeholder="$t('chain.statement.period')" @change="search" />
        <a-select v-model:value="query.status" :options="statusOptions" :placeholder="$t('common.field.status')" allow-clear class="w140" @change="search" />
        <a-button v-perm="'chain:statement:add'" type="primary" @click="openGenerate">
          <template #icon><PlusOutlined /></template>{{ $t('chain.statement.generate') }}
        </a-button>
        <a-tooltip :title="$t('chain.statement.tip')"><InfoCircleOutlined class="sub-text" /></a-tooltip>
      </TableToolbar>
      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: scrollX, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'statementNo'"><a @click="openDetail(record.id)">{{ record.statementNo }}</a></template>
          <template v-else-if="column.key === 'status'"><DictTag type="chain_statement_status" :value="record.status" /></template>
          <template v-else-if="column.money">{{ money(record[column.dataIndex]) }}</template>
          <template v-else-if="column.key === 'action'">
            <TableActions
              :actions="[
                { type: 'detail', onClick: () => openDetail(record.id) },
                { type: 'confirm', perm: 'chain:statement:confirm', show: record.status === FranchiseStatementStatus.DRAFT, onClick: () => confirm(record) },
                { type: 'unaudit', label: $t('chain.statement.unconfirm'), perm: 'chain:statement:confirm', show: record.status === FranchiseStatementStatus.CONFIRMED, onClick: () => unconfirm(record) },
                {
                  type: 'delete',
                  perm: 'chain:statement:remove',
                  show: record.status === FranchiseStatementStatus.DRAFT,
                  confirm: $t('chain.statement.confirmDelete', { no: record.statementNo }),
                  onClick: () => remove(record.id)
                }
              ]"
            />
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="generateOpen" :title="$t('chain.statement.generate')" :confirm-loading="saving" @ok="generate">
      <a-alert type="info" show-icon :message="$t('chain.statement.generateTip')" class="mb12" />
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item :label="$t('chain.common.franchiseStore')" name="storeId">
          <StoreSelect v-model:value="form.storeId" :types="[StoreType.FRANCHISE]" :allow-clear="false" />
        </a-form-item>
        <a-form-item :label="$t('chain.statement.period')" name="period">
          <a-date-picker v-model:value="form.period" picker="month" value-format="YYYY-MM" class="full" />
        </a-form-item>
        <a-form-item :label="$t('common.field.remark')" name="remark"><a-input v-model:value="form.remark" :maxlength="255" /></a-form-item>
      </a-form>
    </a-modal>

    <SubPage v-model:open="detailOpen" :title="detail ? $t('chain.statement.detailTitle', { no: detail.statementNo }) : ''">
      <template v-if="detail">
        <a-space class="mb12">
          <a-button @click="print"><template #icon><PrinterOutlined /></template>{{ $t('common.action.print') }}</a-button>
        </a-space>
        <a-descriptions bordered size="small" :column="{ xs: 1, sm: 2, md: 3 }" class="mb12">
          <a-descriptions-item :label="$t('chain.common.franchiseStore')">{{ detail.storeName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('chain.common.franchisee')">{{ detail.franchisee || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('chain.statement.period')">{{ detail.beginDate }} ~ {{ detail.endDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('chain.statement.opening')">{{ money(detail.opening) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('chain.statement.delivery')">{{ money(detail.deliveryAmount) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('chain.statement.return')">{{ money(detail.returnAmount) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('chain.statement.payment')">{{ money(detail.paymentAmount) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('chain.statement.other')">{{ money(detail.otherAmount) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('chain.statement.closing')">{{ money(detail.closing) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')"><DictTag type="chain_statement_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item :label="$t('chain.statement.confirmBy')">{{ detail.confirmByName || '-' }} {{ detail.confirmTime || '' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.remark')">{{ detail.remark || '-' }}</a-descriptions-item>
        </a-descriptions>
        <StatementRows ref="rowsRef" :rows="detail.rows || []" />
      </template>
    </SubPage>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import { InfoCircleOutlined, PlusOutlined, PrinterOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import StoreSelect from '@/components/store/StoreSelect.vue'
import {
  confirmFranchiseStatement,
  deleteFranchiseStatement,
  generateFranchiseStatement,
  getFranchiseStatement,
  pageFranchiseStatement,
  unconfirmFranchiseStatement
} from '@/api/chain'
import { FranchiseStatementStatus, StoreType } from '@/constants/enums'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { findDict } from '@/utils/dict'
import { money, moneyOrBlank } from '@/utils/number'
import { printDocs } from '@/utils/print'
import { printName as n } from '@/utils/printTemplates'
import StatementRows from '../StatementRows.vue'

/** 加盟对账单（月结）：按加盟店 + 月份生成，期初 + 配送 − 退货 + 其他 − 收款 = 期末；确认后锁定，可打印给加盟商核对 */
const { t } = useI18n()
const dict = useDict('chain_statement_status')
const statusOptions = computed(() => (dict.chain_statement_status || []).map((d) => ({ value: d.value, label: d.label })))
const query = reactive({ storeId: undefined, period: undefined, status: undefined })
const columns = [
  { title: t('chain.statement.statementNo'), key: 'statementNo', dataIndex: 'statementNo', width: 160, fixed: 'left' },
  { title: t('chain.common.franchiseStore'), dataIndex: 'storeName', width: 160 },
  { title: t('chain.statement.period'), dataIndex: 'period', width: 90 },
  { title: t('chain.statement.opening'), dataIndex: 'opening', width: 110, align: 'right', money: true },
  { title: t('chain.statement.delivery'), dataIndex: 'deliveryAmount', width: 110, align: 'right', money: true },
  { title: t('chain.statement.return'), dataIndex: 'returnAmount', width: 110, align: 'right', money: true },
  { title: t('chain.statement.payment'), dataIndex: 'paymentAmount', width: 110, align: 'right', money: true },
  { title: t('chain.statement.other'), dataIndex: 'otherAmount', width: 110, align: 'right', money: true },
  { title: t('chain.statement.closing'), dataIndex: 'closing', width: 110, align: 'right', money: true },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 90, dict: 'chain_statement_status' },
  { title: t('chain.statement.confirmBy'), dataIndex: 'confirmByName', width: 100 },
  actionColumn(4)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'chain-franchise-statement')
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageFranchiseStatement, query)
onMounted(load)

const generateOpen = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({ storeId: undefined, period: undefined, remark: '' })
const rules = {
  storeId: [{ required: true, message: t('chain.common.selectStore') }],
  period: [{ required: true, message: t('chain.statement.selectPeriod') }]
}
function openGenerate() {
  Object.assign(form, { storeId: query.storeId, period: dayjs().subtract(1, 'month').format('YYYY-MM'), remark: '' })
  generateOpen.value = true
}
async function generate() {
  await formRef.value.validate()
  saving.value = true
  try {
    const id = await generateFranchiseStatement({ ...form })
    generateOpen.value = false
    load()
    openDetail(id)
  } finally {
    saving.value = false
  }
}

const detailOpen = ref(false)
const detail = ref()
const rowsRef = ref()
async function openDetail(id) {
  detail.value = await getFranchiseStatement(id)
  detailOpen.value = true
}
const refreshDetail = async () => {
  load()
  if (detailOpen.value && detail.value) detail.value = await getFranchiseStatement(detail.value.id)
}
const confirm = useAction((r) => confirmFranchiseStatement(r.id), {
  confirm: (r) => t('chain.statement.confirmConfirm', { no: r.statementNo }),
  reload: () => refreshDetail()
})
const unconfirm = useAction((r) => unconfirmFranchiseStatement(r.id), { reload: () => refreshDetail() })
const remove = useAction(deleteFranchiseStatement, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

function print() {
  const d = detail.value
  const status = findDict(dict.chain_statement_status, d.status)?.label || d.status
  printDocs(
    {
      title: n('franchiseStatement'),
      no: d.statementNo,
      fields: [
        [n('franchiseStore'), d.storeName, false, n('franchiseStore')],
        [n('franchisee'), d.franchisee || '-', false, n('franchisee')],
        [n('statementPeriod'), `${d.beginDate} ~ ${d.endDate}`, false, n('statementPeriod')],
        [n('beginBalance'), money(d.opening), false, n('beginBalance')],
        [n('deliveryAmount'), money(d.deliveryAmount), false, n('deliveryAmount')],
        [n('returnAmount'), money(d.returnAmount), false, n('returnAmount')],
        [n('paymentAmount'), money(d.paymentAmount), false, n('paymentAmount')],
        [n('otherAmount'), money(d.otherAmount), false, n('otherAmount')],
        [n('endBalance'), money(d.closing), false, n('endBalance')],
        [n('status'), status, false, n('status')]
      ],
      columns: [
        { key: 'bizDate', title: n('date') },
        { key: 'type', title: n('type'), value: (r) => rowsRef.value?.typeText(r) },
        { key: 'docNo', title: n('docNo') },
        { key: 'source', title: n('sourceSettle'), value: (r) => rowsRef.value?.sourceText(r) },
        { key: 'increase', title: n('increase'), align: 'right', value: (r) => moneyOrBlank(r.increase) },
        { key: 'decrease', title: n('decrease'), align: 'right', value: (r) => moneyOrBlank(r.decrease) },
        { key: 'balance', title: n('balance'), align: 'right', value: (r) => money(r.balance) },
        { key: 'remark', title: n('remark') }
      ],
      rows: d.rows || [],
      signs: [n('tabulator'), n('finReview'), n('franchiseeSeal')],
      note: d.remark || ''
    },
    d.statementNo,
    'chain.franchiseStatement'
  )
}
</script>

<style scoped>
.w200 {
  width: 200px;
}
.w140 {
  width: 140px;
}
.full {
  width: 100%;
}
.mb12 {
  margin-bottom: 12px;
}
.sub-text {
  color: var(--app-text-3);
}
</style>
