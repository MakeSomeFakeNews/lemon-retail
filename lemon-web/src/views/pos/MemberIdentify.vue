<template>
  <div>
    <a-input-search v-model:value="code" :placeholder="$t('pos.marketing.memberCodePh')" :enter-button="$t('pos.marketing.identify')" :loading="loading" @search="identify" />
    <div v-if="member" class="member-info">
      <b>{{ member.name || member.memberCode }}</b>
      <span class="sub-text">{{ member.memberCode }} · {{ member.phone }} · {{ member.levelName || '-' }}</span>
      <div class="sub-text">{{ $t('pos.marketing.memberAccount', { points: qty(member.points), balance: money(member.balance) }) }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { identifyMember } from '@/api/pos'
import { money, qty } from '@/utils/number'

/** 按手机号或会员号识别会员（后台调整、后台充值使用），v-model 为会员 ID */
const props = defineProps({ value: Number })
const emit = defineEmits(['update:value', 'select'])
const code = ref('')
const member = ref(null)
const loading = ref(false)

async function identify() {
  if (!code.value?.trim()) return
  loading.value = true
  try {
    member.value = await identifyMember(code.value.trim())
    emit('update:value', member.value.id)
    emit('select', member.value)
  } catch {
    member.value = null
    emit('update:value', undefined)
  } finally {
    loading.value = false
  }
}

function reset() {
  code.value = ''
  member.value = null
}

defineExpose({ reset, current: () => (props.value ? member.value : null) })
</script>

<style scoped>
.member-info {
  margin-top: 6px;
}
.sub-text {
  margin-left: 6px;
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
