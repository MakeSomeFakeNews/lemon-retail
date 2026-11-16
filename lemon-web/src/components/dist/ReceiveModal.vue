<template>
  <a-modal v-model:open="open" :title="detail ? $t('dist.receive.title', { no: detail.deliveryNo }) : ''" width="min(1000px, 100vw)" :confirm-loading="saving" @ok="onOk">
    <template v-if="detail">
      <a-alert type="info" show-icon :message="$t('dist.receive.tip')" class="block" />
      <a-space class="block" wrap>
        <span>{{ $t('dist.common.receiveDate') }}</span>
        <a-date-picker v-model:value="receiveDate" value-format="YYYY-MM-DD" />
        <a-button size="small" @click="fillAll">{{ $t('dist.receive.allReceived') }}</a-button>
      </a-space>
      <a-table row-key="id" size="small" :columns="columns" :data-source="detail.items" :pagination="false" :scroll="{ x: 900, y: 420 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">
            <div>{{ record.materialCode }} {{ record.materialName }}</div>
            <div class="sub-text">{{ record.batchNo }}</div>
          </template>
          <template v-else-if="column.key === 'qty'">{{ qty(record.qty) }}</template>
          <template v-else-if="column.key === 'received'">
            <a-tooltip :title="record.serialEnabled === 1 ? $t('dist.receive.serialTip') : ''">
              <a-input-number v-model:value="record.received" :min="0" :precision="4" :disabled="record.serialEnabled === 1" class="full" />
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'diffKind'">
            <a-select v-if="Number(record.received) < Number(record.qty)" v-model:value="record.kind" :options="lessKinds" class="full" />
            <DictTag v-else-if="Number(record.received) > Number(record.qty)" type="dist_discrepancy_kind" :value="DiscrepancyKind.OVER" />
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'reason'">
            <a-input v-if="Number(record.received) !== Number(record.qty)" v-model:value="record.reason" :maxlength="255" />
          </template>
        </template>
      </a-table>
    </template>
  </a-modal>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import { getDelivery, receiveDelivery } from '@/api/dist'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'
import { qty } from '@/utils/number'
import { DiscrepancyKind } from '@/constants/enums'

/** 收货：逐行填写实收数量（默认等于发货数量），少于发货时选择少收 / 破损并填写说明，多于发货时为多收 */
const props = defineProps({ kind: { type: String, required: true } })
const emit = defineEmits(['done'])
const { t } = useI18n()
const dict = useDict('dist_discrepancy_kind')
const lessKinds = computed(() =>
  [DiscrepancyKind.SHORT, DiscrepancyKind.DAMAGED].map((v) => ({ value: v, label: findDict(dict.dist_discrepancy_kind, v)?.label ?? v }))
)

const open = ref(false)
const saving = ref(false)
const detail = ref()
const receiveDate = ref()
const columns = computed(() => [
  { title: t('dist.common.material'), key: 'material', width: 260 },
  { title: t('common.field.unit'), dataIndex: 'unit', width: 60 },
  { title: t('dist.receive.shippedQty'), key: 'qty', width: 100, align: 'right' },
  { title: t('dist.receive.receivedQty'), key: 'received', width: 140 },
  { title: t('dist.receive.diffKind'), key: 'diffKind', width: 120 },
  { title: t('dist.receive.reason'), key: 'reason', width: 200 }
])

async function show(id) {
  const d = await getDelivery(props.kind, id)
  d.items.forEach((i) => Object.assign(i, { received: Number(i.qty), kind: DiscrepancyKind.SHORT, reason: '' }))
  detail.value = d
  receiveDate.value = dayjs().format('YYYY-MM-DD')
  open.value = true
}

function fillAll() {
  detail.value.items.forEach((i) => (i.received = Number(i.qty)))
}

async function onOk() {
  saving.value = true
  try {
    await receiveDelivery(props.kind, detail.value.id, {
      receiveDate: receiveDate.value,
      items: detail.value.items.map((i) => ({ itemId: i.id, receivedQty: i.received ?? 0, kind: i.kind, reason: i.reason || null }))
    })
    message.success(t('dist.receive.received'))
    open.value = false
    emit('done', detail.value.id)
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
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
