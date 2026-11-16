<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('dist.requisition.reqNo')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('dist.common.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="statuses">
          <a-select v-model:value="query.statuses" mode="multiple" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.dist_requisition_status)" />
        </a-form-item>
        <a-form-item :label="$t('dist.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" />
        </a-form-item>
        <a-form-item :label="$t('dist.common.dateRange')" name="range">
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" />
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
        <a-button v-perm="'dist:requisition:add'" type="primary" @click="formRef.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('dist.requisition.add') }}
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
          <template v-if="column.key === 'reqNo'">
            <a @click="detailRef.open(record.id)">{{ record.reqNo }}</a>
          </template>
          <template v-else-if="column.key === 'status'"><DictTag type="dist_requisition_status" :value="record.status" /></template>
          <template v-else-if="column.qty">{{ qty(record[column.dataIndex]) }}</template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <RequisitionForm ref="formRef" @saved="onSaved" />
    <RequisitionDetail ref="detailRef" @edit="(d) => formRef.openEdit(d)" @changed="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import StoreSelect from '@/components/store/StoreSelect.vue'
import { actionColumn } from '@/components/common/tableActions'
import { pageRequisition } from '@/api/dist'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { qty } from '@/utils/number'
import RequisitionDetail from './RequisitionDetail.vue'
import RequisitionForm from './RequisitionForm.vue'
import { useRequisitionActions } from './useRequisitionActions'

/**
 * 门店要货单：店长为本店录入要货（显示现存、在途、建议量），提交后（可选审批）由总部 / 配送中心审核、配送；
 * 只列出可管辖门店的要货单（要货门店或供货门店）
 */
const { t } = useI18n()
const dict = useDict('dist_requisition_status')

const columns = [
  { title: t('dist.requisition.reqNo'), key: 'reqNo', dataIndex: 'reqNo', width: 170 },
  { title: t('dist.common.requestStore'), dataIndex: 'storeName', width: 140, ellipsis: true },
  { title: t('dist.common.supplyWarehouse'), dataIndex: 'supplyWarehouseName', width: 140, ellipsis: true },
  { title: t('dist.common.bizDate'), dataIndex: 'bizDate', width: 110 },
  { title: t('dist.common.expectedDate'), dataIndex: 'expectedDate', width: 110 },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 100, dict: 'dist_requisition_status' },
  { title: t('dist.requisition.itemCount'), dataIndex: 'itemCount', width: 80, align: 'right' },
  { title: t('dist.requisition.qty'), dataIndex: 'totalQty', width: 100, align: 'right', qty: true },
  { title: t('dist.requisition.approvedQty'), dataIndex: 'approvedQty', width: 100, align: 'right', qty: true },
  { title: t('dist.requisition.deliveredQty'), dataIndex: 'deliveredQty', width: 100, align: 'right', qty: true },
  { title: t('dist.common.createBy'), dataIndex: 'createByName', width: 100 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  actionColumn(3)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'dist-requisition')
const showSearch = ref(true)
const query = reactive({ keyword: undefined, statuses: [], storeId: undefined, range: undefined })
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageRequisition, query, {
  params: ({ range, ...q }) => ({ ...q, begin: range?.[0], end: range?.[1] })
})
onMounted(load)

function resetQuery() {
  Object.assign(query, { keyword: undefined, statuses: [], storeId: undefined, range: undefined })
  search()
}

const formRef = ref()
const detailRef = ref()
const { submit, remove, isDraft } = useRequisitionActions(() => load())
const rowActions = (record) => [
  { type: 'edit', show: isDraft(record), perm: 'dist:requisition:edit', onClick: () => formRef.value.openEdit(record) },
  { type: 'submit', show: isDraft(record), perm: 'dist:requisition:submit', onClick: () => submit(record) },
  { type: 'delete', show: isDraft(record), perm: 'dist:requisition:remove', confirm: t('dist.requisition.confirmDelete', { no: record.reqNo }), onClick: () => remove(record) }
]

function onSaved(id) {
  load()
  if (detailRef.value?.isOpen(id)) detailRef.value.reload()
}
</script>
