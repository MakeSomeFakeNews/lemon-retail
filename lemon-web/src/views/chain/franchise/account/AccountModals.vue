<template>
  <a-modal v-model:open="topupOpen" :title="$t('chain.account.topupTitle', { name: store.storeName })" :confirm-loading="saving" @ok="saveTopup">
    <a-alert type="info" show-icon :message="$t('chain.account.topupTip')" class="mb12" />
    <a-form ref="topupRef" :model="topup" :rules="topupRules" layout="vertical">
      <a-form-item :label="$t('chain.common.date')" name="date"><a-date-picker v-model:value="topup.date" value-format="YYYY-MM-DD" class="full" /></a-form-item>
      <a-form-item :label="$t('chain.common.account')" name="accountId">
        <AccountSelect v-model:value="topup.accountId" :roles="['CASH', 'BANK']" />
      </a-form-item>
      <a-form-item :label="$t('chain.common.amount')" name="amount">
        <a-input-number v-model:value="topup.amount" :min="0.01" :precision="2" class="full" />
      </a-form-item>
      <a-form-item :label="$t('common.field.remark')" name="remark"><a-input v-model:value="topup.remark" :maxlength="255" /></a-form-item>
    </a-form>
  </a-modal>

  <a-modal v-model:open="settingsOpen" :title="$t('chain.account.settingsTitle', { name: store.storeName })" :confirm-loading="saving" @ok="saveSettings">
    <a-form :model="settings" layout="vertical">
      <a-form-item :label="$t('chain.account.creditLimit')" :extra="$t('chain.account.creditLimitTip')">
        <a-input-number v-model:value="settings.creditLimit" :min="0" :precision="2" :placeholder="$t('chain.account.unlimited')" class="full" />
      </a-form-item>
      <a-form-item :label="$t('common.field.remark')"><a-input v-model:value="settings.remark" :maxlength="255" /></a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import { topupFranchise, updateFranchiseAccount } from '@/api/chain'

/** 加盟商账户的充值（预付货款，登记预收款并自动冲抵加盟应收）、账户设置（月结信用额度） */
const emit = defineEmits(['saved'])
const { t } = useI18n()
const store = ref({})
const saving = ref(false)

const topupOpen = ref(false)
const topupRef = ref()
const topup = reactive({ date: undefined, accountId: undefined, amount: undefined, remark: '' })
const topupRules = {
  date: [{ required: true, message: t('chain.common.selectDate') }],
  accountId: [{ required: true, message: t('chain.common.selectAccount') }],
  amount: [{ required: true, message: t('chain.common.inputAmount') }]
}
function openTopup(record) {
  store.value = record
  Object.assign(topup, { date: dayjs().format('YYYY-MM-DD'), accountId: undefined, amount: undefined, remark: '' })
  topupOpen.value = true
}
async function saveTopup() {
  await topupRef.value.validate()
  saving.value = true
  try {
    await topupFranchise(store.value.storeId, { ...topup })
    message.success(t('chain.account.topupDone'))
    topupOpen.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}

const settingsOpen = ref(false)
const settings = reactive({ creditLimit: null, remark: '' })
function openSettings(record) {
  store.value = record
  Object.assign(settings, { creditLimit: record.creditLimit ?? null, remark: record.remark || '' })
  settingsOpen.value = true
}
async function saveSettings() {
  saving.value = true
  try {
    await updateFranchiseAccount(store.value.storeId, { creditLimit: settings.creditLimit ?? null, remark: settings.remark })
    message.success(t('common.msg.saveSuccess'))
    settingsOpen.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}

defineExpose({ openTopup, openSettings })
</script>

<style scoped>
.full {
  width: 100%;
}
.mb12 {
  margin-bottom: 12px;
}
</style>
