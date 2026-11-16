<template>
  <SubPage :open="open" :title="detail ? $t('dist.delivery.detailTitle', { name: docName, no: detail.deliveryNo }) : ''" width="min(1100px, 100vw)" @close="open = false">
    <template v-if="detail" #extra>
      <a-space>
        <a-button @click="printDelivery(detail, a.isReturn)">{{ $t('common.action.print') }}</a-button>
        <template v-if="!receiveOnly && a.isDraft(detail)">
          <span v-perm="a.perm.remove">
            <a-popconfirm :title="$t('dist.delivery.confirmDelete', { no: detail.deliveryNo })" @confirm="removeAndClose">
              <a-button danger>{{ $t('common.action.delete') }}</a-button>
            </a-popconfirm>
          </span>
          <a-button v-perm="a.perm.edit" @click="emit('edit', detail)">{{ $t('common.action.edit') }}</a-button>
          <a-button v-if="a.isReturn" v-perm="a.perm.edit" type="primary" @click="a.submit(detail)">{{ $t('dist.delivery.submit') }}</a-button>
        </template>
        <template v-if="!receiveOnly && a.isReturn && detail.status === DistDeliveryStatus.SUBMITTED">
          <a-button v-perm="a.perm.audit" danger @click="emit('reject', detail)">{{ $t('dist.delivery.reject') }}</a-button>
          <a-button v-perm="a.perm.audit" type="primary" @click="a.approve(detail)">{{ $t('dist.delivery.approve') }}</a-button>
        </template>
        <a-button v-if="!receiveOnly && a.canShip(detail)" v-perm="a.perm.ship" type="primary" @click="a.ship(detail)">{{ $t('dist.delivery.ship') }}</a-button>
        <a-button v-if="a.canReceive(detail)" v-perm="a.perm.receive" type="primary" @click="emit('receive', detail)">{{ $t('dist.delivery.receive') }}</a-button>
      </a-space>
    </template>
    <template v-if="detail">
      <a-alert v-if="detail.rejectReason && a.isDraft(detail)" type="warning" show-icon :message="`${$t('dist.delivery.rejectReason')}：${detail.rejectReason}`" class="block" />
      <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
        <a-descriptions-item :label="$t('dist.common.fromStore')">{{ detail.fromStoreName }}</a-descriptions-item>
        <a-descriptions-item :label="$t('dist.common.fromWarehouse')">{{ detail.fromWarehouseName }}</a-descriptions-item>
        <a-descriptions-item :label="$t('common.field.status')"><DictTag type="dist_delivery_status" :value="detail.status" /></a-descriptions-item>
        <a-descriptions-item :label="$t('dist.common.toStore')">
          {{ detail.toStoreName }} <DictTag v-if="detail.toStoreType" type="store_type" :value="detail.toStoreType" />
        </a-descriptions-item>
        <a-descriptions-item :label="$t('dist.common.toWarehouse')">{{ detail.toWarehouseName }}</a-descriptions-item>
        <a-descriptions-item :label="$t('dist.common.bizDate')">{{ detail.bizDate }}</a-descriptions-item>
        <a-descriptions-item :label="$t('dist.common.transferNo')">{{ detail.transferNo || '-' }}</a-descriptions-item>
        <a-descriptions-item :label="$t('dist.delivery.totalAmount')">{{ detail.totalAmount == null ? '-' : money(detail.totalAmount) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('dist.common.receiveDate')">{{ detail.receiveDate || '-' }}</a-descriptions-item>
        <a-descriptions-item :label="$t('dist.common.createBy')">{{ detail.createByName }} {{ detail.createTime }}</a-descriptions-item>
        <a-descriptions-item :label="$t('dist.delivery.shipBy')">{{ detail.shipByName || '-' }} {{ detail.shipTime || '' }}</a-descriptions-item>
        <a-descriptions-item :label="$t('dist.delivery.receiveBy')">{{ detail.receiveByName || '-' }} {{ detail.receiveTime || '' }}</a-descriptions-item>
        <a-descriptions-item v-if="a.isReturn" :label="$t('dist.delivery.approveBy')">{{ detail.approveByName || '-' }} {{ detail.approveTime || '' }}</a-descriptions-item>
        <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')" :span="3">{{ detail.remark }}</a-descriptions-item>
      </a-descriptions>
      <a-tabs v-model:active-key="tab">
        <a-tab-pane key="items" :tab="$t('dist.common.items')">
          <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="detail.items.length > 50 ? { pageSize: 50 } : false" :scroll="{ x: 1100 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'material'">
                <div>{{ record.materialCode }} {{ record.materialName }}</div>
                <div class="sub-text">{{ record.spec }}</div>
              </template>
              <template v-else-if="column.key === 'qty' || column.key === 'receivedQty'">{{ record[column.key] == null ? '-' : qty(record[column.key]) }}</template>
              <template v-else-if="column.key === 'price'">{{ record.price == null ? '-' : price(record.price) }}</template>
              <template v-else-if="column.key === 'amount'">{{ record.amount == null ? '-' : money(record.amount) }}</template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane v-if="detail.discrepancies?.length" key="diffs" :tab="`${$t('dist.delivery.discrepancies')} (${detail.discrepancies.length})`">
          <a-table row-key="id" size="small" :columns="diffColumns" :data-source="detail.discrepancies" :pagination="false" :scroll="{ x: 1000 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
              <template v-else-if="column.key === 'kind'"><DictTag type="dist_discrepancy_kind" :value="record.kind" /></template>
              <template v-else-if="column.key === 'status'"><DictTag type="dist_discrepancy_status" :value="record.status" /></template>
              <template v-else-if="column.key === 'handling'"><DictTag v-if="record.handling" type="dist_discrepancy_handling" :value="record.handling" /></template>
              <template v-else-if="column.key === 'qty'">{{ qty(record.qty) }}</template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </template>
  </SubPage>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import { deleteDelivery, getDelivery } from '@/api/dist'
import { useDict } from '@/composables/useDict'
import { money, price, qty } from '@/utils/number'
import { DistDeliveryStatus } from '@/constants/enums'
import { printDelivery } from './distPrint'
import { useDeliveryActions } from './useDeliveryActions'

/** 配送单 / 退仓单详情：明细（配送价、金额、实收）与收货差异；按状态提供删除、修改、提交、审核、发货、收货（驳回、收货由页面处理） */
const props = defineProps({ kind: { type: String, required: true }, receiveOnly: Boolean })
const emit = defineEmits(['edit', 'changed', 'receive', 'reject'])
const { t } = useI18n()
useDict('dist_delivery_status', 'dist_discrepancy_kind', 'dist_discrepancy_status', 'dist_discrepancy_handling', 'store_type')

const open = ref(false)
const detail = ref()
const tab = ref('items')
const docName = computed(() => (props.kind === 'return' ? t('dist.delivery.returnTitle') : t('dist.delivery.deliveryTitle')))
const itemColumns = computed(() => [
  { title: t('dist.common.material'), key: 'material', width: 240 },
  { title: t('common.field.unit'), dataIndex: 'unit', width: 60 },
  { title: t('dist.delivery.location'), dataIndex: 'locationName', width: 120 },
  { title: t('dist.delivery.batch'), dataIndex: 'batchNo', width: 120 },
  { title: t('common.field.quantity'), key: 'qty', width: 90, align: 'right' },
  { title: t('dist.delivery.price'), key: 'price', width: 100, align: 'right' },
  { title: t('dist.delivery.amount'), key: 'amount', width: 110, align: 'right' },
  { title: t('dist.delivery.receivedQty'), key: 'receivedQty', width: 90, align: 'right' },
  { title: t('dist.delivery.requisitionNo'), dataIndex: 'requisitionNo', width: 160 },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true }
])
const diffColumns = computed(() => [
  { title: t('dist.common.material'), key: 'material', width: 220, ellipsis: true },
  { title: t('dist.discrepancy.kind'), key: 'kind', width: 80 },
  { title: t('dist.discrepancy.qty'), key: 'qty', width: 90, align: 'right' },
  { title: t('dist.discrepancy.reason'), dataIndex: 'reason', width: 140, ellipsis: true },
  { title: t('common.field.status'), key: 'status', width: 80 },
  { title: t('dist.discrepancy.handling'), key: 'handling', width: 110 },
  { title: t('dist.discrepancy.transferNo'), dataIndex: 'transferNo', width: 150 },
  { title: t('dist.discrepancy.handleDocs'), dataIndex: 'handleDocNos', width: 200, ellipsis: true },
  { title: t('dist.discrepancy.handleRemark'), dataIndex: 'handleRemark', ellipsis: true }
])

async function show(id) {
  detail.value = await getDelivery(props.kind, id)
  tab.value = 'items'
  open.value = true
}
const reload = () => {
  emit('changed')
  return detail.value ? show(detail.value.id) : undefined
}
const a = useDeliveryActions(props.kind, reload)
async function removeAndClose() {
  await deleteDelivery(props.kind, detail.value.id)
  message.success(t('common.msg.deleteSuccess'))
  open.value = false
  emit('changed')
}

defineExpose({ open: show, reload, isOpen: (id) => open.value && detail.value?.id === id })
</script>

<style scoped>
.block {
  margin-bottom: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
