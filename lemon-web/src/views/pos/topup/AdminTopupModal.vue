<template>
  <a-modal v-model:open="visible" :title="$t('pos.topup.adminTopup')" width="560px" :confirm-loading="saving" @ok="submit">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-form-item :label="$t('store.common.store')" name="storeId">
        <StoreSelect v-model:value="form.storeId" :allow-clear="false" />
      </a-form-item>
      <a-form-item :label="$t('pos.marketing.member')" name="memberId">
        <MemberIdentify ref="identifyRef" v-model:value="form.memberId" />
      </a-form-item>
      <a-row :gutter="12">
        <a-col :span="12">
          <a-form-item :label="$t('pos.topup.amount')" name="amount">
            <a-input-number v-model:value="form.amount" :min="0.01" :precision="2" class="full" @change="onAmount" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('pos.topup.bonusAmount')" name="bonusAmount" :extra="$t('pos.topup.bonusTip')">
            <a-input-number v-model:value="form.bonusAmount" :min="0" :precision="2" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('pos.sale.payMethod')" name="payMethod">
            <a-select v-model:value="form.payMethod" :options="methods.map((m) => ({ value: m, label: dictLabel('pos_pay_method', m) }))" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('pos.topup.refNo')" name="refNo">
            <a-input v-model:value="form.refNo" :maxlength="64" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item :label="$t('pos.topup.account')" name="accountId" :extra="$t('pos.topup.accountTip')">
        <AccountSelect v-model:value="form.accountId" :roles="['CASH', 'BANK']" allow-clear />
      </a-form-item>
      <a-form-item :label="$t('common.field.remark')" name="remark">
        <a-input v-model:value="form.remark" :maxlength="255" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import StoreSelect from '@/components/store/StoreSelect.vue'
import { adminTopup, getMemberRule } from '@/api/pos'
import { money } from '@/utils/number'
import { PayMethod } from '@/constants/enums'
import { bonusFor } from '../marketing'
import { dictLabel } from '../pos'
import MemberIdentify from '../MemberIdentify.vue'

/** 后台充值：登记收款方式与结算账户（为空取门店默认结算账户），赠送金额默认按充值档位 */
const emit = defineEmits(['saved'])
const { t } = useI18n()
const methods = [PayMethod.CASH, PayMethod.BANK_CARD, PayMethod.WECHAT, PayMethod.ALIPAY]

const visible = ref(false)
const saving = ref(false)
const formRef = ref()
const identifyRef = ref()
const tiers = ref([])
const empty = () => ({ storeId: undefined, memberId: undefined, amount: undefined, bonusAmount: 0, payMethod: PayMethod.CASH, accountId: undefined, refNo: '', remark: '' })
const form = reactive(empty())
const rules = {
  storeId: [{ required: true, message: t('pos.topup.storeRequired') }],
  memberId: [{ required: true, message: t('pos.marketing.memberRequired') }],
  amount: [{ required: true, message: t('pos.topup.amountRequired') }]
}

async function open() {
  Object.assign(form, empty())
  identifyRef.value?.reset()
  visible.value = true
  tiers.value = (await getMemberRule().catch(() => null))?.tiers || []
}

function onAmount() {
  form.bonusAmount = bonusFor(tiers.value, form.amount)
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const r = await adminTopup({ ...form, accountId: form.accountId || null, refNo: form.refNo || null, remark: form.remark || null })
    message.success(t('pos.topup.success', { no: r.topupNo, balance: money(r.balanceAfter) }))
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.full {
  width: 100%;
}
</style>
