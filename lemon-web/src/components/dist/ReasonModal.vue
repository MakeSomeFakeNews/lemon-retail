<template>
  <a-modal v-model:open="open" :title="title" :confirm-loading="saving" @ok="onOk">
    <a-textarea v-model:value="reason" :maxlength="255" :rows="3" :placeholder="$t('dist.audit.reasonPh')" />
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { t } from '@/locales'

/** 填写原因后执行（驳回、关闭）：open(标题, (原因) => Promise)，成功后触发 done */
const emit = defineEmits(['done'])
const open = ref(false)
const saving = ref(false)
const title = ref('')
const reason = ref('')
let action = null

function show(text, fn) {
  title.value = text
  reason.value = ''
  action = fn
  open.value = true
}

async function onOk() {
  saving.value = true
  try {
    await action(reason.value || null)
    message.success(t('common.msg.operationSuccess'))
    open.value = false
    emit('done')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>
