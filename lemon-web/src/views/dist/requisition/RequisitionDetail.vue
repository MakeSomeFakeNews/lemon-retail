<template>
  <SubPage :open="open" :title="detail ? $t('dist.requisition.detailTitle', { no: detail.reqNo }) : ''" width="min(1100px, 100vw)" @close="open = false">
    <template v-if="detail" #extra>
      <a-space>
        <a-button @click="print">{{ $t('common.action.print') }}</a-button>
        <template v-if="isDraft(detail)">
          <span v-perm="'dist:requisition:remove'">
            <a-popconfirm :title="$t('dist.requisition.confirmDelete', { no: detail.reqNo })" @confirm="removeAndClose">
              <a-button danger>{{ $t('common.action.delete') }}</a-button>
            </a-popconfirm>
          </span>
          <a-button v-perm="'dist:requisition:edit'" @click="emit('edit', detail)">{{ $t('common.action.edit') }}</a-button>
          <a-button v-perm="'dist:requisition:submit'" type="primary" @click="submit(detail)">{{ $t('common.action.submit') }}</a-button>
        </template>
        <a-button v-if="deliverable(detail)" v-perm="'dist:requisition:close'" danger @click="close(detail)">{{ $t('dist.audit.close') }}</a-button>
      </a-space>
    </template>
    <template v-if="detail">
      <a-alert v-if="detail.rejectReason && isDraft(detail)" type="warning" show-icon :message="`${$t('dist.requisition.rejectReason')}：${detail.rejectReason}`" class="block" />
      <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
        <a-descriptions-item :label="$t('dist.common.requestStore')">{{ detail.storeName }}</a-descriptions-item>
        <a-descriptions-item :label="$t('dist.common.receiveWarehouse')">{{ detail.warehouseName }}</a-descriptions-item>
        <a-descriptions-item :label="$t('common.field.status')"><DictTag type="dist_requisition_status" :value="detail.status" /></a-descriptions-item>
        <a-descriptions-item :label="$t('dist.common.supplyStore')">{{ detail.supplyStoreName }}</a-descriptions-item>
        <a-descriptions-item :label="$t('dist.common.supplyWarehouse')">{{ detail.supplyWarehouseName }}</a-descriptions-item>
        <a-descriptions-item :label="$t('dist.common.bizDate')">{{ detail.bizDate }}</a-descriptions-item>
        <a-descriptions-item :label="$t('dist.common.expectedDate')">{{ detail.expectedDate || '-' }}</a-descriptions-item>
        <a-descriptions-item :label="$t('dist.common.createBy')">{{ detail.createByName }} {{ detail.createTime }}</a-descriptions-item>
        <a-descriptions-item :label="$t('dist.requisition.auditBy')">{{ detail.auditByName || '-' }} {{ detail.auditTime || '' }}</a-descriptions-item>
        <a-descriptions-item v-if="detail.closeReason" :label="$t('dist.requisition.closeReason')" :span="3">{{ detail.closeReason }}</a-descriptions-item>
        <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')" :span="3">{{ detail.remark }}</a-descriptions-item>
      </a-descriptions>
      <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="detail.items.length > 50 ? { pageSize: 50 } : false" :scroll="{ x: 1000 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">
            <div>{{ record.materialCode }} {{ record.materialName }}</div>
            <div class="sub-text">{{ record.spec }}</div>
          </template>
          <template v-else-if="column.qty">{{ record[column.key] == null ? '-' : qty(record[column.key]) }}</template>
        </template>
      </a-table>
    </template>
  </SubPage>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import { deleteRequisition, getRequisition } from '@/api/dist'
import { printRequisition } from '@/components/dist/distPrint'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'
import { qty } from '@/utils/number'
import { useRequisitionActions } from './useRequisitionActions'

/** 要货单详情：草稿可修改、提交、删除；已审核、部分配送可关闭；明细显示本店现存、在途和供货仓库可用 */
const emit = defineEmits(['edit', 'changed'])
const { t } = useI18n()
const dict = useDict('dist_requisition_status')

const open = ref(false)
const detail = ref()
const q = (key, title, width = 100) => ({ title, key, width, align: 'right', qty: true })
const itemColumns = computed(() => [
  { title: t('dist.common.material'), key: 'material', width: 260 },
  { title: t('common.field.unit'), dataIndex: 'unit', width: 70 },
  q('qty', t('dist.requisition.qty')),
  q('approvedQty', t('dist.requisition.approvedQty')),
  q('deliveredQty', t('dist.requisition.deliveredQty')),
  q('stockQty', t('dist.requisition.stockQty')),
  q('transitQty', t('dist.requisition.transitQty'), 80),
  q('supplyAvailable', t('dist.requisition.supplyAvailable')),
  q('suggestQty', t('dist.requisition.suggestQty'), 80),
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true }
])

async function show(id) {
  detail.value = await getRequisition(id)
  open.value = true
}
const reload = () => {
  emit('changed')
  return detail.value ? show(detail.value.id) : undefined
}
const { submit, close, isDraft, deliverable } = useRequisitionActions(reload)
async function removeAndClose() {
  await deleteRequisition(detail.value.id)
  message.success(t('common.msg.deleteSuccess'))
  open.value = false
  emit('changed')
}
const print = () => printRequisition({ ...detail.value, statusLabel: findDict(dict.dist_requisition_status, detail.value.status)?.label })

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
