<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('pos.terminal.terminal')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('pos.terminal.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('store.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :exporter="exportAll" :columns="columns" @refresh="load">
        <a-button v-perm="'pos:terminal:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('pos.terminal.add') }}
        </a-button>
        <a-button v-perm="'pos:terminal:edit'" @click="accountsRef.open(query.storeId)">
          <template #icon><WalletOutlined /></template>{{ $t('pos.terminal.payAccounts') }}
        </a-button>
      </TableToolbar>
      <a-table row-key="id" :size="size" :columns="visibleColumns" :data-source="list" :loading="loading" :pagination="pagination" :scroll="{ x: scrollX, y: '100%' }" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div>{{ record.name }}</div>
            <div class="sub-text">{{ record.code }}</div>
          </template>
          <template v-else-if="column.key === 'receiptPaper'"><DictTag type="pos_receipt_paper" :value="record.receiptPaper" /></template>
          <template v-else-if="column.key === 'shift'">
            <span v-if="record.openShiftNo">{{ record.openShiftNo }} · {{ record.openCashierName }}</span>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'status'"><DictTag type="sys_common_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'"><TableActions :actions="rowActions(record)" /></template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('pos.terminal.add')" :edit-title="$t('pos.terminal.edit')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('pos.terminal.code')" name="code">
              <a-input v-model:value="form.code" :maxlength="32" :placeholder="$t('pos.terminal.codePh')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('pos.terminal.name')" name="name">
              <a-input v-model:value="form.name" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('store.common.store')" name="storeId">
              <StoreSelect v-model:value="form.storeId" trading :allow-clear="false" @update:value="onStore" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('pos.terminal.warehouse')" name="warehouseId" :extra="$t('pos.terminal.warehouseTip')">
              <WarehouseSelect v-model:value="form.warehouseId" :store-id="form.storeId" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('pos.terminal.cashAccount')" name="cashAccountId" :extra="$t('pos.terminal.cashAccountTip')">
              <AccountSelect v-model:value="form.cashAccountId" :roles="['CASH', 'BANK']" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('pos.terminal.receiptPaper')" name="receiptPaper">
              <a-radio-group v-model:value="form.receiptPaper" :options="toOptions(dict.pos_receipt_paper)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.status')" name="status">
              <a-radio-group v-model:value="form.status" :options="toOptions(dict.sys_common_status, true)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.sort')" name="sort">
              <a-input-number v-model:value="form.sort" :min="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')" name="remark">
              <a-input v-model:value="form.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>

    <PayAccountsModal ref="accountsRef" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined, WalletOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import StoreSelect from '@/components/store/StoreSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { addTerminal, deleteTerminal, getTerminal, pageTerminal, updateTerminal } from '@/api/pos'
import { storeOptions } from '@/api/store/store'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { STATUS_ENABLED } from '@/utils/constants'
import { ReceiptPaper } from '@/constants/enums'
import PayAccountsModal from './PayAccountsModal.vue'

/** 收银台：每台属于一个门店（出库仓库、现金账户为本门店的），收银员在收银台开班后收银；门店收款账户设置其他支付方式记入的结算账户 */
const { t } = useI18n()
const dict = useDict('pos_receipt_paper', 'sys_common_status')

const columns = [
  { title: t('pos.terminal.terminal'), key: 'name', width: 180, exportColumns: [
    { title: t('pos.terminal.code'), dataIndex: 'code' },
    { title: t('pos.terminal.name'), dataIndex: 'name' }
  ] },
  { title: t('store.common.store'), dataIndex: 'storeName', width: 160, ellipsis: true },
  { title: t('pos.terminal.warehouse'), dataIndex: 'warehouseName', width: 150, ellipsis: true },
  { title: t('pos.terminal.cashAccount'), dataIndex: 'cashAccountName', width: 150, ellipsis: true },
  { title: t('pos.terminal.receiptPaper'), key: 'receiptPaper', dataIndex: 'receiptPaper', width: 90, dict: 'pos_receipt_paper' },
  { title: t('pos.terminal.openShift'), key: 'shift', width: 200, exportValue: (r) => r.openShiftNo || '' },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 80, dict: 'sys_common_status' },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  actionColumn(2)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'pos-terminal')
const showSearch = ref(true)
const emptyQuery = () => ({ keyword: undefined, storeId: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageTerminal, query)
onMounted(load)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const rules = {
  name: [{ required: true, message: t('pos.terminal.nameRequired') }],
  storeId: [{ required: true, message: t('store.common.selectStore') }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, code: '', name: '', storeId: undefined, warehouseId: undefined, cashAccountId: undefined,
    receiptPaper: ReceiptPaper.MM80, status: STATUS_ENABLED, sort: 0, remark: '' }),
  fetch: (r) => getTerminal(r.id),
  create: addTerminal,
  update: updateTerminal,
  toPayload: (f) => ({ code: f.code || null, name: f.name, storeId: f.storeId, warehouseId: f.warehouseId || null, cashAccountId: f.cashAccountId || null,
    receiptPaper: f.receiptPaper, status: f.status, sort: f.sort, remark: f.remark || null }),
  onSuccess: () => load()
})
const { formRef, form } = drawer

/** 选择门店后带出门店的默认仓库、默认结算账户 */
async function onStore(storeId) {
  const store = (await storeOptions({}).catch(() => [])).find((s) => s.id === storeId)
  if (store) {
    form.value.warehouseId = store.defaultWarehouseId || undefined
    form.value.cashAccountId = store.defaultAccountId || form.value.cashAccountId
  }
}

const accountsRef = ref()
const remove = useAction(deleteTerminal, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
const rowActions = (record) => [
  { type: 'edit', perm: 'pos:terminal:edit', onClick: () => drawer.openEdit(record) },
  { type: 'delete', perm: 'pos:terminal:remove', confirm: t('pos.terminal.confirmDelete', { name: record.name }), onClick: () => remove(record.id) }
]
</script>

<style scoped>
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
