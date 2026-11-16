<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('pos.ledger.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('pos.ledger.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('store.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('pos.ledger.changeType')" name="changeType">
          <a-select v-model:value="query.changeType" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.pos_stored_value_type)" />
        </a-form-item>
        <a-form-item :label="$t('pos.ledger.businessDate')" name="range">
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :exporter="exportAll" :columns="columns" @refresh="load">
        <a-button v-perm="'pos:member:storedValue'" type="primary" @click="openAdjust">
          <template #icon><EditOutlined /></template>{{ $t('pos.ledger.adjustStoredValue') }}
        </a-button>
      </TableToolbar>
      <a-table row-key="id" :size="size" :columns="visibleColumns" :data-source="list" :loading="loading" :pagination="pagination" :scroll="{ x: scrollX, y: '100%' }" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'member'">{{ record.memberName || record.memberCode }}<span class="sub-text"> {{ record.memberCode }}</span></template>
          <template v-else-if="column.key === 'changeType'"><DictTag type="pos_stored_value_type" :value="record.changeType" /></template>
          <template v-else-if="column.key === 'amount'">
            <span :class="Number(record.amount) < 0 ? 'neg' : 'pos'">{{ Number(record.amount) > 0 ? '+' : '' }}{{ money(record.amount) }}</span>
          </template>
          <template v-else-if="column.key === 'balanceAfter'">{{ money(record.balanceAfter) }}</template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="adjustOpen" :title="$t('pos.ledger.adjustStoredValue')" :confirm-loading="saving" @ok="submitAdjust">
      <a-form ref="adjustRef" :model="adjust" :rules="adjustRules" layout="vertical">
        <a-form-item :label="$t('pos.marketing.member')" name="memberId">
          <MemberIdentify ref="identifyRef" v-model:value="adjust.memberId" />
        </a-form-item>
        <a-form-item :label="$t('pos.ledger.adjustAmount')" name="amount" :extra="$t('pos.ledger.adjustAmountTip')">
          <a-input-number v-model:value="adjust.amount" :precision="2" class="full" />
        </a-form-item>
        <a-form-item :label="$t('store.common.store')" name="storeId" :extra="$t('pos.ledger.adjustStoreTip')">
          <StoreSelect v-model:value="adjust.storeId" />
        </a-form-item>
        <a-form-item :label="$t('pos.ledger.reason')" name="reason">
          <a-input v-model:value="adjust.reason" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { EditOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import StoreSelect from '@/components/store/StoreSelect.vue'
import { adjustStoredValue, pageStoredValueLog } from '@/api/pos'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { money } from '@/utils/number'
import MemberIdentify from '../MemberIdentify.vue'

/**
 * 储值流水：余额全局一份，每笔记录门店（充值为收款门店、消费 / 退回为消费门店），按营业日期筛选；
 * 跨店结算报表穿透时带 ?storeId=&changeType=&begin=&end=。储值调整是敏感操作（pos:member:storedValue）
 */
const { t } = useI18n()
const route = useRoute()
const dict = useDict('pos_stored_value_type')

const columns = [
  { title: t('pos.ledger.time'), dataIndex: 'createTime', width: 160 },
  { title: t('pos.ledger.businessDate'), dataIndex: 'businessDate', width: 110 },
  { title: t('pos.marketing.member'), key: 'member', width: 180, exportValue: (r) => r.memberCode },
  { title: t('pos.ledger.changeType'), key: 'changeType', dataIndex: 'changeType', width: 100, dict: 'pos_stored_value_type' },
  { title: t('pos.ledger.amount'), key: 'amount', dataIndex: 'amount', width: 110, align: 'right' },
  { title: t('pos.ledger.balanceAfter'), key: 'balanceAfter', dataIndex: 'balanceAfter', width: 110, align: 'right' },
  { title: t('store.common.store'), dataIndex: 'storeName', width: 140, ellipsis: true },
  { title: t('pos.ledger.sourceNo'), dataIndex: 'sourceNo', width: 180 },
  { title: t('pos.ledger.remark'), dataIndex: 'remark', width: 180, ellipsis: true },
  { title: t('pos.ledger.operator'), dataIndex: 'createByName', width: 100 }
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'pos-stored-value-log')
const showSearch = ref(true)
const emptyQuery = () => ({ keyword: undefined, storeId: undefined, changeType: undefined, range: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageStoredValueLog, query, {
  params: ({ range, ...q }) => ({ ...q, begin: range?.[0], end: range?.[1] })
})

/** 穿透：地址参数作为查询条件 */
function applyRoute() {
  const q = route.query
  if (!q.storeId && !q.changeType && !q.begin) return false
  Object.assign(query, emptyQuery(), {
    storeId: q.storeId ? Number(q.storeId) : undefined,
    changeType: q.changeType || undefined,
    range: q.begin && q.end ? [String(q.begin), String(q.end)] : undefined
  })
  return true
}
applyRoute()
onMounted(load)
watch(
  () => route.query,
  () => applyRoute() && search()
)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const saving = ref(false)
const adjustOpen = ref(false)
const adjustRef = ref()
const identifyRef = ref()
const adjust = reactive({ memberId: undefined, amount: undefined, storeId: undefined, reason: '' })
const adjustRules = {
  memberId: [{ required: true, message: t('pos.marketing.memberRequired') }],
  amount: [{ required: true, message: t('pos.ledger.amountRequired') }],
  reason: [{ required: true, message: t('pos.ledger.reasonRequired') }]
}
function openAdjust() {
  Object.assign(adjust, { memberId: undefined, amount: undefined, storeId: undefined, reason: '' })
  identifyRef.value?.reset()
  adjustOpen.value = true
}
async function submitAdjust() {
  await adjustRef.value.validate()
  saving.value = true
  try {
    const balance = await adjustStoredValue({ ...adjust, storeId: adjust.storeId || null })
    message.success(t('pos.ledger.adjustedBalance', { n: money(balance) }))
    adjustOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.full {
  width: 100%;
}
.pos {
  color: #389e0d;
}
.neg {
  color: #cf1322;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
