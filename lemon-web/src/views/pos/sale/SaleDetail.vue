<template>
  <SubPage :open="open" :title="detail ? $t('pos.sale.detailTitle', { no: detail.saleNo }) : ''" @close="open = false">
    <template v-if="detail" #extra>
      <a-space>
        <a-button @click="printReceipt(detail)">{{ $t('pos.sale.reprint') }}</a-button>
        <a-button v-if="voidable" v-perm="'pos:sale:void'" danger @click="voidIt">{{ $t('pos.sale.void') }}</a-button>
      </a-space>
    </template>
    <template v-if="detail">
      <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
        <a-descriptions-item :label="$t('pos.sale.kind')"><DictTag type="pos_sale_kind" :value="detail.kind" /></a-descriptions-item>
        <a-descriptions-item :label="$t('common.field.status')"><DictTag type="pos_sale_status" :value="detail.status" /></a-descriptions-item>
        <a-descriptions-item :label="$t('pos.sale.saleTime')">{{ detail.saleTime }}</a-descriptions-item>
        <a-descriptions-item :label="$t('store.common.store')">{{ detail.storeName }}</a-descriptions-item>
        <a-descriptions-item :label="$t('pos.sale.terminal')">{{ detail.terminalName }} · {{ detail.shiftNo }}</a-descriptions-item>
        <a-descriptions-item :label="$t('pos.sale.cashier')">{{ detail.cashierName }}</a-descriptions-item>
        <a-descriptions-item :label="$t('pos.sale.member')">{{ detail.memberCode ? `${detail.memberName || ''} ${detail.memberCode} ${detail.memberPhone || ''}` : $t('pos.sale.walkIn') }}</a-descriptions-item>
        <a-descriptions-item v-if="detail.origSaleNo" :label="$t('pos.sale.origSale')">{{ detail.origSaleNo }}</a-descriptions-item>
        <a-descriptions-item :label="$t('pos.sale.wmsDocNo')">{{ detail.wmsDocNo }}</a-descriptions-item>
        <a-descriptions-item :label="$t('pos.sale.listAmount')">{{ money(detail.listAmount) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('pos.sale.amount')">{{ money(detail.amount) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('pos.sale.orderDiscount')">{{ money(detail.orderDiscount) }} / {{ $t('pos.sale.roundOff') }} {{ money(detail.roundOff) }}</a-descriptions-item>
        <a-descriptions-item v-if="detail.promotionId" :label="$t('pos.sale.promotion')">
          {{ detail.promotionName }}<span v-if="Number(detail.promoAmount)"> −{{ money(detail.promoAmount) }}</span>
        </a-descriptions-item>
        <a-descriptions-item v-if="Number(detail.pointsEarned)" :label="$t('pos.sale.pointsEarned')">{{ qty(detail.pointsEarned) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('pos.sale.payable')"><b>{{ money(detail.payableAmount) }}</b></a-descriptions-item>
        <a-descriptions-item :label="$t('pos.sale.paid')">{{ money(detail.paidAmount) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('pos.sale.change')">{{ money(detail.changeAmount) }}</a-descriptions-item>
        <a-descriptions-item v-if="detail.voidTime" :label="$t('pos.sale.voidInfo')" :span="3">
          {{ detail.voidByName }} {{ detail.voidTime }} {{ detail.voidReason || '' }}
        </a-descriptions-item>
        <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')" :span="3">{{ detail.remark }}</a-descriptions-item>
      </a-descriptions>

      <div class="section-title">{{ $t('pos.sale.items') }}</div>
      <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="false" :scroll="{ x: 900 }" class="block">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">
            <div>{{ record.materialCode }} {{ record.materialName }} <a-tag v-if="record.gift" color="magenta">{{ $t('pos.sale.gift') }}</a-tag></div>
            <div class="sub-text">{{ record.spec }}</div>
          </template>
          <template v-else-if="column.key === 'qty'">{{ qty(record.inputQty) }} {{ record.unit }}</template>
          <template v-else-if="column.key === 'priceSource'">
            <DictTag type="pos_price_source" :value="record.priceSource" />
            <div v-if="record.specialName" class="sub-text">{{ record.specialName }}</div>
          </template>
          <template v-else-if="['listPrice', 'price', 'salePrice', 'amount', 'promoShare', 'orderShare', 'finalAmount'].includes(column.key)">{{ money(record[column.key]) }}</template>
          <template v-else-if="column.key === 'returned'">{{ Number(record.returnedQty) ? qty(record.returnedQty) : '-' }}</template>
        </template>
      </a-table>

      <div class="section-title">{{ $t('pos.sale.payments') }}</div>
      <a-table row-key="id" size="small" :columns="payColumns" :data-source="detail.payments" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'payMethod'"><DictTag type="pos_pay_method" :value="record.payMethod" /></template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="pos_payment_status" :value="record.status" />
            <div v-if="record.failReason" class="sub-text">{{ record.failReason }}</div>
          </template>
          <template v-else-if="column.key === 'amount'">
            {{ money(record.amount) }}
            <div v-if="record.points != null" class="sub-text">{{ $t('pos.sale.pointsN', { n: qty(record.points) }) }}</div>
          </template>
          <template v-else-if="column.key === 'tendered'">{{ record.tendered == null ? '-' : `${money(record.tendered)} / ${money(record.changeAmount)}` }}</template>
          <template v-else-if="column.key === 'action'">
            <a-button v-if="record.status === PaymentStatus.FAILED && record.amount < 0" type="link" size="small" @click="retry(record)">{{ $t('pos.sale.retryRefund') }}</a-button>
          </template>
        </template>
      </a-table>
    </template>
  </SubPage>
</template>

<script setup>
import { computed, h, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Input, Modal, message } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import { getSale, retryRefund, voidSale } from '@/api/pos'
import { money, qty } from '@/utils/number'
import { PaymentStatus, SaleKind, SaleStatus } from '@/constants/enums'
import { printReceipt } from '../printing'

/** POS 单据详情：明细（价格来源、已退数量）、收款 / 退款；当天的销售单可作废（需要作废权限，门店当天未日结） */
const emit = defineEmits(['changed'])
const { t } = useI18n()

const open = ref(false)
const detail = ref()
const today = () => new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10)
const voidable = computed(() => detail.value?.kind === SaleKind.SALE && detail.value?.status === SaleStatus.COMPLETED && detail.value?.businessDate === today())

const itemColumns = computed(() => [
  { title: t('pos.sale.material'), key: 'material', width: 240 },
  { title: t('common.field.quantity'), key: 'qty', width: 100, align: 'right' },
  { title: t('pos.sale.listPrice'), key: 'listPrice', width: 90, align: 'right' },
  { title: t('pos.sale.price'), key: 'price', width: 90, align: 'right' },
  { title: t('pos.sale.priceSource'), key: 'priceSource', width: 120 },
  { title: t('pos.sale.salePrice'), key: 'salePrice', width: 90, align: 'right' },
  { title: t('pos.sale.lineAmount'), key: 'amount', width: 100, align: 'right' },
  { title: t('pos.sale.promoShare'), key: 'promoShare', width: 90, align: 'right' },
  { title: t('pos.sale.orderShare'), key: 'orderShare', width: 90, align: 'right' },
  { title: t('pos.sale.finalAmount'), key: 'finalAmount', width: 100, align: 'right' },
  { title: t('pos.sale.returned'), key: 'returned', width: 80, align: 'right' }
])
const payColumns = computed(() => [
  { title: t('pos.sale.payMethod'), key: 'payMethod', width: 100 },
  { title: t('common.field.amount'), key: 'amount', width: 100, align: 'right' },
  { title: t('pos.sale.tendered'), key: 'tendered', width: 150, align: 'right' },
  { title: t('pos.sale.account'), dataIndex: 'accountName', width: 150, ellipsis: true },
  { title: t('common.field.status'), key: 'status', width: 140 },
  { title: t('pos.sale.tradeNo'), dataIndex: 'outTradeNo', width: 200, ellipsis: true },
  { title: t('pos.sale.refNo'), dataIndex: 'refNo', width: 120, ellipsis: true },
  { title: '', key: 'action', width: 90 }
])

async function show(id) {
  detail.value = await getSale(id)
  open.value = true
}
const reload = () => (detail.value ? show(detail.value.id) : undefined)

function voidIt() {
  let reason = ''
  Modal.confirm({
    title: t('pos.sale.voidTitle', { no: detail.value.saleNo }),
    content: () => h('div', [h('p', t('pos.sale.voidContent')), h(Input, { placeholder: t('pos.sale.voidReasonPh'), maxlength: 255, onChange: (e) => (reason = e.target.value) })]),
    okType: 'danger',
    onOk: async () => {
      await voidSale(detail.value.id, reason || null)
      message.success(t('common.msg.operationSuccess'))
      emit('changed')
      await reload()
    }
  })
}

async function retry(record) {
  const p = await retryRefund(record.id)
  message[p.status === PaymentStatus.SUCCESS ? 'success' : 'warning'](t(p.status === PaymentStatus.SUCCESS ? 'pos.sale.refundOk' : 'pos.sale.refundStillFailed'))
  await reload()
}

defineExpose({ open: show, reload })
</script>

<style scoped>
.block {
  margin-bottom: 12px;
}
.section-title {
  margin: 12px 0 8px;
  font-weight: 500;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
