<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" :exporter="exportAll" @refresh="load">
        <a-input-search v-model:value="query.keyword" :placeholder="$t('chain.account.keywordPh')" allow-clear class="w200" @search="search" />
        <a-select v-model:value="query.settleMode" :options="settleModes" :placeholder="$t('chain.common.settleMode')" allow-clear class="w160" @change="search" />
        <a-tooltip :title="$t('chain.account.tip')"><InfoCircleOutlined class="sub-text" /></a-tooltip>
      </TableToolbar>
      <a-table
        row-key="storeId"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: scrollX, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'store'">
            <a @click="detailRef.open(record)">{{ record.storeName }}</a>
            <div class="sub-text">{{ record.storeCode }}</div>
          </template>
          <template v-else-if="column.key === 'settleMode'"><DictTag type="store_settle_mode" :value="record.settleMode" /></template>
          <template v-else-if="column.key === 'available'">
            <span :class="{ danger: record.available != null && record.available < 0 }">{{ record.available == null ? $t('chain.account.unlimited') : money(record.available) }}</span>
          </template>
          <template v-else-if="column.money">{{ money(record[column.dataIndex]) }}</template>
          <template v-else-if="column.key === 'action'">
            <TableActions
              :actions="[
                { type: 'detail', onClick: () => detailRef.open(record) },
                { type: 'add', label: $t('chain.account.topup'), perm: 'chain:franchise:topup', onClick: () => modalRef.openTopup(record) },
                { type: 'setting', label: $t('chain.account.settings'), perm: 'chain:franchise:edit', onClick: () => modalRef.openSettings(record) }
              ]"
            />
          </template>
        </template>
      </a-table>
    </a-card>
    <AccountModals ref="modalRef" @saved="load" />
    <AccountDetail ref="detailRef" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { InfoCircleOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { pageFranchiseAccount } from '@/api/chain'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { money } from '@/utils/number'
import AccountModals from './AccountModals.vue'
import AccountDetail from './AccountDetail.vue'

/** 加盟商账户：每个加盟店的应收、预收、余额与可用额度、保证金；充值（预付货款）、信用额度设置、往来明细 */
const { t } = useI18n()
const dict = useDict('store_settle_mode')
const settleModes = computed(() => (dict.store_settle_mode || []).map((d) => ({ value: d.value, label: d.label })))
const query = reactive({ keyword: undefined, settleMode: undefined })
const columns = [
  { title: t('chain.common.franchiseStore'), key: 'store', dataIndex: 'storeName', width: 180, fixed: 'left' },
  { title: t('chain.common.franchisee'), dataIndex: 'franchisee', width: 140 },
  { title: t('chain.common.settleMode'), key: 'settleMode', dataIndex: 'settleMode', width: 100, dict: 'store_settle_mode' },
  { title: t('chain.account.customer'), dataIndex: 'customerName', width: 160 },
  { title: t('chain.account.receivable'), dataIndex: 'receivable', width: 120, align: 'right', money: true },
  { title: t('chain.account.prepaid'), dataIndex: 'prepaid', width: 120, align: 'right', money: true },
  { title: t('chain.account.balance'), dataIndex: 'balance', width: 120, align: 'right', money: true },
  { title: t('chain.account.creditLimit'), dataIndex: 'creditLimit', width: 120, align: 'right', money: true },
  { title: t('chain.account.available'), key: 'available', dataIndex: 'available', width: 120, align: 'right' },
  { title: t('chain.account.depositBalance'), dataIndex: 'depositBalance', width: 120, align: 'right', money: true },
  { title: t('chain.account.contract'), dataIndex: 'contractEnd', width: 110 },
  actionColumn(3)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'chain-franchise-account')
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageFranchiseAccount, query)
const modalRef = ref()
const detailRef = ref()
onMounted(load)
</script>

<style scoped>
.w200 {
  width: 200px;
}
.w160 {
  width: 160px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.danger {
  color: var(--ant-color-error, #ff4d4f);
}
</style>
