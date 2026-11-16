<template>
  <a-modal v-model:open="visible" :title="line ? $t('pos.lineEdit.title', { name: line.name }) : ''" @ok="save" @cancel="emit('closed')">
    <a-form v-if="line" layout="vertical">
      <a-form-item :label="$t('pos.lineEdit.price')" :extra="$t('pos.lineEdit.priceTip')">
        <a-input-number v-model:value="price" :min="0" :precision="2" class="full" :disabled="!canPrice" :placeholder="money(line.price)" />
      </a-form-item>
      <a-form-item :label="$t('pos.lineEdit.discountRate')" :extra="$t('pos.lineEdit.discountTip')">
        <a-input-number v-model:value="rate" :min="1" :max="100" :precision="2" class="full" :disabled="!canDiscount" addon-after="%" @press-enter="save" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { money } from '@/utils/number'

/** 单行改价（需要改价权限）、单行折扣（需要折扣权限，受折扣上限约束，结算时由后端校验） */
defineProps({ canPrice: Boolean, canDiscount: Boolean })
const emit = defineEmits(['saved', 'closed'])

const visible = ref(false)
const line = ref()
const price = ref()
const rate = ref()

function open(l) {
  line.value = l
  price.value = l.manualPrice ?? undefined
  rate.value = l.discountRate ?? undefined
  visible.value = true
}

function save() {
  line.value.manualPrice = price.value ?? null
  line.value.discountRate = rate.value && rate.value < 100 ? rate.value : null
  visible.value = false
  emit('saved')
  emit('closed')
}

defineExpose({ open })
</script>

<style scoped>
.full {
  width: 100%;
}
</style>
