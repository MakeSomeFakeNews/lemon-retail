<template>
  <a-modal v-model:open="visible" :title="$t('pos.topupDialog.title')" width="520px" :mask-closable="false" :ok-text="$t('pos.topupDialog.confirm')"
           :confirm-loading="saving" :ok-button-props="{ disabled: !valid }" @ok="submit" @cancel="close">
    <div v-if="member" class="member">{{ member.name || member.memberCode }} · {{ member.phone }} · {{ $t('pos.cashier.balance') }} {{ money(member.balance) }}</div>
    <a-form layout="vertical">
      <a-row :gutter="12">
        <a-col :span="12">
          <a-form-item :label="$t('pos.topup.amount')">
            <a-input-number ref="amountRef" v-model:value="amount" :min="0.01" :precision="2" size="large" class="full" @change="onAmount" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('pos.topup.bonusAmount')" :extra="canOverride ? '' : $t('pos.topupDialog.bonusByTier')">
            <a-input-number v-model:value="bonus" :min="0" :precision="2" size="large" class="full" :disabled="!canOverride" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('pos.sale.payMethod')">
            <a-radio-group v-model:value="method" button-style="solid">
              <a-radio-button v-for="m in methods" :key="m" :value="m">{{ dictLabel('pos_pay_method', m) }}</a-radio-button>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col v-if="method === PayMethod.CASH" :span="12">
          <a-form-item :label="$t('pos.pay.tendered')">
            <a-input-number v-model:value="tendered" :min="0" :precision="2" size="large" class="full" @press-enter="submit" />
          </a-form-item>
        </a-col>
        <a-col v-if="method === PayMethod.CASH" :span="12">
          <a-form-item :label="$t('pos.pay.change')">
            <div class="change">{{ money(change) }}</div>
          </a-form-item>
        </a-col>
        <a-col v-if="method === PayMethod.BANK_CARD" :span="24">
          <a-form-item :label="$t('pos.pay.refNo')">
            <a-input v-model:value="refNo" size="large" :maxlength="64" @press-enter="submit" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { cashierTopup } from '@/api/pos'
import { money, round2 } from '@/utils/number'
import { PayMethod } from '@/constants/enums'
import { bonusFor } from '../marketing'
import { dictLabel } from '../pos'

/**
 * 收银台储值充值（F10，需要 pos:sale:topup，已识别会员）：充值金额、赠送（按档位，有「后台充值」权限可修改），现金（找零）或银行卡付款；
 * 在当班班次中收款，充值后返回新的余额
 */
const props = defineProps({ terminalId: Number, ctx: Object })
const emit = defineEmits(['done', 'closed'])
const { t } = useI18n()
const methods = [PayMethod.CASH, PayMethod.BANK_CARD]

const visible = ref(false)
const saving = ref(false)
const member = ref(null)
const amount = ref()
const bonus = ref(0)
const method = ref(PayMethod.CASH)
const tendered = ref()
const refNo = ref('')
const amountRef = ref()

const canOverride = computed(() => (props.ctx?.perms || []).includes('pos:topup:add'))
const change = computed(() => Math.max(0, round2(Number(tendered.value || 0) - Number(amount.value || 0))))
const valid = computed(() => Number(amount.value) > 0 && (method.value !== PayMethod.CASH || Number(tendered.value || 0) >= Number(amount.value)))

function open(m) {
  member.value = m
  amount.value = undefined
  bonus.value = 0
  method.value = PayMethod.CASH
  tendered.value = undefined
  refNo.value = ''
  visible.value = true
  nextTick(() => amountRef.value?.focus())
}

function onAmount() {
  bonus.value = bonusFor(props.ctx?.topupTiers, amount.value)
  if (method.value === PayMethod.CASH) tendered.value = amount.value
}

async function submit() {
  if (!valid.value || saving.value) return
  saving.value = true
  try {
    const tender = method.value === PayMethod.CASH
      ? { payMethod: PayMethod.CASH, amount: tendered.value }
      : { payMethod: method.value, amount: amount.value, refNo: refNo.value || null }
    const r = await cashierTopup({
      terminalId: props.terminalId,
      memberId: member.value.id,
      amount: amount.value,
      bonusAmount: canOverride.value ? bonus.value : null,
      tenders: [tender]
    })
    message.success(t('pos.topup.success', { no: r.topupNo, balance: money(r.balanceAfter) }))
    visible.value = false
    emit('done', r)
  } finally {
    saving.value = false
  }
}

function close() {
  visible.value = false
  emit('closed')
}

defineExpose({ open })
</script>

<style scoped>
.member {
  margin-bottom: 12px;
  color: var(--app-text-2);
}
.full {
  width: 100%;
}
.change {
  font-size: 22px;
  font-weight: 700;
  color: #389e0d;
}
</style>
