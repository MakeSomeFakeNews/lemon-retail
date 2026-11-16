<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('scm.priceAdjust.adjustNo')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('scm.priceAdjust.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.scm_price_adjust_status)" />
        </a-form-item>
        <a-form-item :label="$t('scm.priceAdjust.target')" name="target">
          <a-select v-model:value="query.target" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.scm_price_adjust_target)" />
        </a-form-item>
        <a-form-item :label="$t('scm.priceAdjust.scheme')" name="schemeId">
          <a-select v-model:value="query.schemeId" :placeholder="$t('common.placeholder.all')" allow-clear show-search option-filter-prop="label" :options="schemeOptions" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :exporter="exportAll"
        :columns="columns"
        @refresh="load"
      >
        <a-button v-perm="'scm:priceAdjust:add'" type="primary" @click="formRef.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('scm.priceAdjust.add') }}
        </a-button>
        <a-button @click="historyRef.open()">
          <template #icon><HistoryOutlined /></template>{{ $t('scm.priceAdjust.history') }}
        </a-button>
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
          <template v-if="column.key === 'adjustNo'">
            <a @click="detailRef.open(record.id)">{{ record.adjustNo }}</a>
          </template>
          <template v-else-if="column.key === 'target'"><DictTag type="scm_price_adjust_target" :value="record.target" /></template>
          <template v-else-if="column.key === 'schemes'">{{ schemeText(record) }}</template>
          <template v-else-if="column.key === 'effectiveTime'">{{ record.effectiveTime || $t('scm.priceAdjust.immediate') }}</template>
          <template v-else-if="column.key === 'status'"><DictTag type="scm_price_adjust_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <AdjustForm ref="formRef" @saved="onSaved" />
    <AdjustDetail ref="detailRef" @edit="(d) => formRef.openEdit(d)" @changed="load" />
    <PriceHistory ref="historyRef" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { HistoryOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { pagePriceAdjust } from '@/api/scm/priceAdjust'
import { priceSchemeOptions } from '@/api/scm/priceScheme'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { PriceAdjustStatus, PriceAdjustTarget } from '@/constants/enums'
import AdjustDetail from './AdjustDetail.vue'
import AdjustForm from './AdjustForm.vue'
import PriceHistory from './PriceHistory.vue'
import { useAdjustActions } from './useAdjustActions'

/**
 * 调价单：调整总部零售价或门店价格方案，生效时间为立即（审核后）或定时（到时由定时任务生效）；
 * 生效时记录每个商品调整前后的价格，可按商品查看调价历史
 */
const { t } = useI18n()
const dict = useDict('scm_price_adjust_status', 'scm_price_adjust_target')

const schemes = ref([])
const schemeOptions = computed(() => schemes.value.map((s) => ({ value: s.id, label: `${s.code} ${s.name}` })))
const schemeText = (r) => (r.target === PriceAdjustTarget.SCHEME ? (r.schemeNames || []).join(', ') : t('scm.priceAdjust.hqPrice'))

const columns = [
  { title: t('scm.priceAdjust.adjustNo'), key: 'adjustNo', dataIndex: 'adjustNo', width: 160 },
  { title: t('scm.priceAdjust.target'), key: 'target', dataIndex: 'target', width: 100, dict: 'scm_price_adjust_target' },
  { title: t('scm.priceAdjust.scheme'), key: 'schemes', width: 200, ellipsis: true, exportValue: (r) => schemeText(r) },
  { title: t('scm.priceAdjust.itemCount'), dataIndex: 'itemCount', width: 90, align: 'right' },
  { title: t('scm.priceAdjust.effectiveTime'), key: 'effectiveTime', dataIndex: 'effectiveTime', width: 170 },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 100, dict: 'scm_price_adjust_status' },
  { title: t('scm.priceAdjust.effectTime'), dataIndex: 'effectTime', width: 170 },
  { title: t('scm.priceAdjust.reason'), dataIndex: 'reason', width: 200, ellipsis: true },
  { title: t('scm.priceAdjust.createBy'), dataIndex: 'createByName', width: 100 },
  { title: t('common.field.createTime'), dataIndex: 'createTime', width: 170 },
  // 草稿：编辑、提交、删除；审批中：撤回；待生效：作废
  actionColumn(3, [t('scm.priceAdjust.withdraw'), t('scm.priceAdjust.void')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'scm-price-adjust')
const showSearch = ref(true)
const query = reactive({ keyword: undefined, status: undefined, target: undefined, schemeId: undefined })
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pagePriceAdjust, query)
onMounted(async () => {
  load()
  schemes.value = await priceSchemeOptions().catch(() => [])
})

function resetQuery() {
  Object.assign(query, { keyword: undefined, status: undefined, target: undefined, schemeId: undefined })
  search()
}

const formRef = ref()
const detailRef = ref()
const historyRef = ref()
const { submit, withdraw, voidAdjust, remove, isDraft } = useAdjustActions(() => load())
const rowActions = (record) => [
  { type: 'edit', show: isDraft(record), perm: 'scm:priceAdjust:edit', onClick: () => formRef.value.openEdit(record) },
  { type: 'submit', show: isDraft(record), perm: 'scm:priceAdjust:audit', onClick: () => submit(record) },
  {
    type: 'unaudit',
    label: t('scm.priceAdjust.withdraw'),
    show: record.status === PriceAdjustStatus.APPROVING,
    perm: 'scm:priceAdjust:audit',
    onClick: () => withdraw(record)
  },
  {
    type: 'cancel',
    label: t('scm.priceAdjust.void'),
    danger: true,
    show: record.status === PriceAdjustStatus.APPROVED,
    perm: 'scm:priceAdjust:audit',
    onClick: () => voidAdjust(record)
  },
  { type: 'delete', show: isDraft(record), perm: 'scm:priceAdjust:remove', confirm: t('scm.priceAdjust.confirmDelete', { no: record.adjustNo }), onClick: () => remove(record) }
]

function onSaved(id) {
  load()
  if (detailRef.value?.isOpen(id)) detailRef.value.reload()
}
</script>
