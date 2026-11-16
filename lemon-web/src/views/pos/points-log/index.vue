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
          <a-select v-model:value="query.changeType" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.pos_points_type)" />
        </a-form-item>
        <a-form-item :label="$t('pos.ledger.date')" name="range">
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :exporter="exportAll" :columns="columns" @refresh="load">
        <a-button v-perm="'pos:member:points'" type="primary" @click="openAdjust">
          <template #icon><EditOutlined /></template>{{ $t('pos.ledger.adjustPoints') }}
        </a-button>
        <a-button v-perm="'pos:member:points'" danger @click="openClear">
          <template #icon><ClearOutlined /></template>{{ $t('pos.ledger.clearPoints') }}
        </a-button>
      </TableToolbar>
      <a-table row-key="id" :size="size" :columns="visibleColumns" :data-source="list" :loading="loading" :pagination="pagination" :scroll="{ x: scrollX, y: '100%' }" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'member'">{{ record.memberName || record.memberCode }}<span class="sub-text"> {{ record.memberCode }}</span></template>
          <template v-else-if="column.key === 'changeType'"><DictTag type="pos_points_type" :value="record.changeType" /></template>
          <template v-else-if="column.key === 'points'">
            <span :class="Number(record.points) < 0 ? 'neg' : 'pos'">{{ Number(record.points) > 0 ? '+' : '' }}{{ qty(record.points) }}</span>
          </template>
          <template v-else-if="column.key === 'balanceAfter'">{{ qty(record.balanceAfter) }}</template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="adjustOpen" :title="$t('pos.ledger.adjustPoints')" :confirm-loading="saving" @ok="submitAdjust">
      <a-form ref="adjustRef" :model="adjust" :rules="adjustRules" layout="vertical">
        <a-form-item :label="$t('pos.marketing.member')" name="memberId">
          <MemberIdentify ref="identifyRef" v-model:value="adjust.memberId" />
        </a-form-item>
        <a-form-item :label="$t('pos.ledger.adjustPointsValue')" name="points" :extra="$t('pos.ledger.adjustPointsTip')">
          <a-input-number v-model:value="adjust.points" :precision="0" class="full" />
        </a-form-item>
        <a-form-item :label="$t('pos.ledger.reason')" name="reason">
          <a-input v-model:value="adjust.reason" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="clearOpen" :title="$t('pos.ledger.clearPoints')" :ok-text="$t('pos.ledger.clearConfirmOk')" :ok-button-props="{ danger: true }" :confirm-loading="saving" @ok="submitClear">
      <a-alert type="error" show-icon :message="$t('pos.ledger.clearWarning')" class="gap" />
      <a-form ref="clearRef" :model="clear" :rules="clearRules" layout="vertical">
        <a-form-item :label="$t('pos.ledger.clearBefore')" name="before" :extra="$t('pos.ledger.clearBeforeTip')">
          <a-date-picker v-model:value="clear.before" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <a-form-item :label="$t('pos.ledger.reason')" name="reason">
          <a-input v-model:value="clear.reason" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Modal, message } from 'ant-design-vue'
import { ClearOutlined, EditOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import StoreSelect from '@/components/store/StoreSelect.vue'
import { adjustPoints, clearPoints, pagePointsLog } from '@/api/pos'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { qty } from '@/utils/number'
import MemberIdentify from '../MemberIdentify.vue'

/** 积分流水：消费获得、抵现、退货扣回、退回抵现积分、调整、清零；调整与清零是敏感操作（pos:member:points），清零需二次确认 */
const { t } = useI18n()
const dict = useDict('pos_points_type')

const columns = [
  { title: t('pos.ledger.time'), dataIndex: 'createTime', width: 160 },
  { title: t('pos.marketing.member'), key: 'member', width: 180, exportValue: (r) => r.memberCode },
  { title: t('pos.ledger.changeType'), key: 'changeType', dataIndex: 'changeType', width: 110, dict: 'pos_points_type' },
  { title: t('pos.ledger.points'), key: 'points', dataIndex: 'points', width: 110, align: 'right' },
  { title: t('pos.ledger.balanceAfter'), key: 'balanceAfter', dataIndex: 'balanceAfter', width: 110, align: 'right' },
  { title: t('store.common.store'), dataIndex: 'storeName', width: 140, ellipsis: true },
  { title: t('pos.ledger.sourceNo'), dataIndex: 'sourceNo', width: 180 },
  { title: t('pos.ledger.reason'), dataIndex: 'reason', width: 180, ellipsis: true },
  { title: t('pos.ledger.operator'), dataIndex: 'createByName', width: 100 }
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'pos-points-log')
const showSearch = ref(true)
const emptyQuery = () => ({ keyword: undefined, storeId: undefined, changeType: undefined, range: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pagePointsLog, query, {
  params: ({ range, ...q }) => ({ ...q, begin: range?.[0], end: range?.[1] })
})
onMounted(load)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const saving = ref(false)
const adjustOpen = ref(false)
const adjustRef = ref()
const identifyRef = ref()
const adjust = reactive({ memberId: undefined, points: undefined, reason: '' })
const adjustRules = {
  memberId: [{ required: true, message: t('pos.marketing.memberRequired') }],
  points: [{ required: true, message: t('pos.ledger.pointsRequired') }],
  reason: [{ required: true, message: t('pos.ledger.reasonRequired') }]
}
function openAdjust() {
  Object.assign(adjust, { memberId: undefined, points: undefined, reason: '' })
  identifyRef.value?.reset()
  adjustOpen.value = true
}
async function submitAdjust() {
  await adjustRef.value.validate()
  saving.value = true
  try {
    const balance = await adjustPoints({ ...adjust })
    message.success(t('pos.ledger.adjustedPoints', { n: qty(balance) }))
    adjustOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

const clearOpen = ref(false)
const clearRef = ref()
const clear = reactive({ before: undefined, reason: '' })
const clearRules = { reason: [{ required: true, message: t('pos.ledger.reasonRequired') }] }
function openClear() {
  Object.assign(clear, { before: undefined, reason: '' })
  clearOpen.value = true
}
async function submitClear() {
  await clearRef.value.validate()
  // 二次确认：清零不可恢复
  Modal.confirm({
    title: t('pos.ledger.clearConfirmTitle'),
    content: clear.before ? t('pos.ledger.clearConfirmBefore', { date: clear.before }) : t('pos.ledger.clearConfirmAll'),
    okType: 'danger',
    onOk: async () => {
      saving.value = true
      try {
        const r = await clearPoints({ before: clear.before || null, reason: clear.reason })
        message.success(t('pos.ledger.cleared', { members: r.members, points: qty(r.points) }))
        clearOpen.value = false
        load()
      } finally {
        saving.value = false
      }
    }
  })
}
</script>

<style scoped>
.full {
  width: 100%;
}
.gap {
  margin-bottom: 12px;
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
