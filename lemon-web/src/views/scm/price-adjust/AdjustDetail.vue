<template>
  <SubPage :open="open" :title="detail ? $t('scm.priceAdjust.detailTitle', { no: detail.adjustNo }) : ''" width="min(1000px, 100vw)" @close="open = false">
    <template v-if="detail" #extra>
      <a-space>
        <template v-if="isDraft(detail)">
          <span v-perm="'scm:priceAdjust:remove'">
            <a-popconfirm :title="$t('scm.priceAdjust.confirmDelete', { no: detail.adjustNo })" @confirm="removeAndClose">
              <a-button danger>{{ $t('common.action.delete') }}</a-button>
            </a-popconfirm>
          </span>
          <a-button v-perm="'scm:priceAdjust:edit'" @click="emit('edit', detail)">{{ $t('common.action.edit') }}</a-button>
          <a-button v-perm="'scm:priceAdjust:audit'" type="primary" @click="submit(detail)">{{ $t('common.action.submit') }}</a-button>
        </template>
        <a-button v-else-if="detail.status === PriceAdjustStatus.APPROVING" v-perm="'scm:priceAdjust:audit'" @click="withdraw(detail)">
          {{ $t('scm.priceAdjust.withdraw') }}
        </a-button>
        <a-button v-else-if="detail.status === PriceAdjustStatus.APPROVED" v-perm="'scm:priceAdjust:audit'" danger @click="voidAdjust(detail)">
          {{ $t('scm.priceAdjust.void') }}
        </a-button>
      </a-space>
    </template>
    <template v-if="detail">
      <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
        <a-descriptions-item :label="$t('scm.priceAdjust.target')"><DictTag type="scm_price_adjust_target" :value="detail.target" /></a-descriptions-item>
        <a-descriptions-item :label="$t('common.field.status')"><DictTag type="scm_price_adjust_status" :value="detail.status" /></a-descriptions-item>
        <a-descriptions-item :label="$t('scm.priceAdjust.effectiveTime')">{{ detail.effectiveTime || $t('scm.priceAdjust.immediate') }}</a-descriptions-item>
        <a-descriptions-item v-if="detail.target === PriceAdjustTarget.SCHEME" :label="$t('scm.priceAdjust.scheme')" :span="3">
          {{ (detail.schemeNames || []).join(', ') }}
        </a-descriptions-item>
        <a-descriptions-item :label="$t('scm.priceAdjust.createBy')">{{ detail.createByName }} {{ detail.createTime }}</a-descriptions-item>
        <a-descriptions-item :label="$t('scm.priceAdjust.approveTime')">{{ detail.approveTime || '-' }}</a-descriptions-item>
        <a-descriptions-item :label="$t('scm.priceAdjust.effectTime')">{{ detail.effectTime || '-' }}</a-descriptions-item>
        <a-descriptions-item v-if="detail.reason" :label="$t('scm.priceAdjust.reason')" :span="3">{{ detail.reason }}</a-descriptions-item>
      </a-descriptions>

      <a-tabs v-model:active-key="tab">
        <a-tab-pane key="items" :tab="$t('scm.priceAdjust.items')">
          <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="detail.items.length > 50 ? { pageSize: 50 } : false" :scroll="{ x: 760 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'material'">
                <div>{{ record.materialCode }} {{ record.materialName }}</div>
                <div class="sub-text">{{ record.spec }}</div>
              </template>
              <template v-else-if="column.key === 'hqRetailPrice'">{{ priceText(record.hqRetailPrice) }}</template>
              <template v-else-if="column.key === 'retailPrice'">{{ priceText(record.retailPrice) }}</template>
              <template v-else-if="column.key === 'memberPrice'">{{ priceText(record.memberPrice) }}</template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane v-if="detail.status === PriceAdjustStatus.EFFECTIVE" key="history" :tab="$t('scm.priceAdjust.changes')">
          <HistoryTable v-if="tab === 'history'" :adjust-id="detail.id" />
        </a-tab-pane>
      </a-tabs>
    </template>
  </SubPage>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import { deletePriceAdjust, getPriceAdjust } from '@/api/scm/priceAdjust'
import { useDict } from '@/composables/useDict'
import { price } from '@/utils/number'
import { PriceAdjustStatus, PriceAdjustTarget } from '@/constants/enums'
import HistoryTable from './HistoryTable.vue'
import { useAdjustActions } from './useAdjustActions'

/** 调价单详情：草稿可修改、提交、删除；审批中可撤回；待生效可作废；已生效时可查看各商品调整前后的价格 */
const emit = defineEmits(['edit', 'changed'])
const { t } = useI18n()
useDict('scm_price_adjust_status', 'scm_price_adjust_target')
const priceText = (v) => (v == null ? '-' : price(v))

const open = ref(false)
const detail = ref()
const tab = ref('items')
const itemColumns = computed(() => [
  { title: t('scm.priceAdjust.material'), key: 'material', width: 260 },
  { title: t('common.field.unit'), dataIndex: 'unit', width: 70 },
  { title: t('scm.priceAdjust.hqRetailNow'), key: 'hqRetailPrice', width: 120, align: 'right' },
  { title: t('scm.priceAdjust.newRetailPrice'), key: 'retailPrice', width: 120, align: 'right' },
  ...(detail.value?.target === PriceAdjustTarget.SCHEME ? [{ title: t('scm.priceAdjust.newMemberPrice'), key: 'memberPrice', width: 120, align: 'right' }] : []),
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true }
])

async function show(id) {
  detail.value = await getPriceAdjust(id)
  tab.value = 'items'
  open.value = true
}
const reload = () => {
  emit('changed')
  return detail.value ? show(detail.value.id) : undefined
}
const { submit, withdraw, voidAdjust, isDraft } = useAdjustActions(reload)
async function removeAndClose() {
  await deletePriceAdjust(detail.value.id)
  message.success(t('common.msg.deleteSuccess'))
  open.value = false
  emit('changed')
}

defineExpose({ open: show, reload, isOpen: (id) => open.value && detail.value?.id === id })
</script>

<style scoped>
.block {
  margin-bottom: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
