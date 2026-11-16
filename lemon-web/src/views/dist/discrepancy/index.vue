<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('dist.delivery.deliveryNo')" name="keyword">
          <a-input v-model:value="query.keyword" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('dist.discrepancy.deliveryKind')" name="kind">
          <a-select v-model:value="query.kind" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.dist_delivery_kind)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="statuses">
          <a-select v-model:value="query.statuses" mode="multiple" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.dist_discrepancy_status)" />
        </a-form-item>
        <a-form-item :label="$t('dist.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" />
        </a-form-item>
        <a-form-item :label="$t('dist.common.receiveDate')" name="range">
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :exporter="exportAll" :columns="columns" @refresh="load" />
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
          <template v-if="column.key === 'material'">
            <div>{{ record.materialCode }} {{ record.materialName }}</div>
            <div class="sub-text">{{ record.batchNo }}</div>
          </template>
          <template v-else-if="column.key === 'deliveryKind'"><DictTag type="dist_delivery_kind" :value="record.deliveryKind" /></template>
          <template v-else-if="column.key === 'kind'"><DictTag type="dist_discrepancy_kind" :value="record.kind" /></template>
          <template v-else-if="column.key === 'status'"><DictTag type="dist_discrepancy_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'handling'"><DictTag v-if="record.handling" type="dist_discrepancy_handling" :value="record.handling" /></template>
          <template v-else-if="column.key === 'qty'">{{ qty(record.qty) }}</template>
          <template v-else-if="column.key === 'amount'">{{ record.amount == null ? '-' : money(record.amount) }}</template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="handleOpen" :title="$t('dist.discrepancy.handleTitle')" :confirm-loading="saving" @ok="submitHandle">
      <template v-if="current">
        <a-descriptions :column="1" size="small" class="block">
          <a-descriptions-item :label="$t('dist.delivery.deliveryNo')">{{ current.deliveryNo }}</a-descriptions-item>
          <a-descriptions-item :label="$t('dist.common.material')">{{ current.materialCode }} {{ current.materialName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('dist.discrepancy.kind')">
            <DictTag type="dist_discrepancy_kind" :value="current.kind" /> {{ qty(current.qty) }} {{ current.unit }}
          </a-descriptions-item>
          <a-descriptions-item v-if="current.reason" :label="$t('dist.discrepancy.reason')">{{ current.reason }}</a-descriptions-item>
        </a-descriptions>
        <a-form layout="vertical">
          <a-form-item :label="$t('dist.discrepancy.handling')" required>
            <a-radio-group v-model:value="handleForm.handling">
              <a-radio v-for="h in handlingOptions" :key="h.value" :value="h.value" class="radio-line">
                {{ h.label }}<div class="sub-text">{{ $t(`dist.discrepancy.tips.${h.value}`) }}</div>
              </a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item v-if="isWriteOff" :label="$t('dist.discrepancy.ioCategory')">
            <a-select v-model:value="handleForm.ioCategory" allow-clear :placeholder="$t('dist.discrepancy.ioCategoryPh')" :options="toOptions(dict.wms_out_category)" />
          </a-form-item>
          <a-form-item :label="$t('dist.discrepancy.handleRemark')">
            <a-input v-model:value="handleForm.remark" :maxlength="255" />
          </a-form-item>
        </a-form>
      </template>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import StoreSelect from '@/components/store/StoreSelect.vue'
import { actionColumn } from '@/components/common/tableActions'
import { handleDiscrepancy, pageDiscrepancy } from '@/api/dist'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { findDict, toOptions } from '@/utils/dict'
import { money, qty } from '@/utils/number'
import { DiscrepancyHandling, DiscrepancyKind, DiscrepancyStatus } from '@/constants/enums'

/**
 * 收货差异：配送 / 退仓收货时实收与发货不一致的明细。总部判定责任方后处理：
 * 少收 / 破损可退回发货方、发货方承担、收货方承担（报损），多收可收货方留用或退回发货方
 */
const { t } = useI18n()
const dict = useDict('dist_delivery_kind', 'dist_discrepancy_kind', 'dist_discrepancy_status', 'dist_discrepancy_handling', 'wms_out_category')

const columns = [
  { title: t('dist.delivery.deliveryNo'), dataIndex: 'deliveryNo', width: 170 },
  { title: t('dist.discrepancy.deliveryKind'), key: 'deliveryKind', dataIndex: 'deliveryKind', width: 90, dict: 'dist_delivery_kind' },
  { title: t('dist.common.fromStore'), dataIndex: 'fromStoreName', width: 130, ellipsis: true },
  { title: t('dist.common.toStore'), dataIndex: 'toStoreName', width: 130, ellipsis: true },
  { title: t('dist.common.receiveDate'), dataIndex: 'receiveDate', width: 110 },
  { title: t('dist.common.material'), key: 'material', width: 220 },
  { title: t('dist.discrepancy.kind'), key: 'kind', dataIndex: 'kind', width: 80, dict: 'dist_discrepancy_kind' },
  { title: t('dist.discrepancy.qty'), key: 'qty', dataIndex: 'qty', width: 90, align: 'right' },
  { title: t('dist.discrepancy.amount'), key: 'amount', dataIndex: 'amount', width: 100, align: 'right' },
  { title: t('dist.discrepancy.reason'), dataIndex: 'reason', width: 140, ellipsis: true },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 80, dict: 'dist_discrepancy_status' },
  { title: t('dist.discrepancy.handling'), key: 'handling', dataIndex: 'handling', width: 110, dict: 'dist_discrepancy_handling' },
  { title: t('dist.discrepancy.handleDocs'), dataIndex: 'handleDocNos', width: 200, ellipsis: true },
  { title: t('dist.discrepancy.handleBy'), dataIndex: 'handleByName', width: 90 },
  { title: t('dist.discrepancy.handleTime'), dataIndex: 'handleTime', width: 160 },
  actionColumn(1)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'dist-discrepancy')
const showSearch = ref(true)
const query = reactive({ keyword: undefined, kind: undefined, statuses: [DiscrepancyStatus.PENDING], storeId: undefined, range: undefined })
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageDiscrepancy, query, {
  params: ({ range, ...q }) => ({ ...q, begin: range?.[0], end: range?.[1] })
})
onMounted(load)

function resetQuery() {
  Object.assign(query, { keyword: undefined, kind: undefined, statuses: [DiscrepancyStatus.PENDING], storeId: undefined, range: undefined })
  search()
}

const rowActions = (record) => [
  { type: 'confirm', label: t('dist.discrepancy.handle'), show: record.status === DiscrepancyStatus.PENDING, perm: 'dist:discrepancy:handle', onClick: () => openHandle(record) }
]

const handleOpen = ref(false)
const saving = ref(false)
const current = ref()
const handleForm = reactive({ handling: undefined, ioCategory: undefined, remark: '' })
const SHORT_OPTIONS = [DiscrepancyHandling.RETURN_SOURCE, DiscrepancyHandling.SOURCE_WRITE_OFF, DiscrepancyHandling.TARGET_WRITE_OFF]
const OVER_OPTIONS = [DiscrepancyHandling.ACCEPT, DiscrepancyHandling.RETURN_SOURCE]
const handlingOptions = computed(() =>
  (current.value?.kind === DiscrepancyKind.OVER ? OVER_OPTIONS : SHORT_OPTIONS).map((v) => ({
    value: v,
    label: findDict(dict.dist_discrepancy_handling, v)?.label ?? v
  }))
)
const isWriteOff = computed(() => handleForm.handling === DiscrepancyHandling.SOURCE_WRITE_OFF || handleForm.handling === DiscrepancyHandling.TARGET_WRITE_OFF)

function openHandle(record) {
  current.value = record
  Object.assign(handleForm, { handling: handlingOptions.value[0]?.value, ioCategory: undefined, remark: '' })
  handleOpen.value = true
}

async function submitHandle() {
  saving.value = true
  try {
    await handleDiscrepancy(current.value.id, { handling: handleForm.handling, ioCategory: isWriteOff.value ? handleForm.ioCategory || null : null, remark: handleForm.remark || null })
    message.success(t('dist.discrepancy.handled'))
    handleOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.block {
  margin-bottom: 12px;
}
.radio-line {
  display: flex;
  margin-bottom: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
