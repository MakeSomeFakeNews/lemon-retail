<template>
  <a-modal v-model:open="visible" :title="$t('pos.terminal.payAccountsTitle')" :confirm-loading="saving" @ok="save">
    <a-alert type="info" show-icon :message="$t('pos.terminal.payAccountsTip')" class="tip" />
    <a-form layout="vertical">
      <a-form-item :label="$t('store.common.store')">
        <StoreSelect v-model:value="storeId" trading :allow-clear="false" @update:value="loadAccounts" />
      </a-form-item>
      <a-form-item v-for="m in methods" :key="m" :label="dictLabel('pos_pay_method', m)">
        <AccountSelect v-model:value="accounts[m]" :roles="['CASH', 'BANK']" allow-clear :placeholder="$t('pos.terminal.storeDefaultAccount')" :disabled="!storeId" />
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
import { payAccounts, savePayAccounts } from '@/api/pos'
import { PayMethod } from '@/constants/enums'
import { dictLabel } from '../pos'

/** 门店收款账户：微信、支付宝、银行卡记入的结算账户（现金为收银台的现金账户），未设置时为门店默认结算账户 */
const { t } = useI18n()
const methods = [PayMethod.WECHAT, PayMethod.ALIPAY, PayMethod.BANK_CARD]
const visible = ref(false)
const saving = ref(false)
const storeId = ref()
const accounts = reactive({})

async function open(store) {
  storeId.value = store
  methods.forEach((m) => (accounts[m] = undefined))
  visible.value = true
  if (store) await loadAccounts(store)
}

async function loadAccounts(id) {
  const map = await payAccounts(id)
  methods.forEach((m) => (accounts[m] = map[m] || undefined))
}

async function save() {
  if (!storeId.value) return
  saving.value = true
  try {
    await savePayAccounts({ storeId: storeId.value, items: methods.map((m) => ({ payMethod: m, accountId: accounts[m] || null })) })
    message.success(t('common.msg.saveSuccess'))
    visible.value = false
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
</style>
