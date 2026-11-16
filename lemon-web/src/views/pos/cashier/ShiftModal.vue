<template>
  <!-- 开班：登记备用金 -->
  <a-modal v-model:open="openVisible" :title="$t('pos.shift.openTitle')" :confirm-loading="saving" @ok="submitOpen" @cancel="emit('closed')">
    <a-form layout="vertical">
      <a-form-item :label="$t('pos.shift.openingCash')">
        <a-input-number ref="cashRef" v-model:value="openingCash" :min="0" :precision="2" class="full" size="large" @press-enter="submitOpen" />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 交班：系统金额（按支付方式）与实点现金 -->
  <a-modal v-model:open="closeVisible" :title="$t('pos.shift.closeTitle', { no: report?.shift?.shiftNo })" width="640px" @cancel="emit('closed')">
    <template v-if="report">
      <ShiftReportView :report="report" />
      <a-alert v-if="report.unsettled" type="error" show-icon :message="$t('pos.shift.unsettled', { n: report.unsettled })" class="gap" />
      <a-form layout="vertical" class="gap">
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item :label="$t('pos.shift.countedCash')">
              <a-input-number v-model:value="countedCash" :min="0" :precision="2" class="full" size="large" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('pos.shift.cashDiff')">
              <span class="diff" :class="{ neg: diff < 0, pos: diff > 0 }">{{ countedCash == null ? '-' : money(diff) }}</span>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="$t('common.field.remark')">
          <a-input v-model:value="remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </template>
    <template #footer>
      <a-button @click="closeVisible = false; emit('closed')">{{ $t('common.action.cancel') }}</a-button>
      <a-button type="primary" :loading="saving" :disabled="countedCash == null" @click="submitClose">{{ $t('pos.shift.confirmClose') }}</a-button>
    </template>
  </a-modal>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { closeShift, openShift, shiftReport } from '@/api/pos'
import { money, round2 } from '@/utils/number'
import { printShift } from '../printing'
import ShiftReportView from '../shift/ShiftReportView.vue'

/** 收银台的开班、交班（交班后打印交班单） */
const emit = defineEmits(['changed', 'closed'])
const { t } = useI18n()

const openVisible = ref(false)
const closeVisible = ref(false)
const saving = ref(false)
const terminalId = ref()
const openingCash = ref(0)
const cashRef = ref()
const report = ref()
const countedCash = ref()
const remark = ref('')
const diff = computed(() => round2(Number(countedCash.value || 0) - Number(report.value?.expectedCash || 0)))

function openOpen(id) {
  terminalId.value = id
  openingCash.value = 0
  openVisible.value = true
  nextTick(() => cashRef.value?.focus())
}

async function submitOpen() {
  saving.value = true
  try {
    await openShift({ terminalId: terminalId.value, openingCash: openingCash.value || 0 })
    message.success(t('pos.shift.opened'))
    openVisible.value = false
    emit('changed')
  } finally {
    saving.value = false
  }
}

async function openClose(shift) {
  report.value = await shiftReport(shift.id)
  countedCash.value = undefined
  remark.value = ''
  closeVisible.value = true
}

async function submitClose() {
  saving.value = true
  try {
    const r = await closeShift(report.value.shift.id, { countedCash: countedCash.value, remark: remark.value || null })
    message.success(t('pos.shift.closed'))
    closeVisible.value = false
    printShift(r).catch(() => {})
    emit('changed')
  } finally {
    saving.value = false
  }
}

defineExpose({ openOpen, openClose })
</script>

<style scoped>
.full {
  width: 100%;
}
.gap {
  margin-top: 12px;
}
.diff {
  font-size: 20px;
  font-weight: 600;
}
.diff.neg {
  color: #d4380d;
}
.diff.pos {
  color: #389e0d;
}
</style>
