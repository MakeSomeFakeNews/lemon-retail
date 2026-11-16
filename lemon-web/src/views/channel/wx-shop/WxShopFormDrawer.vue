<template>
  <FormDrawer :drawer="drawer" :create-title="$t('channel.shop.createTitle')" :edit-title="$t('channel.shop.editTitle')" width="min(760px, 100vw)">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <div class="section-title">{{ $t('channel.shop.sectionBasic') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('channel.shop.name')" name="name">
            <a-input v-model:value="form.name" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('channel.shop.mode')" name="mode" :extra="$t('channel.shop.modeExtra')">
            <a-radio-group v-model:value="form.mode" :options="toOptions(dict.ch_wx_shop_mode)" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('channel.shop.store')" name="storeId">
            <StoreSelect v-model:value="form.storeId" trading @update:value="onStoreChange" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('channel.shop.warehouse')" name="warehouseId" :extra="$t('channel.shop.warehouseExtra')">
            <WarehouseSelect v-model:value="form.warehouseId" :store-id="form.storeId" :disabled="!form.storeId" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('channel.shop.customer')" name="customerId" :extra="$t('channel.shop.customerExtra')">
            <PartnerSelect v-model:value="form.customerId" type="customer" :label="form.customerName" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('common.field.status')" name="status">
            <a-radio-group v-model:value="form.status">
              <a-radio :value="STATUS_ENABLED">{{ $t('common.status.enabled') }}</a-radio>
              <a-radio :value="STATUS_DISABLED">{{ $t('common.status.disabled') }}</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
      </a-row>

      <template v-if="form.mode === WxShopMode.LIVE">
        <div class="section-title">{{ $t('channel.shop.sectionApi') }}</div>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="AppID" name="appId">
              <a-input v-model:value="form.appId" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="AppSecret" name="appSecret" :extra="secretExtra(form.appSecretSet)">
              <a-input-password v-model:value="form.appSecret" :maxlength="128" autocomplete="new-password" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('channel.shop.returnAddressId')" name="returnAddressId" :extra="$t('channel.shop.returnAddressIdExtra')">
              <a-input v-model:value="form.returnAddressId" :maxlength="64" />
            </a-form-item>
          </a-col>
        </a-row>
      </template>

      <div class="section-title">{{ $t('channel.shop.sectionPush') }}</div>
      <a-alert type="info" show-icon class="block" :message="$t('channel.shop.pushTip')">
        <template v-if="callbackUrl" #description>
          <a-typography-paragraph :copyable="{ text: callbackUrl }" class="callback-url">{{ callbackUrl }}</a-typography-paragraph>
        </template>
      </a-alert>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('channel.shop.pushToken')" name="pushToken" :extra="secretExtra(form.pushTokenSet)">
            <a-input-password v-model:value="form.pushToken" :maxlength="64" autocomplete="new-password" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item label="EncodingAESKey" name="aesKey" :extra="secretExtra(form.aesKeySet, $t('channel.shop.aesKeyExtra'))">
            <a-input-password v-model:value="form.aesKey" :maxlength="43" autocomplete="new-password" />
          </a-form-item>
        </a-col>
        <a-col v-if="isEdit && (form.pushTokenSet || form.aesKeySet)" :span="24">
          <a-form-item name="clearPushSecrets" class="check-item">
            <a-checkbox v-model:checked="form.clearPushSecrets">{{ $t('channel.shop.clearPushSecrets') }}</a-checkbox>
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('channel.shop.sectionSync') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item name="orderSyncEnabled" class="check-item" :extra="$t('channel.shop.orderSyncExtra')">
            <a-checkbox v-model:checked="form.orderSyncEnabled">{{ $t('channel.shop.orderSyncEnabled') }}</a-checkbox>
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item name="autoCreateOrder" class="check-item" :extra="$t('channel.shop.autoCreateOrderExtra')">
            <a-checkbox v-model:checked="form.autoCreateOrder">{{ $t('channel.shop.autoCreateOrder') }}</a-checkbox>
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item name="stockSyncEnabled" class="check-item" :extra="$t('channel.shop.stockSyncExtra')">
            <a-checkbox v-model:checked="form.stockSyncEnabled">{{ $t('channel.shop.stockSyncEnabled') }}</a-checkbox>
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('channel.shop.stockSyncInterval')" name="stockSyncInterval">
            <a-input-number v-model:value="form.stockSyncInterval" :min="5" :max="1440" :precision="0" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('channel.shop.safetyStock')" name="safetyStock">
            <a-input-number v-model:value="form.safetyStock" :min="0" :precision="4" class="full" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item :label="$t('common.field.remark')" name="remark">
        <a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" show-count />
      </a-form-item>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import StoreSelect from '@/components/store/StoreSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { addWxShop, getWxShop, updateWxShop } from '@/api/channel/wxShop'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { WxShopMode } from '@/constants/enums'
import { STATUS_DISABLED, STATUS_ENABLED } from '@/utils/constants'
import { toOptions } from '@/utils/dict'

/**
 * 新增 / 编辑微信小店店铺。密钥只写不读：已设置时留空表示不修改。通过 ref 调用 openCreate() / openEdit(record)
 */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ch_wx_shop_mode')

const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined,
    name: '',
    mode: WxShopMode.MOCK,
    storeId: undefined,
    warehouseId: undefined,
    customerId: undefined,
    customerName: undefined,
    status: STATUS_ENABLED,
    appId: '',
    appSecret: '',
    returnAddressId: '',
    pushToken: '',
    aesKey: '',
    clearPushSecrets: false,
    orderSyncEnabled: true,
    autoCreateOrder: true,
    stockSyncEnabled: false,
    stockSyncInterval: 30,
    safetyStock: 0,
    remark: ''
  }),
  fetch: async (record) => ({ ...(await getWxShop(record.id)), appSecret: '', pushToken: '', aesKey: '', clearPushSecrets: false }),
  create: addWxShop,
  update: updateWxShop,
  toPayload: (f) => ({
    name: f.name,
    mode: f.mode,
    storeId: f.storeId,
    warehouseId: f.warehouseId,
    customerId: f.customerId ?? null,
    status: f.status,
    appId: f.appId,
    appSecret: f.appSecret || null,
    returnAddressId: f.returnAddressId,
    pushToken: f.pushToken || null,
    aesKey: f.aesKey || null,
    clearPushSecrets: f.clearPushSecrets,
    orderSyncEnabled: f.orderSyncEnabled,
    autoCreateOrder: f.autoCreateOrder,
    stockSyncEnabled: f.stockSyncEnabled,
    stockSyncInterval: f.stockSyncInterval,
    safetyStock: f.safetyStock ?? 0,
    remark: f.remark
  }),
  onSuccess: () => emit('success')
})
const { form, formRef, isEdit } = drawer

/** 消息推送地址：当前访问地址 + 接口路径（保存后才有店铺 ID） */
const callbackUrl = computed(() => (form.value.callbackPath ? `${window.location.origin}/api${form.value.callbackPath}` : ''))

const secretExtra = (set, extra) => [set ? t('channel.shop.secretKeep') : '', extra || ''].filter(Boolean).join(' ')

function onStoreChange() {
  form.value.warehouseId = undefined
}

const rules = {
  name: [{ required: true, whitespace: true, message: t('channel.shop.inputName') }],
  mode: [{ required: true, message: t('channel.shop.selectMode') }],
  storeId: [{ required: true, message: t('channel.shop.selectStore') }],
  warehouseId: [{ required: true, message: t('channel.shop.selectWarehouse') }],
  appId: [{ required: true, whitespace: true, message: t('channel.shop.inputAppId') }],
  appSecret: [
    {
      validator: (_r, v) => (!v && !form.value.appSecretSet ? Promise.reject(t('channel.shop.inputAppSecret')) : Promise.resolve())
    }
  ],
  aesKey: [{ pattern: /^[A-Za-z0-9]{43}$/, message: t('channel.shop.aesKeyInvalid') }]
}

defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.section-title {
  margin: 4px 0 12px;
  padding-left: 8px;
  border-left: 3px solid var(--app-primary);
  font-weight: 600;
  line-height: 1.2;
}
.full {
  width: 100%;
}
.check-item {
  margin-bottom: 8px;
}
.block {
  margin-bottom: 12px;
}
.callback-url {
  margin: 4px 0 0 !important;
  word-break: break-all;
}
</style>
