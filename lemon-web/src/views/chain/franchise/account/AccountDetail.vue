<template>
  <a-drawer v-model:open="open" :title="$t('chain.account.detailTitle', { name: store.storeName })" width="min(1000px, 100vw)" destroy-on-close>
    <a-space wrap class="mb12">
      <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" @change="load" />
    </a-space>
    <a-row :gutter="12" class="mb12">
      <a-col :xs="12" :sm="6"><a-statistic :title="$t('chain.account.opening')" :value="money(data?.opening)" /></a-col>
      <a-col :xs="12" :sm="6"><a-statistic :title="$t('chain.account.increase')" :value="money(data?.increase)" /></a-col>
      <a-col :xs="12" :sm="6"><a-statistic :title="$t('chain.account.decrease')" :value="money(data?.decrease)" /></a-col>
      <a-col :xs="12" :sm="6"><a-statistic :title="$t('chain.account.closing')" :value="money(data?.closing)" /></a-col>
    </a-row>
    <a-tabs v-model:active-key="tab">
      <a-tab-pane key="statement" :tab="$t('chain.account.statementTab')">
        <StatementRows :rows="data?.rows || []" :loading="loading" />
      </a-tab-pane>
      <a-tab-pane key="ledger" :tab="$t('chain.account.ledgerTab')">
        <a-table row-key="id" size="small" :columns="ledgerColumns" :data-source="ledger" :loading="ledgerLoading" :pagination="false" :scroll="{ x: 800 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'kind'"><DictTag type="chain_ledger_kind" :value="record.kind" /></template>
            <template v-else-if="column.key === 'amount'">{{ money(record.amount) }}</template>
          </template>
        </a-table>
      </a-tab-pane>
    </a-tabs>
  </a-drawer>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import DictTag from '@/components/DictTag.vue'
import { franchisePartnerStatement, pageFranchiseLedger } from '@/api/chain'
import { useDict } from '@/composables/useDict'
import { money } from '@/utils/number'
import StatementRows from '../StatementRows.vue'

/** 加盟商往来明细：财务往来对账单（应收、收款、预收逐笔与余额）与加盟往来（配送、退回生成的应收） */
const { t } = useI18n()
useDict('chain_ledger_kind')
const open = ref(false)
const tab = ref('statement')
const store = ref({})
const range = ref([dayjs().startOf('month').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])
const data = ref()
const loading = ref(false)
const ledger = ref([])
const ledgerLoading = ref(false)

const ledgerColumns = [
  { title: t('chain.common.date'), dataIndex: 'bizDate', width: 110 },
  { title: t('chain.common.kind'), key: 'kind', width: 100 },
  { title: t('chain.ledger.sourceNo'), dataIndex: 'sourceNo', width: 160 },
  { title: t('chain.ledger.transferNo'), dataIndex: 'transferNo', width: 160 },
  { title: t('chain.ledger.billNo'), dataIndex: 'billNo', width: 160 },
  { title: t('chain.common.amount'), key: 'amount', width: 120, align: 'right' }
]

async function load() {
  loading.value = true
  ledgerLoading.value = true
  try {
    const [st, page] = await Promise.all([
      franchisePartnerStatement(store.value.storeId, range.value[0], range.value[1]),
      pageFranchiseLedger({ current: 1, size: 500, storeId: store.value.storeId, begin: range.value[0], end: range.value[1] })
    ])
    data.value = st
    ledger.value = page.records
  } finally {
    loading.value = false
    ledgerLoading.value = false
  }
}

function openDetail(record) {
  store.value = record
  data.value = undefined
  ledger.value = []
  open.value = true
  load()
}

defineExpose({ open: openDetail })
</script>

<style scoped>
.mb12 {
  margin-bottom: 12px;
}
</style>
