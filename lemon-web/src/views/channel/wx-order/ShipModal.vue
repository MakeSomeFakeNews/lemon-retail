<template>
  <a-modal v-model:open="open" :title="$t('channel.order.shipTitle', { no: order?.orderId })" :confirm-loading="saving" destroy-on-close @ok="submit">
    <a-alert type="info" show-icon class="block" :message="$t('channel.order.shipTip')" />
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-form-item :label="$t('channel.order.deliveryCompany')" name="companyId">
        <a-select
          v-model:value="form.companyId"
          :options="companies"
          :loading="loadingCompanies"
          show-search
          option-filter-prop="label"
          :placeholder="$t('channel.order.selectCompany')"
        />
      </a-form-item>
      <a-form-item :label="$t('channel.order.waybillNo')" name="waybillNo">
        <a-input v-model:value="form.waybillNo" :maxlength="64" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { shipWxOrder, wxDeliveryCompanies } from '@/api/channel/wxShop'

/** 发货：选择快递公司、填写运单号，生成销售出库后回传小店。open(order) 打开，成功后触发 success */
const emit = defineEmits(['success'])
const { t } = useI18n()

const open = ref(false)
const saving = ref(false)
const order = ref()
const formRef = ref()
const form = reactive({ companyId: undefined, waybillNo: '' })
const companies = ref([])
const loadingCompanies = ref(false)

const rules = {
  companyId: [{ required: true, message: t('channel.order.selectCompany') }],
  waybillNo: [{ required: true, whitespace: true, message: t('channel.order.inputWaybill') }]
}

async function show(record) {
  order.value = record
  Object.assign(form, { companyId: undefined, waybillNo: '' })
  open.value = true
  loadingCompanies.value = true
  try {
    companies.value = (await wxDeliveryCompanies(record.shopId)).map((c) => ({ value: c.id, label: c.name }))
  } finally {
    loadingCompanies.value = false
  }
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const company = companies.value.find((c) => c.value === form.companyId)
    await shipWxOrder(order.value.id, { companyId: form.companyId, companyName: company?.label, waybillNo: form.waybillNo.trim() })
    message.success(t('channel.order.shipped'))
    open.value = false
    emit('success')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.block {
  margin-bottom: 12px;
}
</style>
