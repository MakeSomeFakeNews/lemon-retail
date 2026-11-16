<template>
  <div>
    <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="gap">
      <a-descriptions-item :label="$t('pos.daily.saleCount')">{{ sales?.saleCount ?? 0 }}</a-descriptions-item>
      <a-descriptions-item :label="$t('pos.daily.saleAmount')">{{ money(sales?.saleAmount) }}</a-descriptions-item>
      <a-descriptions-item :label="$t('pos.daily.returnAmount')">{{ money(sales?.returnAmount) }} ({{ sales?.returnCount ?? 0 }})</a-descriptions-item>
      <a-descriptions-item :label="$t('pos.daily.netAmount')"><b>{{ money(netAmount) }}</b></a-descriptions-item>
      <a-descriptions-item :label="$t('pos.daily.shiftCount')">{{ shiftCount }}</a-descriptions-item>
      <a-descriptions-item :label="$t('pos.daily.cash')">{{ money(countedCash) }} / {{ money(expectedCash) }}</a-descriptions-item>
      <a-descriptions-item :label="$t('pos.daily.cashDiff')"><span :class="{ neg: cashDiff < 0 }">{{ money(cashDiff) }}</span></a-descriptions-item>
    </a-descriptions>
    <a-table :row-key="(r) => `${r.payMethod}-${r.accountId}`" size="small" :columns="columns" :data-source="pays || []" :pagination="false" class="gap">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'payMethod'"><DictTag type="pos_pay_method" :value="record.payMethod" /></template>
        <template v-else-if="['receiptAmount', 'refundAmount'].includes(column.key)">{{ money(record[column.key]) }}</template>
      </template>
    </a-table>
    <template v-if="member">
      <div class="section-title gap">{{ $t('pos.daily.memberSection') }}</div>
      <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered>
        <a-descriptions-item :label="$t('pos.daily.topupAmount')">{{ money(member.topupAmount) }} ({{ member.topupCount ?? 0 }})</a-descriptions-item>
        <a-descriptions-item :label="$t('pos.daily.bonusAmount')">{{ money(member.bonusAmount) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('pos.daily.storedValueAmount')">{{ money(member.storedValueAmount) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('pos.daily.pointsAmount')">{{ money(member.pointsAmount) }}</a-descriptions-item>
        <a-descriptions-item v-if="member.voucherId !== undefined" :label="$t('pos.daily.voucher')">
          {{ member.voucherId ? $t('pos.daily.voucherGenerated') : '-' }}
        </a-descriptions-item>
      </a-descriptions>
      <a-table v-if="member.topupPays?.length" :row-key="(r) => `${r.payMethod}-${r.accountId}`" size="small" :columns="columns" :data-source="member.topupPays" :pagination="false" class="gap">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'payMethod'"><DictTag type="pos_pay_method" :value="record.payMethod" /></template>
          <template v-else-if="['receiptAmount', 'refundAmount'].includes(column.key)">{{ money(record[column.key]) }}</template>
        </template>
      </a-table>
      <div class="sub-text gap">{{ $t('pos.daily.memberTip') }}</div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import { money } from '@/utils/number'

/**
 * 日结汇总：单据、各支付方式 × 结算账户的收款与退款、现金（实点 / 应有）与差额；
 * member 为储值与积分（充值收款、赠送、本店储值消费净额、积分抵现、充值按账户的收款、是否生成储值凭证）
 */
defineProps({
  sales: Object,
  pays: Array,
  netAmount: [Number, String],
  shiftCount: Number,
  expectedCash: [Number, String],
  countedCash: [Number, String],
  cashDiff: [Number, String],
  member: Object
})
const { t } = useI18n()
const columns = computed(() => [
  { title: t('pos.shift.payMethod'), key: 'payMethod', width: 100 },
  { title: t('pos.shift.account'), dataIndex: 'accountName', ellipsis: true },
  { title: t('pos.shift.receipts'), key: 'receiptAmount', width: 110, align: 'right' },
  { title: t('pos.shift.refunds'), key: 'refundAmount', width: 110, align: 'right' },
  { title: t('pos.shift.count'), dataIndex: 'payCount', width: 70, align: 'right' }
])
</script>

<style scoped>
.gap {
  margin-top: 12px;
}
.neg {
  color: #d4380d;
}
.section-title {
  font-weight: 500;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
