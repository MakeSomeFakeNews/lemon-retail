<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('dist.delivery.deliveryNo')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('dist.common.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="statuses">
          <a-select v-model:value="query.statuses" mode="multiple" :placeholder="$t('common.placeholder.all')" allow-clear :options="statusOptions" />
        </a-form-item>
        <a-form-item :label="receiveOnly ? $t('dist.common.toStore') : $t('dist.common.store')" name="storeId">
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
        <a-button v-if="!receiveOnly" v-perm="`dist:${kind}:add`" type="primary" @click="formRef.openCreate()">
          <template #icon><PlusOutlined /></template>{{ a.isReturn ? $t('dist.delivery.addReturn') : $t('dist.delivery.addDelivery') }}
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
          <template v-if="column.key === 'deliveryNo'">
            <a @click="detailRef.open(record.id)">{{ record.deliveryNo }}</a>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="dist_delivery_status" :value="record.status" />
            <a-tag v-if="record.pendingDiffs" color="red">{{ record.pendingDiffs }}</a-tag>
          </template>
          <template v-else-if="column.key === 'totalQty' || column.key === 'receivedQty'">{{ record[column.key] == null ? '-' : qty(record[column.key]) }}</template>
          <template v-else-if="column.key === 'totalAmount'">{{ record.totalAmount == null ? '-' : money(record.totalAmount) }}</template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <DeliveryForm v-if="!receiveOnly" ref="formRef" :kind="kind" @saved="onSaved" />
    <DeliveryDetail ref="detailRef" :kind="kind" :receive-only="receiveOnly" @edit="(d) => formRef.openEdit(d)" @changed="load" @receive="(d) => receiveRef.open(d.id)" @reject="openReject" />
    <ReceiveModal ref="receiveRef" :kind="kind" @done="afterReceive" />
    <ReasonModal ref="reasonRef" @done="afterReject" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import StoreSelect from '@/components/store/StoreSelect.vue'
import { actionColumn } from '@/components/common/tableActions'
import { pageDelivery } from '@/api/dist'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { money, qty } from '@/utils/number'
import { DistDeliveryStatus } from '@/constants/enums'
import DeliveryDetail from './DeliveryDetail.vue'
import DeliveryForm from './DeliveryForm.vue'
import ReasonModal from './ReasonModal.vue'
import ReceiveModal from './ReceiveModal.vue'
import { useDeliveryActions } from './useDeliveryActions'

/**
 * 配送单 / 门店退仓单列表（kind：delivery | return）。receiveOnly：门店收货页面，只列出配送单（默认待收货），只能收货。
 * 列表按可管辖门店过滤（发货或收货门店）
 */
const props = defineProps({ kind: { type: String, required: true }, receiveOnly: Boolean })
const { t } = useI18n()
const dict = useDict('dist_delivery_status')
const statusOptions = computed(() => toOptions(dict.dist_delivery_status))

const columns = [
  { title: t('dist.delivery.deliveryNo'), key: 'deliveryNo', dataIndex: 'deliveryNo', width: 170 },
  { title: t('dist.common.fromStore'), dataIndex: 'fromStoreName', width: 130, ellipsis: true },
  { title: t('dist.common.fromWarehouse'), dataIndex: 'fromWarehouseName', width: 130, ellipsis: true },
  { title: t('dist.common.toStore'), dataIndex: 'toStoreName', width: 130, ellipsis: true },
  { title: t('dist.common.toWarehouse'), dataIndex: 'toWarehouseName', width: 130, ellipsis: true },
  { title: t('dist.common.bizDate'), dataIndex: 'bizDate', width: 110 },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 130, dict: 'dist_delivery_status' },
  { title: t('dist.common.totalQty'), key: 'totalQty', dataIndex: 'totalQty', width: 100, align: 'right' },
  { title: t('dist.delivery.receivedQty'), key: 'receivedQty', dataIndex: 'receivedQty', width: 100, align: 'right' },
  { title: t('dist.delivery.totalAmount'), key: 'totalAmount', dataIndex: 'totalAmount', width: 110, align: 'right' },
  { title: t('dist.common.receiveDate'), dataIndex: 'receiveDate', width: 110 },
  { title: t('dist.common.transferNo'), dataIndex: 'transferNo', width: 150 },
  { title: t('dist.common.createBy'), dataIndex: 'createByName', width: 100 },
  actionColumn(3, [t('dist.delivery.ship'), t('dist.delivery.receive')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, `dist-${props.kind}${props.receiveOnly ? '-receive' : ''}`)
const showSearch = ref(true)
const defaultStatuses = () => (props.receiveOnly ? [DistDeliveryStatus.SHIPPED] : [])
const query = reactive({ keyword: undefined, statuses: defaultStatuses(), storeId: undefined, range: undefined })
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable((p) => pageDelivery(props.kind, p), query, {
  params: ({ range, storeId, ...q }) => ({ ...q, begin: range?.[0], end: range?.[1], ...(props.receiveOnly ? { toStoreId: storeId } : { storeId }) })
})
onMounted(load)

function resetQuery() {
  Object.assign(query, { keyword: undefined, statuses: defaultStatuses(), storeId: undefined, range: undefined })
  search()
}

const formRef = ref()
const detailRef = ref()
const receiveRef = ref()
const reasonRef = ref()
const a = useDeliveryActions(props.kind, () => load())
const rowActions = (record) => [
  { type: 'edit', show: !props.receiveOnly && a.isDraft(record), perm: a.perm.edit, onClick: () => formRef.value.openEdit(record) },
  { type: 'submit', label: t('dist.delivery.submit'), show: !props.receiveOnly && a.isReturn && a.isDraft(record), perm: a.perm.edit, onClick: () => a.submit(record) },
  { type: 'approve', label: t('dist.delivery.approve'), show: !props.receiveOnly && a.isReturn && record.status === DistDeliveryStatus.SUBMITTED, perm: a.perm.audit, onClick: () => a.approve(record) },
  { type: 'reject', label: t('dist.delivery.reject'), show: !props.receiveOnly && a.isReturn && record.status === DistDeliveryStatus.SUBMITTED, perm: a.perm.audit, onClick: () => openReject(record) },
  { type: 'confirm', label: t('dist.delivery.ship'), show: !props.receiveOnly && a.canShip(record), perm: a.perm.ship, onClick: () => a.ship(record) },
  { type: 'confirm', label: t('dist.delivery.receive'), show: a.canReceive(record), perm: a.perm.receive, onClick: () => receiveRef.value.open(record.id) },
  { type: 'delete', show: !props.receiveOnly && a.isDraft(record), perm: a.perm.remove, confirm: t('dist.delivery.confirmDelete', { no: record.deliveryNo }), onClick: () => a.remove(record) }
]

let rejectingId = null
function openReject(record) {
  rejectingId = record.id
  reasonRef.value.open(t('dist.delivery.rejectTitle', { no: record.deliveryNo }), (reason) => a.reject(record, reason))
}
function refreshDetail(id) {
  if (detailRef.value?.isOpen(id)) detailRef.value.reload()
}
function onSaved(id) {
  load()
  refreshDetail(id)
}
function afterReceive(id) {
  load()
  refreshDetail(id)
}
function afterReject() {
  load()
  refreshDetail(rejectingId)
}
</script>
