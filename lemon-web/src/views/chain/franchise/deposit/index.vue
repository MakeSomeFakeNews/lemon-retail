<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" :exporter="exportAll" @refresh="load">
        <StoreSelect v-model:value="query.storeId" :types="[StoreType.FRANCHISE]" class="w200" @update:value="onStoreChange" />
        <a-select v-model:value="query.kind" :options="kindOptions" :placeholder="$t('chain.common.kind')" allow-clear class="w120" @change="search" />
        <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" @change="search" />
        <a-button v-perm="'chain:deposit:add'" type="primary" @click="openAdd">
          <template #icon><PlusOutlined /></template>{{ $t('chain.deposit.add') }}
        </a-button>
        <span v-if="balance != null" class="balance">{{ $t('chain.deposit.balanceText', { amount: money(balance) }) }}</span>
        <a-tooltip :title="$t('chain.deposit.tip')"><InfoCircleOutlined class="sub-text" /></a-tooltip>
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
          <template v-if="column.key === 'kind'"><DictTag type="chain_deposit_kind" :value="record.kind" /></template>
          <template v-else-if="column.key === 'amount'">{{ money(record.amount) }}</template>
          <template v-else-if="column.key === 'action'">
            <TableActions
              :actions="[
                { type: 'delete', perm: 'chain:deposit:remove', confirm: $t('chain.deposit.confirmDelete', { no: record.depositNo }), onClick: () => remove(record.id) }
              ]"
            />
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="addOpen" :title="$t('chain.deposit.add')" :confirm-loading="saving" @ok="save">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item :label="$t('chain.common.franchiseStore')" name="storeId">
          <StoreSelect v-model:value="form.storeId" :types="[StoreType.FRANCHISE]" :allow-clear="false" />
        </a-form-item>
        <a-form-item :label="$t('chain.common.kind')" name="kind">
          <a-radio-group v-model:value="form.kind">
            <a-radio v-for="o in kindOptions" :key="o.value" :value="o.value">{{ o.label }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('chain.common.date')" name="bizDate"><a-date-picker v-model:value="form.bizDate" value-format="YYYY-MM-DD" class="full" /></a-form-item>
        <a-form-item :label="$t('chain.common.amount')" name="amount"><a-input-number v-model:value="form.amount" :min="0.01" :precision="2" class="full" /></a-form-item>
        <a-form-item :label="$t('chain.common.account')" name="accountId"><AccountSelect v-model:value="form.accountId" :roles="['CASH', 'BANK']" allow-clear /></a-form-item>
        <a-form-item :label="$t('common.field.remark')" name="remark"><a-input v-model:value="form.remark" :maxlength="255" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { InfoCircleOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import StoreSelect from '@/components/store/StoreSelect.vue'
import { addFranchiseDeposit, deleteFranchiseDeposit, franchiseDepositBalance, pageFranchiseDeposit } from '@/api/chain'
import { FranchiseDepositKind, StoreType } from '@/constants/enums'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { money } from '@/utils/number'

/** 加盟保证金：收取、退还登记与余额（台账，不进入加盟应收） */
const { t } = useI18n()
const dict = useDict('chain_deposit_kind')
const kindOptions = computed(() => (dict.chain_deposit_kind || []).map((d) => ({ value: d.value, label: d.label })))
const query = reactive({ storeId: undefined, kind: undefined, range: undefined })
const toParams = ({ range, ...q }) => ({ ...q, begin: range?.[0], end: range?.[1] })
const columns = [
  { title: t('chain.deposit.depositNo'), dataIndex: 'depositNo', width: 160 },
  { title: t('chain.common.franchiseStore'), dataIndex: 'storeName', width: 160 },
  { title: t('chain.common.kind'), key: 'kind', dataIndex: 'kind', width: 90, dict: 'chain_deposit_kind' },
  { title: t('chain.common.date'), dataIndex: 'bizDate', width: 110 },
  { title: t('chain.common.amount'), key: 'amount', dataIndex: 'amount', width: 120, align: 'right' },
  { title: t('chain.common.account'), dataIndex: 'accountName', width: 140 },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true },
  { title: t('chain.deposit.createBy'), dataIndex: 'createByName', width: 100 },
  actionColumn(1)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'chain-franchise-deposit')
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageFranchiseDeposit, query, { params: toParams })
const balance = ref(null)
async function loadBalance() {
  balance.value = query.storeId ? await franchiseDepositBalance(query.storeId) : null
}
function onStoreChange() {
  search()
  loadBalance()
}
onMounted(load)

const addOpen = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({ storeId: undefined, kind: FranchiseDepositKind.RECEIVE, bizDate: undefined, amount: undefined, accountId: null, remark: '' })
const rules = {
  storeId: [{ required: true, message: t('chain.common.selectStore') }],
  bizDate: [{ required: true, message: t('chain.common.selectDate') }],
  amount: [{ required: true, message: t('chain.common.inputAmount') }]
}
function openAdd() {
  Object.assign(form, { storeId: query.storeId, kind: FranchiseDepositKind.RECEIVE, bizDate: dayjs().format('YYYY-MM-DD'), amount: undefined, accountId: null, remark: '' })
  addOpen.value = true
}
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    await addFranchiseDeposit({ ...form })
    message.success(t('common.msg.saveSuccess'))
    addOpen.value = false
    load()
    loadBalance()
  } finally {
    saving.value = false
  }
}
const remove = useAction(deleteFranchiseDeposit, {
  successKey: 'common.msg.deleteSuccess',
  reload: () => {
    load()
    loadBalance()
  }
})
</script>

<style scoped>
.w200 {
  width: 200px;
}
.w120 {
  width: 120px;
}
.full {
  width: 100%;
}
.balance {
  font-weight: 500;
}
.sub-text {
  color: var(--app-text-3);
}
</style>
