<template>
  <a-modal v-model:open="visible" :title="$t('pos.returnDialog.title')" width="760px" :confirm-loading="saving" :ok-text="$t('pos.returnDialog.confirm')" @ok="submit" @cancel="emit('closed')">
    <a-radio-group v-model:value="mode" class="mode" @change="resetLines">
      <a-radio-button value="receipt" :disabled="!canReceipt">{{ $t('pos.returnDialog.withReceipt') }}</a-radio-button>
      <a-radio-button value="none" :disabled="!canNoReceipt">{{ $t('pos.returnDialog.withoutReceipt') }}</a-radio-button>
    </a-radio-group>

    <template v-if="mode === 'receipt'">
      <a-input-search ref="noRef" v-model:value="saleNo" :placeholder="$t('pos.returnDialog.saleNoPh')" :enter-button="$t('pos.memberModal.find')" @search="findSale" />
      <div v-if="orig" class="orig">
        <span>{{ orig.saleNo }} · {{ orig.saleTime }} · {{ money(orig.payableAmount) }}</span>
        <span class="sub-text">{{ (orig.payments || []).map((p) => dictLabel('pos_pay_method', p.payMethod)).join(' / ') }}</span>
      </div>
    </template>
    <template v-else>
      <a-row :gutter="8">
        <a-col :span="14">
          <a-input ref="codeRef" v-model:value="code" :placeholder="$t('pos.cashier.scanPh')" @press-enter="addGoods" />
        </a-col>
        <a-col :span="10">
          <a-select v-model:value="refundMethod" class="full" :options="refundOptions" />
        </a-col>
      </a-row>
    </template>

    <a-table row-key="key" size="small" :columns="columns" :data-source="lines" :pagination="false" class="lines">
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'name'">
          <div>{{ record.name }}</div>
          <div class="sub-text">{{ record.code }}</div>
        </template>
        <template v-else-if="column.key === 'returnable'">{{ record.returnable == null ? '-' : qty(record.returnable) }}</template>
        <template v-else-if="column.key === 'qty'">
          <a-input-number v-model:value="record.qty" :min="0" :max="record.returnable ?? undefined" :precision="4" size="small" />
        </template>
        <template v-else-if="column.key === 'price'">
          <a-input-number v-if="mode === 'none'" v-model:value="record.price" :min="0" :precision="2" size="small" />
          <span v-else>{{ money(record.price) }}</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button v-if="mode === 'none'" type="text" danger size="small" @click="lines.splice(index, 1)"><DeleteOutlined /></a-button>
        </template>
      </template>
    </a-table>
    <div class="total">{{ $t('pos.returnDialog.estimated') }} <b>{{ money(estimate) }}</b></div>
  </a-modal>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { DeleteOutlined } from '@ant-design/icons-vue'
import { lookupGoods, returnSale, saleByNo } from '@/api/pos'
import { money, qty, round2 } from '@/utils/number'
import { PayMethod } from '@/constants/enums'
import { dictLabel } from '../pos'

/**
 * 退货：原单退货（扫小票号，按原销售行退，可部分退，退款原路退回）；无原单退货（需要权限，扫码加商品，退现金或银行卡）
 */
const props = defineProps({ terminalId: Number, ctx: Object })
const emit = defineEmits(['done', 'closed'])
const { t } = useI18n()

const visible = ref(false)
const saving = ref(false)
const mode = ref('receipt')
const saleNo = ref('')
const orig = ref(null)
const code = ref('')
const lines = ref([])
const refundMethod = ref(PayMethod.CASH)
const noRef = ref()
const codeRef = ref()

const canReceipt = computed(() => (props.ctx?.perms || []).includes('pos:sale:return'))
const canNoReceipt = computed(() => (props.ctx?.perms || []).includes('pos:sale:noReceiptReturn'))
const refundOptions = computed(() => [PayMethod.CASH, PayMethod.BANK_CARD].map((m) => ({ value: m, label: dictLabel('pos_pay_method', m) })))
const estimate = computed(() => lines.value.reduce((s, l) => s + Number(l.qty || 0) * Number(l.unitRefund ?? l.price ?? 0), 0))

const columns = computed(() => [
  { title: t('pos.cashier.goods'), key: 'name' },
  { title: t('common.field.unit'), dataIndex: 'unit', width: 60 },
  { title: t('pos.returnDialog.returnable'), key: 'returnable', width: 90, align: 'right' },
  { title: t('pos.returnDialog.qty'), key: 'qty', width: 110 },
  { title: t('pos.cashier.price'), key: 'price', width: 110, align: 'right' },
  { title: '', key: 'action', width: 46 }
])

function open() {
  mode.value = canReceipt.value ? 'receipt' : 'none'
  saleNo.value = ''
  refundMethod.value = PayMethod.CASH
  resetLines()
  visible.value = true
  nextTick(() => (mode.value === 'receipt' ? noRef.value : codeRef.value)?.focus())
}

function resetLines() {
  orig.value = null
  lines.value = []
}

async function findSale() {
  if (!saleNo.value?.trim()) return
  const s = await saleByNo(saleNo.value.trim())
  orig.value = s
  lines.value = (s.items || []).map((i) => ({
    key: i.id,
    origItemId: i.id,
    materialId: i.materialId,
    code: i.materialCode,
    name: i.materialName,
    unit: i.unit,
    returnable: Number(i.inputQty) - Number(i.returnedQty || 0),
    qty: 0,
    price: i.salePrice,
    // 按实收（分摊整单优惠后）估算退款，以后端计算为准
    unitRefund: Number(i.inputQty) ? round2(Number(i.finalAmount) / Number(i.inputQty)) : 0
  }))
}

async function addGoods() {
  const c = code.value?.trim()
  if (!c) return
  code.value = ''
  let g
  try {
    g = await lookupGoods(props.terminalId, c)
  } catch {
    return // 错误提示由请求拦截器处理
  }
  lines.value.push({ key: `${g.materialId}-${Date.now()}`, materialId: g.materialId, code: g.code, name: g.name, unit: g.unit, returnable: null, qty: 1, price: g.price })
}

async function submit() {
  const picked = lines.value.filter((l) => Number(l.qty) > 0)
  if (!picked.length) {
    message.warning(t('pos.returnDialog.noLines'))
    return
  }
  saving.value = true
  try {
    const sale = await returnSale({
      terminalId: props.terminalId,
      origSaleId: mode.value === 'receipt' ? orig.value?.id : null,
      refundMethod: mode.value === 'none' ? refundMethod.value : null,
      lines: picked.map((l) =>
        mode.value === 'receipt'
          ? { origItemId: l.origItemId, qty: l.qty }
          : { materialId: l.materialId, unit: l.unit, qty: l.qty, price: l.price }
      )
    })
    message.success(t('pos.returnDialog.success', { no: sale.saleNo, amount: money(sale.payableAmount) }))
    visible.value = false
    emit('done', sale)
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.mode {
  margin-bottom: 12px;
}
.orig {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
}
.lines {
  margin-top: 12px;
}
.total {
  margin-top: 12px;
  text-align: right;
}
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
