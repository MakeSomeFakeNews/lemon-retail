<template>
  <a-drawer v-model:open="visible" :title="$t('pos.held.title')" width="min(560px, 100vw)" @close="emit('closed')">
    <a-table row-key="id" size="small" :columns="columns" :data-source="list" :loading="loading" :pagination="false" :custom-row="(r) => ({ onDblclick: () => resume(r) })">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'amount'">{{ money(record.amount) }}</template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="resume(record)">{{ $t('pos.held.resume') }}</a-button>
            <a-popconfirm :title="$t('pos.held.confirmDelete')" @confirm="remove(record)">
              <a-button type="link" danger size="small">{{ $t('common.action.delete') }}</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>
  </a-drawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { deleteHeld, heldList, resumeHeld } from '@/api/pos'
import { money } from '@/utils/number'

/** 取单：本门店收银台的挂单，取单后挂单删除 */
const props = defineProps({ terminalId: Number })
const emit = defineEmits(['resume', 'closed'])
const { t } = useI18n()

const visible = ref(false)
const loading = ref(false)
const list = ref([])
const columns = computed(() => [
  { title: t('pos.held.time'), dataIndex: 'createTime', width: 150 },
  { title: t('pos.held.terminal'), dataIndex: 'terminalName', width: 110, ellipsis: true },
  { title: t('pos.held.items'), dataIndex: 'itemCount', width: 60, align: 'right' },
  { title: t('common.field.amount'), key: 'amount', width: 90, align: 'right' },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true },
  { title: '', key: 'action', width: 120 }
])

async function open() {
  visible.value = true
  loading.value = true
  try {
    list.value = await heldList(props.terminalId)
  } finally {
    loading.value = false
  }
}

async function resume(record) {
  const held = await resumeHeld(record.id, props.terminalId)
  visible.value = false
  emit('resume', held)
}

async function remove(record) {
  await deleteHeld(record.id, props.terminalId)
  list.value = list.value.filter((x) => x.id !== record.id)
}

defineExpose({ open })
</script>
