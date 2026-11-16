<template>
  <a-modal v-model:open="visible" :title="$t('pos.memberModal.title')" :footer="null" @cancel="emit('closed')">
    <a-input-search
      ref="codeRef"
      v-model:value="code"
      size="large"
      :placeholder="$t('pos.memberModal.codePh')"
      :enter-button="$t('pos.memberModal.find')"
      :loading="loading"
      @search="find"
    />
    <template v-if="notFound && canRegister">
      <a-divider>{{ $t('pos.memberModal.register') }}</a-divider>
      <a-form layout="vertical">
        <a-form-item :label="$t('pos.member.phone')">
          <a-input v-model:value="phone" :maxlength="20" />
        </a-form-item>
        <a-form-item :label="$t('pos.member.name')">
          <a-input v-model:value="name" :maxlength="64" @press-enter="register" />
        </a-form-item>
        <a-button type="primary" block :loading="loading" @click="register">{{ $t('pos.memberModal.registerAndUse') }}</a-button>
      </a-form>
    </template>
  </a-modal>
</template>

<script setup>
import { nextTick, ref } from 'vue'
import { identifyMember, quickMember } from '@/api/pos'

/** 识别会员（手机号或会员号）；找不到时可快速登记（开卡门店为收银台的门店） */
const props = defineProps({ terminalId: Number, canRegister: Boolean })
const emit = defineEmits(['select', 'closed'])

const visible = ref(false)
const loading = ref(false)
const code = ref('')
const phone = ref('')
const name = ref('')
const notFound = ref(false)
const codeRef = ref()

function open() {
  code.value = ''
  phone.value = ''
  name.value = ''
  notFound.value = false
  visible.value = true
  nextTick(() => codeRef.value?.focus())
}

async function find() {
  if (!code.value?.trim()) return
  loading.value = true
  try {
    const m = await identifyMember(code.value.trim())
    done(m)
  } catch {
    notFound.value = true
    phone.value = /^[0-9+-]{5,20}$/.test(code.value.trim()) ? code.value.trim() : ''
  } finally {
    loading.value = false
  }
}

async function register() {
  loading.value = true
  try {
    done(await quickMember({ terminalId: props.terminalId, phone: phone.value, name: name.value || null }))
  } finally {
    loading.value = false
  }
}

function done(m) {
  visible.value = false
  emit('select', m)
  emit('closed')
}

defineExpose({ open })
</script>
