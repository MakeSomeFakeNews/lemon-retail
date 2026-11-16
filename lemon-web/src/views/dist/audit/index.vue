<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab">
        <a-tab-pane key="pending" :tab="$t('dist.audit.pendingTab')" />
        <a-tab-pane key="deliver" :tab="$t('dist.audit.deliverTab')" />
        <a-tab-pane key="summary" :tab="$t('dist.audit.summaryTab')" />
      </a-tabs>
      <template v-if="tab !== 'summary'">
        <div class="toolbar">
          <a-space wrap>
            <a-input-search v-model:value="query.keyword" allow-clear :placeholder="$t('dist.common.keywordPh')" class="w200" @search="search" />
            <StoreSelect v-model:value="query.storeId" class="w200" @update:value="search" />
            <WarehouseSelect v-model:value="query.supplyWarehouseId" all :placeholder="$t('dist.common.supplyWarehouse')" class="w200" @update:value="search" />
          </a-space>
          <a-space v-if="tab === 'deliver'">
            <a-date-picker v-model:value="deliveryDate" value-format="YYYY-MM-DD" :placeholder="$t('dist.audit.deliveryDate')" />
            <a-tooltip :title="$t('dist.audit.toDeliveryTip')">
              <a-button v-perm="'dist:delivery:add'" type="primary" :disabled="!selectedKeys.length" :loading="creating" @click="toDelivery">
                {{ $t('dist.audit.toDelivery') }}
              </a-button>
            </a-tooltip>
          </a-space>
        </div>
        <a-table
          row-key="id"
          size="middle"
          :columns="columns"
          :data-source="list"
          :loading="loading"
          :pagination="pagination"
          :row-selection="tab === 'deliver' ? rowSelection : undefined"
          :scroll="{ x: 1200, y: '100%' }"
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
      </template>
      <SummaryTab v-else />
    </a-card>

    <AuditModal ref="auditRef" @done="load" />
    <ReasonModal ref="reasonRef" @done="load" />
    <RequisitionDetail ref="detailRef" @changed="load" />
  </div>
</template>

<script setup>
import { computed, h, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Modal } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import StoreSelect from '@/components/store/StoreSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import ReasonModal from '@/components/dist/ReasonModal.vue'
import { actionColumn } from '@/components/common/tableActions'
import { closeRequisition, pageRequisition, rejectRequisition, requisitionToDelivery } from '@/api/dist'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { qty } from '@/utils/number'
import { RequisitionStatus } from '@/constants/enums'
import RequisitionDetail from '../requisition/RequisitionDetail.vue'
import AuditModal from './AuditModal.vue'
import SummaryTab from './SummaryTab.vue'

/**
 * 要货审核（总部 / 配送中心）：待审核的要货单逐张审核（调整批准数量）或驳回；
 * 待配送的勾选后生成配送单（按 供货仓库 + 收货仓库 分组），可关闭；要货汇总对照可用库存，缺货生成采购订单
 */
const { t } = useI18n()
const router = useRouter()
useDict('dist_requisition_status')

const tab = ref('pending')
const STATUS_OF_TAB = { pending: [RequisitionStatus.SUBMITTED], deliver: [RequisitionStatus.APPROVED, RequisitionStatus.PARTIAL] }
const query = reactive({ keyword: undefined, storeId: undefined, supplyWarehouseId: undefined })
const { list, loading, pagination, load, search, onTableChange, rowSelection, selectedKeys } = useTable(pageRequisition, query, {
  params: (q) => ({ ...q, statuses: STATUS_OF_TAB[tab.value] })
})
watch(
  tab,
  (v) => {
    if (v === 'summary') return
    selectedKeys.value = []
    search()
  },
  { immediate: true }
)

const columns = computed(() => [
  { title: t('dist.requisition.reqNo'), key: 'reqNo', dataIndex: 'reqNo', width: 170 },
  { title: t('dist.common.requestStore'), dataIndex: 'storeName', width: 140, ellipsis: true },
  { title: t('dist.common.supplyWarehouse'), dataIndex: 'supplyWarehouseName', width: 140, ellipsis: true },
  { title: t('dist.common.bizDate'), dataIndex: 'bizDate', width: 110 },
  { title: t('dist.common.expectedDate'), dataIndex: 'expectedDate', width: 110 },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 100 },
  { title: t('dist.requisition.qty'), dataIndex: 'totalQty', width: 100, align: 'right', qty: true },
  { title: t('dist.requisition.approvedQty'), dataIndex: 'approvedQty', width: 100, align: 'right', qty: true },
  { title: t('dist.requisition.deliveredQty'), dataIndex: 'deliveredQty', width: 100, align: 'right', qty: true },
  { title: t('dist.common.createBy'), dataIndex: 'createByName', width: 100 },
  actionColumn(2, [t('dist.audit.reject'), t('dist.audit.close')])
])

const auditRef = ref()
const reasonRef = ref()
const detailRef = ref()
const rowActions = (record) => [
  { type: 'audit', label: t('dist.audit.audit'), show: record.status === RequisitionStatus.SUBMITTED, perm: 'dist:requisition:audit', onClick: () => auditRef.value.open(record.id) },
  {
    type: 'reject',
    label: t('dist.audit.reject'),
    show: record.status === RequisitionStatus.SUBMITTED,
    perm: 'dist:requisition:audit',
    onClick: () => reasonRef.value.open(t('dist.audit.rejectTitle', { no: record.reqNo }), (reason) => rejectRequisition(record.id, reason))
  },
  {
    type: 'cancel',
    label: t('dist.audit.close'),
    danger: true,
    show: record.status === RequisitionStatus.APPROVED || record.status === RequisitionStatus.PARTIAL,
    perm: 'dist:requisition:close',
    onClick: () => reasonRef.value.open(t('dist.audit.closeTitle', { no: record.reqNo }), (reason) => closeRequisition(record.id, reason))
  }
]

const deliveryDate = ref()
const creating = ref(false)
async function toDelivery() {
  creating.value = true
  try {
    const ids = await requisitionToDelivery(selectedKeys.value, deliveryDate.value || null)
    selectedKeys.value = []
    load()
    Modal.confirm({
      title: t('dist.common.created', { count: ids.length }),
      content: h('div', t('dist.audit.toDeliveryTip')),
      onOk: () => router.push('/dist/delivery')
    })
  } finally {
    creating.value = false
  }
}
</script>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: space-between;
  margin-bottom: 12px;
}
.w200 {
  width: 200px;
}
</style>
