<template>
  <a-modal v-model:open="open" :title="detail ? $t('dist.audit.auditTitle', { no: detail.reqNo }) : ''" width="min(960px, 100vw)" :confirm-loading="saving" @ok="onOk">
    <template v-if="detail">
      <a-alert type="info" show-icon :message="$t('dist.audit.auditTip')" class="block" />
      <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" class="block">
        <a-descriptions-item :label="$t('dist.common.requestStore')">{{ detail.storeName }}</a-descriptions-item>
        <a-descriptions-item :label="$t('dist.common.supplyWarehouse')">{{ detail.supplyWarehouseName }}</a-descriptions-item>
        <a-descriptions-item :label="$t('dist.common.expectedDate')">{{ detail.expectedDate || '-' }}</a-descriptions-item>
      </a-descriptions>
      <a-table row-key="id" size="small" :columns="columns" :data-source="detail.items" :pagination="false" :scroll="{ x: 820, y: 400 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
          <template v-else-if="column.key === 'approvedQty'">
            <a-input-number v-model:value="record.approve" :min="0" :precision="4" class="full" />
          </template>
          <template v-else-if="column.qty">{{ record[column.key] == null ? '-' : qty(record[column.key]) }}</template>
        </template>
      </a-table>
    </template>
  </a-modal>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { auditRequisition, getRequisition } from '@/api/dist'
import { qty } from '@/utils/number'

/** 审核要货单：逐行填写批准数量（默认等于要货数量，对照本店现存、供货仓库可用） */
const emit = defineEmits(['done'])
const { t } = useI18n()
const open = ref(false)
const saving = ref(false)
const detail = ref()
const q = (key, title) => ({ title, key, width: 100, align: 'right', qty: true })
const columns = computed(() => [
  { title: t('dist.common.material'), key: 'material', width: 240, ellipsis: true },
  { title: t('common.field.unit'), dataIndex: 'unit', width: 60 },
  q('qty', t('dist.requisition.qty')),
  q('stockQty', t('dist.requisition.stockQty')),
  q('supplyAvailable', t('dist.requisition.supplyAvailable')),
  { title: t('dist.requisition.approvedQty'), key: 'approvedQty', width: 140 },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true }
])

async function show(id) {
  const d = await getRequisition(id)
  d.items.forEach((i) => (i.approve = Number(i.qty)))
  detail.value = d
  open.value = true
}

async function onOk() {
  saving.value = true
  try {
    await auditRequisition(detail.value.id, detail.value.items.map((i) => ({ itemId: i.id, approvedQty: i.approve ?? 0 })))
    message.success(t('common.msg.operationSuccess'))
    open.value = false
    emit('done')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.block {
  margin-bottom: 12px;
}
.full {
  width: 100%;
}
</style>
