<template>
  <div class="page-wrap">
    <a-card :bordered="false" class="page-card">
      <a-alert type="info" show-icon :message="$t('pos.payConfig.tip')" class="tip" />
      <a-form :model="form" layout="vertical" class="form">
        <a-form-item :label="$t('pos.payConfig.provider')">
          <a-radio-group v-model:value="form.provider">
            <a-radio v-for="p in providers" :key="p" :value="p">{{ dictLabel('pos_pay_provider', p) }}</a-radio>
          </a-radio-group>
          <div class="sub-text">{{ $t(`pos.payConfig.providerTip.${form.provider}`) }}</div>
        </a-form-item>
        <template v-if="form.provider !== PayProviderType.OFFLINE">
          <a-form-item :label="$t('pos.payConfig.appId')">
            <a-input v-model:value="form.appId" :maxlength="64" />
          </a-form-item>
          <a-form-item :label="$t('pos.payConfig.merchantNo')">
            <a-input v-model:value="form.merchantNo" :maxlength="64" />
          </a-form-item>
          <a-form-item :label="$t('pos.payConfig.apiKey')">
            <a-input-password v-model:value="form.apiKey" :placeholder="hasApiKey ? $t('pos.payConfig.secretSet') : ''" autocomplete="new-password" />
          </a-form-item>
          <a-form-item :label="$t('pos.payConfig.apiSecret')">
            <a-textarea v-model:value="form.apiSecret" :rows="3" :placeholder="hasApiSecret ? $t('pos.payConfig.secretSet') : ''" />
          </a-form-item>
          <a-form-item :label="$t('pos.payConfig.notifyUrl')">
            <a-input v-model:value="form.notifyUrl" :maxlength="255" />
          </a-form-item>
        </template>
        <a-form-item :label="$t('common.field.remark')">
          <a-input v-model:value="form.remark" :maxlength="255" />
        </a-form-item>
        <a-button v-perm="'pos:payConfig:edit'" type="primary" :loading="saving" @click="save">{{ $t('common.action.save') }}</a-button>
      </a-form>
    </a-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { getPayConfig, savePayConfig } from '@/api/pos'
import { useDict } from '@/composables/useDict'
import { PayProviderType } from '@/constants/enums'
import { dictLabel } from '../pos'

/** 聚合支付通道设置（按租户）：没有接入服务商时为线下登记；密钥只写，已设置的显示为「已设置」，留空保存时保持原值 */
const { t } = useI18n()
useDict('pos_pay_provider')
const providers = ref([PayProviderType.OFFLINE])
const hasApiKey = ref(false)
const hasApiSecret = ref(false)
const saving = ref(false)
const form = reactive({ provider: PayProviderType.OFFLINE, appId: '', merchantNo: '', apiKey: '', apiSecret: '', notifyUrl: '', remark: '' })

async function load() {
  const c = await getPayConfig()
  providers.value = c.providers?.length ? c.providers : [PayProviderType.OFFLINE]
  hasApiKey.value = c.hasApiKey
  hasApiSecret.value = c.hasApiSecret
  Object.assign(form, { provider: c.provider, appId: c.appId || '', merchantNo: c.merchantNo || '', apiKey: '', apiSecret: '', notifyUrl: c.notifyUrl || '', remark: c.remark || '' })
}
onMounted(load)

async function save() {
  saving.value = true
  try {
    await savePayConfig({ ...form, apiKey: form.apiKey || null, apiSecret: form.apiSecret || null })
    message.success(t('common.msg.saveSuccess'))
    await load()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.page-wrap {
  padding: 0;
}
.tip {
  margin-bottom: 16px;
}
.form {
  max-width: 640px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
