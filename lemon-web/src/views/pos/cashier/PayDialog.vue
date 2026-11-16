<template>
  <a-modal
    v-model:open="visible"
    :title="$t('pos.pay.title')"
    :mask-closable="false"
    :keyboard="!paying"
    width="640px"
    :ok-text="$t('pos.pay.confirm')"
    :confirm-loading="saving"
    :ok-button-props="{ disabled: remaining > 0.004 || paying }"
    @ok="submit"
    @cancel="close"
  >
    <div class="pay-head">
      <div>
        <div class="label">{{ $t('pos.pay.payable') }}</div>
        <div class="big">{{ money(payable) }}</div>
      </div>
      <div>
        <div class="label">{{ remaining > 0 ? $t('pos.pay.remaining') : $t('pos.pay.change') }}</div>
        <div class="big" :class="{ change: remaining <= 0 }">{{ money(remaining > 0 ? remaining : change) }}</div>
      </div>
    </div>

    <div class="methods">
      <a-button
        v-for="(m, i) in methods"
        :key="m"
        :type="method === m ? 'primary' : 'default'"
        @click="selectMethod(m)"
      >
        <span class="fkey">{{ i + 1 }}</span>{{ dictLabel('pos_pay_method', m) }}
      </a-button>
    </div>

    <a-form layout="vertical" class="tender-form">
      <a-row :gutter="12">
        <a-col v-if="method === PayMethod.POINTS" :span="12">
          <a-form-item :label="$t('pos.pay.points')" :extra="$t('pos.pay.pointsTip', { have: qty(member?.points), rate: rule?.redeemPoints, max: money(maxRedeem) })">
            <a-input-number ref="amountRef" v-model:value="points" :min="0" :precision="0" class="full" size="large" @change="onPoints" @press-enter="addTender" />
          </a-form-item>
        </a-col>
        <a-col :span="method === PayMethod.CASH ? 24 : 12">
          <a-form-item :label="method === PayMethod.CASH ? $t('pos.pay.tendered') : $t('pos.pay.amount')"
                       :extra="method === PayMethod.STORED_VALUE ? $t('pos.pay.balanceTip', { balance: money(member?.balance) }) : ''">
            <a-input-number :ref="method === PayMethod.POINTS ? undefined : (el) => (amountRef = el)" v-model:value="amount" :min="0" :precision="2" class="full" size="large"
                            :disabled="method === PayMethod.POINTS" @press-enter="addTender" />
          </a-form-item>
        </a-col>
        <a-col v-if="isElectronic" :span="12">
          <a-form-item :label="$t('pos.pay.authCode')">
            <a-input v-model:value="authCode" size="large" :placeholder="$t('pos.pay.authCodePh')" @press-enter="addTender" />
          </a-form-item>
        </a-col>
        <a-col v-if="method === PayMethod.BANK_CARD" :span="12">
          <a-form-item :label="$t('pos.pay.refNo')">
            <a-input v-model:value="refNo" size="large" @press-enter="addTender" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-button type="dashed" block :loading="paying" @click="addTender">{{ $t('pos.pay.addTender') }} (Enter)</a-button>
    </a-form>

    <!-- 聚合支付处理中：线下登记需要收银员确认到账；扫码支付显示二维码内容 -->
    <a-alert v-if="pending" type="warning" show-icon class="pending">
      <template #message>
        {{ $t('pos.pay.pending', { method: dictLabel('pos_pay_method', pending.payMethod), amount: money(pending.amount) }) }}
        <div v-if="pending.qrCode" class="sub-text">{{ pending.qrCode }}</div>
      </template>
      <template #action>
        <a-space direction="vertical">
          <template v-if="ctx?.provider === PayProviderType.OFFLINE">
            <a-input v-model:value="offlineRef" size="small" :placeholder="$t('pos.pay.offlineRefPh')" />
            <a-button size="small" type="primary" @click="confirmOffline">{{ $t('pos.pay.confirmReceived') }}</a-button>
          </template>
          <a-button v-else size="small" @click="refreshPending">{{ $t('pos.pay.query') }}</a-button>
          <a-button size="small" danger @click="cancelPending">{{ $t('pos.pay.cancelPay') }}</a-button>
        </a-space>
      </template>
    </a-alert>

    <a-table row-key="key" size="small" :columns="columns" :data-source="tenders" :pagination="false" class="tenders">
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'method'">{{ dictLabel('pos_pay_method', record.payMethod) }}</template>
        <template v-else-if="column.key === 'amount'">{{ money(record.amount) }}</template>
        <template v-else-if="column.key === 'info'">
          <span class="sub-text">{{ record.points ? $t('pos.sale.pointsN', { n: record.points }) : record.outTradeNo || record.refNo || '' }}</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="text" danger size="small" @click="removeTender(index)"><DeleteOutlined /></a-button>
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Modal, message } from 'ant-design-vue'
import { DeleteOutlined } from '@ant-design/icons-vue'
import { cancelPay, checkoutSale, confirmPay, createPay, queryPay } from '@/api/pos'
import { money, qty, round2 } from '@/utils/number'
import { PayMethod, PayProviderType, PaymentStatus } from '@/constants/enums'
import { maxRedeemAmount } from '../marketing'
import { dictLabel, newTradeNo } from '../pos'

/**
 * 结算：多种支付方式组合。现金录入顾客付款（多于现金应收的部分找零）；微信、支付宝先经聚合支付收款（付款码扫顾客手机或生成二维码），
 * 成功后才加入付款；银行卡登记凭证号。储值、积分抵现需要识别会员：储值不超过余额（收银员向顾客确认后加入），
 * 积分抵现按「多少积分抵 1 元」折算（积分须为整数），不超过应收的抵现上限和会员积分（需要 pos:sale:redeem）。
 * 付足后结算，已收款的聚合支付在取消结算时提示撤销（原路退回）
 */
const props = defineProps({ terminalId: Number, ctx: Object, member: Object })
const emit = defineEmits(['paid', 'closed'])
const { t } = useI18n()

const visible = ref(false)
const saving = ref(false)
const paying = ref(false)
const payable = ref(0)
const body = ref({})
const tenders = ref([])
const method = ref(PayMethod.CASH)
const amount = ref()
const authCode = ref('')
const refNo = ref('')
const pending = ref(null)
const offlineRef = ref('')
const amountRef = ref()

const points = ref()
const rule = computed(() => props.ctx?.memberRule)
const canRedeem = computed(() => (props.ctx?.perms || []).includes('pos:sale:redeem'))
/** 储值、积分抵现只在识别了会员时可选，积分抵现还需要权限 */
const methods = computed(() => (props.ctx?.payMethods || [PayMethod.CASH]).filter((m) => {
  if (m === PayMethod.STORED_VALUE) return !!props.member
  if (m === PayMethod.POINTS) return !!props.member && canRedeem.value
  return true
}))
const usedPoints = computed(() => tenders.value.filter((x) => x.payMethod === PayMethod.POINTS).reduce((s, x) => s + Number(x.points || 0), 0))
const usedBalance = computed(() => tenders.value.filter((x) => x.payMethod === PayMethod.STORED_VALUE).reduce((s, x) => s + Number(x.amount), 0))
const maxRedeem = computed(() => {
  const usedAmount = tenders.value.filter((x) => x.payMethod === PayMethod.POINTS).reduce((s, x) => s + Number(x.amount), 0)
  const byRule = maxRedeemAmount(rule.value, payable.value, Number(props.member?.points || 0) - usedPoints.value, remaining.value)
  const byRate = rule.value ? Math.floor(Number(payable.value) * Number(rule.value.redeemMaxRate) + 1e-6) / 100 - usedAmount : 0
  return Math.max(0, round2(Math.min(byRule, byRate)))
})
const isElectronic = computed(() => method.value === PayMethod.WECHAT || method.value === PayMethod.ALIPAY)
const nonCash = computed(() => tenders.value.filter((x) => x.payMethod !== PayMethod.CASH).reduce((s, x) => s + Number(x.amount), 0))
const cash = computed(() => tenders.value.filter((x) => x.payMethod === PayMethod.CASH).reduce((s, x) => s + Number(x.amount), 0))
const remaining = computed(() => round2(Number(payable.value) - nonCash.value - cash.value))
const change = computed(() => round2(cash.value - Math.max(0, Number(payable.value) - nonCash.value)))

const columns = computed(() => [
  { title: t('pos.pay.method'), key: 'method', width: 120 },
  { title: t('pos.pay.amount'), key: 'amount', width: 120, align: 'right' },
  { title: t('pos.pay.info'), key: 'info' },
  { title: '', key: 'action', width: 50 }
])

function open(due, payload) {
  payable.value = Number(due)
  body.value = payload
  tenders.value = []
  pending.value = null
  selectMethod(PayMethod.CASH)
  visible.value = true
  window.addEventListener('keydown', onKeydown)
}

function selectMethod(m) {
  method.value = m
  amount.value = remaining.value > 0 ? remaining.value : undefined
  points.value = undefined
  if (m === PayMethod.STORED_VALUE) {
    amount.value = Math.max(0, round2(Math.min(remaining.value, Number(props.member?.balance || 0) - usedBalance.value))) || undefined
  } else if (m === PayMethod.POINTS) {
    // 默认可抵的最大积分（按整数积分折算）
    const rate = Number(rule.value?.redeemPoints || 1)
    points.value = Math.floor(maxRedeem.value * rate + 1e-6) || undefined
    onPoints()
  }
  authCode.value = ''
  refNo.value = ''
  nextTick(() => amountRef.value?.focus())
}

/** 积分 → 抵现金额（积分 ÷ 每元积分，保留 2 位；不能整除到分的积分无效） */
function onPoints() {
  const rate = Number(rule.value?.redeemPoints || 1)
  amount.value = points.value ? round2(Number(points.value) / rate) : undefined
}

async function addTender() {
  const value = Number(amount.value || 0)
  if (value <= 0 || paying.value) return
  if (method.value !== PayMethod.CASH && value > remaining.value + 0.004) {
    message.warning(t('pos.pay.overRemaining'))
    return
  }
  if (method.value === PayMethod.STORED_VALUE) {
    if (value > Number(props.member?.balance || 0) - usedBalance.value + 0.004) {
      message.warning(t('pos.pay.balanceInsufficient'))
      return
    }
    // 收银员向顾客确认使用储值
    Modal.confirm({
      title: t('pos.pay.storedValueConfirm', { amount: money(value) }),
      content: t('pos.pay.storedValueConfirmContent', { name: props.member?.name || props.member?.memberCode, balance: money(props.member?.balance) }),
      onOk: () => {
        tenders.value.push({ key: `s${Date.now()}`, payMethod: PayMethod.STORED_VALUE, amount: value })
        selectMethod(PayMethod.CASH)
      }
    })
    return
  }
  if (method.value === PayMethod.POINTS) {
    const rate = Number(rule.value?.redeemPoints || 1)
    if (Math.abs(value * rate - Number(points.value)) > 1e-6) {
      message.warning(t('pos.pay.pointsNotInteger', { rate }))
      return
    }
    if (value > maxRedeem.value + 0.004) {
      message.warning(t('pos.pay.redeemExceeded', { max: money(maxRedeem.value) }))
      return
    }
    tenders.value.push({ key: `p${Date.now()}`, payMethod: PayMethod.POINTS, amount: value, points: Number(points.value) })
    selectMethod(PayMethod.CASH)
    return
  }
  if (method.value === PayMethod.CASH) {
    const existing = tenders.value.find((x) => x.payMethod === PayMethod.CASH)
    if (existing) existing.amount = round2(Number(existing.amount) + value)
    else tenders.value.push({ key: `c${Date.now()}`, payMethod: PayMethod.CASH, amount: value })
  } else if (isElectronic.value) {
    paying.value = true
    try {
      const p = await createPay({ terminalId: props.terminalId, payMethod: method.value, amount: value, authCode: authCode.value || null, outTradeNo: newTradeNo() })
      handlePayment(p)
    } finally {
      paying.value = false
    }
  } else {
    tenders.value.push({ key: `b${Date.now()}`, payMethod: method.value, amount: value, refNo: refNo.value || null })
  }
  selectMethod(remaining.value > 0 ? method.value : PayMethod.CASH)
}

function handlePayment(p) {
  if (p.status === PaymentStatus.SUCCESS) {
    pending.value = null
    tenders.value.push({ key: p.outTradeNo, payMethod: p.payMethod, amount: Number(p.amount), outTradeNo: p.outTradeNo })
  } else if (p.status === PaymentStatus.PENDING) {
    pending.value = p
  } else {
    pending.value = null
    message.error(t('pos.pay.failed', { reason: p.failReason || '' }))
  }
}

async function confirmOffline() {
  handlePayment(await confirmPay(pending.value.outTradeNo, offlineRef.value || null))
  offlineRef.value = ''
}

async function refreshPending() {
  handlePayment(await queryPay(pending.value.outTradeNo))
}

async function cancelPending() {
  await cancelPay(pending.value.outTradeNo)
  pending.value = null
}

function removeTender(index) {
  const x = tenders.value[index]
  if (x.outTradeNo) {
    // 已收款的聚合支付：撤销（原路退回）
    Modal.confirm({
      title: t('pos.pay.cancelPaidTitle'),
      content: t('pos.pay.cancelPaidContent', { amount: money(x.amount) }),
      onOk: async () => {
        await cancelPay(x.outTradeNo)
        tenders.value.splice(index, 1)
      }
    })
    return
  }
  tenders.value.splice(index, 1)
}

async function submit() {
  if (remaining.value > 0.004) return
  saving.value = true
  try {
    const sale = await checkoutSale({
      ...body.value,
      tenders: tenders.value.map((x) => ({ payMethod: x.payMethod, amount: x.amount, outTradeNo: x.outTradeNo || null, refNo: x.refNo || null }))
    })
    message.success(t('pos.pay.success', { no: sale.saleNo, change: money(sale.changeAmount) }))
    visible.value = false
    window.removeEventListener('keydown', onKeydown)
    emit('paid', sale)
  } finally {
    saving.value = false
  }
}

function close() {
  const paid = tenders.value.filter((x) => x.outTradeNo)
  if (paid.length) {
    message.warning(t('pos.pay.paidNotSettled'))
    visible.value = true
    return
  }
  visible.value = false
  window.removeEventListener('keydown', onKeydown)
  emit('closed')
}

/** 数字键 1–5 切换支付方式（金额输入框外） */
function onKeydown(e) {
  if (!visible.value) return
  const n = Number(e.key)
  const inInput = document.activeElement?.tagName === 'INPUT'
  if (!inInput && n >= 1 && n <= methods.value.length) {
    e.preventDefault()
    selectMethod(methods.value[n - 1])
  } else if (e.key === 'F12' && remaining.value <= 0.004) {
    e.preventDefault()
    submit()
  }
}

defineExpose({ open })
</script>

<style scoped>
.pay-head {
  display: flex;
  justify-content: space-around;
  margin-bottom: 12px;
  text-align: center;
}
.label {
  color: var(--app-text-3);
}
.big {
  font-size: 28px;
  font-weight: 700;
}
.big.change {
  color: #389e0d;
}
.methods {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.fkey {
  margin-right: 4px;
  font-size: 11px;
  opacity: 0.65;
}
.full {
  width: 100%;
}
.tender-form {
  margin-bottom: 12px;
}
.pending {
  margin-bottom: 12px;
}
.tenders {
  margin-top: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
