<template>
  <div>
    <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered>
      <a-descriptions-item :label="$t('pos.shift.terminal')">{{ report.shift.terminalName }}</a-descriptions-item>
      <a-descriptions-item :label="$t('pos.shift.cashier')">{{ report.shift.cashierName }}</a-descriptions-item>
      <a-descriptions-item :label="$t('common.field.status')"><DictTag type="pos_shift_status" :value="report.shift.status" /></a-descriptions-item>
      <a-descriptions-item :label="$t('pos.shift.openTime')">{{ report.shift.openTime }}</a-descriptions-item>
      <a-descriptions-item :label="$t('pos.shift.closeTime')">{{ report.shift.closeTime || '-' }}</a-descriptions-item>
      <a-descriptions-item :label="$t('pos.shift.openingCash')">{{ money(report.shift.openingCash) }}</a-descriptions-item>
      <a-descriptions-item :label="$t('pos.shift.sales')">{{ report.sales?.saleCount ?? 0 }} / {{ money(report.sales?.saleAmount) }}</a-descriptions-item>
      <a-descriptions-item :label="$t('pos.shift.returns')">{{ report.sales?.returnCount ?? 0 }} / {{ money(report.sales?.returnAmount) }}</a-descriptions-item>
      <a-descriptions-item :label="$t('pos.shift.expectedCash')">
        <b>{{ money(report.expectedCash) }}</b>
      </a-descriptions-item>
      <template v-if="report.shift.countedCash != null">
        <a-descriptions-item :label="$t('pos.shift.countedCash')">{{ money(report.shift.countedCash) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('pos.shift.cashDiff')">
          <span :class="{ neg: report.shift.cashDiff < 0 }">{{ money(report.shift.cashDiff) }}</span>
        </a-descriptions-item>
        <a-descriptions-item :label="$t('pos.shift.closedBy')">{{ report.shift.closedByName || '-' }}</a-descriptions-item>
      </template>
    </a-descriptions>
    <a-table row-key="payMethod" size="small" :columns="columns" :data-source="report.pays" :pagination="false" class="pays">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'payMethod'"><DictTag type="pos_pay_method" :value="record.payMethod" /></template>
        <template v-else-if="['receiptAmount', 'refundAmount', 'amount'].includes(column.key)">{{ money(record[column.key]) }}</template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { money } from '@/utils/number'

/** 交班单内容：班次信息与按支付方式 × 结算账户汇总的收款、退款 */
defineProps({ report: { type: Object, required: true } })
const { t } = useI18n()
useDict('pos_shift_status', 'pos_pay_method')

const columns = computed(() => [
  { title: t('pos.shift.payMethod'), key: 'payMethod', width: 100 },
  { title: t('pos.shift.account'), dataIndex: 'accountName', ellipsis: true },
  { title: t('pos.shift.receipts'), key: 'receiptAmount', width: 100, align: 'right' },
  { title: t('pos.shift.refunds'), key: 'refundAmount', width: 100, align: 'right' },
  { title: t('pos.shift.net'), key: 'amount', width: 100, align: 'right' },
  { title: t('pos.shift.count'), dataIndex: 'payCount', width: 70, align: 'right' }
])
</script>

<style scoped>
.pays {
  margin-top: 12px;
}
.neg {
  color: #d4380d;
}
</style>
