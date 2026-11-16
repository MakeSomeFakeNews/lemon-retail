<template>
  <div class="pos-screen" @click="refocus">
    <!-- 顶栏：门店、收银台、班次、操作 -->
    <header class="pos-header">
      <div class="pos-brand">
        <ShoppingCartOutlined />
        <span class="pos-store">{{ ctx?.storeName || $t('pos.cashier.title') }}</span>
        <a-select
          v-model:value="terminalId"
          class="pos-terminal"
          size="small"
          :options="terminalOptions"
          :placeholder="$t('pos.cashier.selectTerminal')"
          @change="loadContext"
        />
      </div>
      <div class="pos-shift">
        <template v-if="shift">
          <a-tag color="green">{{ $t('pos.cashier.onDuty') }}</a-tag>
          <span>{{ shift.shiftNo }} · {{ shift.cashierName }} · {{ shift.openTime }}</span>
          <a-button size="small" @click.stop="shiftRef.openClose(shift)">{{ $t('pos.cashier.closeShift') }}</a-button>
        </template>
        <template v-else-if="ctx">
          <a-tag>{{ $t('pos.cashier.noShift') }}</a-tag>
          <a-button size="small" type="primary" @click.stop="shiftRef.openOpen(terminalId)">{{ $t('pos.cashier.openShift') }}</a-button>
        </template>
        <a-button size="small" @click.stop="exit">{{ $t('pos.cashier.exit') }}</a-button>
      </div>
    </header>

    <main class="pos-body">
      <!-- 左侧：扫码与购物车 -->
      <section class="pos-cart">
        <div class="pos-scan">
          <a-input
            ref="scanRef"
            v-model:value="scan"
            size="large"
            :placeholder="$t('pos.cashier.scanPh')"
            :disabled="!shift"
            allow-clear
            @press-enter="onScan"
          >
            <template #prefix><BarcodeOutlined /></template>
          </a-input>
        </div>
        <a-table
          class="pos-lines"
          row-key="key"
          size="small"
          :columns="columns"
          :data-source="lines"
          :pagination="false"
          :scroll="{ y: 'calc(100vh - 230px)' }"
          :row-class-name="(r, i) => (i === current ? 'pos-row-current' : '')"
          :custom-row="(r, i) => ({ onClick: () => (current = i) })"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'index'">{{ index + 1 }}</template>
            <template v-else-if="column.key === 'name'">
              <div>{{ record.name }}</div>
              <div class="sub-text">{{ record.code }} {{ record.spec || '' }}</div>
            </template>
            <template v-else-if="column.key === 'qty'">
              <a-input-number
                :ref="(el) => (qtyRefs[index] = el)"
                v-model:value="record.qty"
                :min="0.0001"
                :precision="4"
                size="small"
                class="qty-input"
                @change="schedulePreview"
                @press-enter="refocus"
              />
            </template>
            <template v-else-if="column.key === 'price'">
              <span>{{ money(record.price) }}</span>
              <a-tag v-if="record.priceSource" :color="sourceColor(record.priceSource)" class="src-tag">{{ dictLabel('pos_price_source', record.priceSource) }}</a-tag>
              <div v-if="record.discountRate && record.discountRate < 100" class="sub-text">{{ $t('pos.cashier.discountN', { rate: record.discountRate }) }}</div>
            </template>
            <template v-else-if="column.key === 'amount'">
              <span :class="{ 'below-min': record.belowMin }">{{ money(record.amount) }}</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="text" danger size="small" @click.stop="removeLine(index)"><DeleteOutlined /></a-button>
            </template>
          </template>
        </a-table>
      </section>

      <!-- 右侧：会员、合计、功能键 -->
      <aside class="pos-side">
        <div class="pos-member">
          <template v-if="member">
            <div class="member-name">
              <IdcardOutlined /> {{ member.name || member.memberCode }}
              <a-tag color="gold">{{ member.levelName || $t('pos.cashier.member') }}</a-tag>
              <a-button type="link" size="small" @click.stop="setMember(null)">{{ $t('common.action.clear') }}</a-button>
            </div>
            <div class="sub-text">{{ member.memberCode }} · {{ member.phone }}</div>
          </template>
          <a-button v-else block @click.stop="memberRef.open()">{{ $t('pos.cashier.identifyMember') }} (F3)</a-button>
        </div>

        <div class="pos-totals">
          <div class="row"><span>{{ $t('pos.cashier.itemCount') }}</span><span>{{ lines.length }} / {{ qty(totalQty) }}</span></div>
          <div class="row"><span>{{ $t('pos.cashier.listAmount') }}</span><span>{{ money(preview?.listAmount ?? localAmount) }}</span></div>
          <div class="row"><span>{{ $t('pos.cashier.amount') }}</span><span>{{ money(preview?.amount ?? localAmount) }}</span></div>
          <div class="row">
            <span>{{ $t('pos.cashier.orderDiscount') }}</span>
            <a-input-number v-model:value="orderDiscount" :min="0" :precision="2" size="small" :disabled="!can('pos:sale:discount')" @change="schedulePreview" />
          </div>
          <div v-if="preview?.roundOff" class="row"><span>{{ $t('pos.cashier.roundOff') }}</span><span>{{ money(preview.roundOff) }}</span></div>
          <div class="row payable"><span>{{ $t('pos.cashier.payable') }}</span><span>{{ money(payable) }}</span></div>
          <a-alert v-if="previewError" type="error" :message="previewError" show-icon class="preview-error" />
        </div>

        <div class="pos-keys">
          <a-button v-for="k in keys" :key="k.key" :type="k.primary ? 'primary' : 'default'" :danger="k.danger" :disabled="k.disabled" @click.stop="k.run">
            <span class="fkey">{{ k.key }}</span>{{ k.label }}
          </a-button>
        </div>
      </aside>
    </main>

    <PayDialog ref="payRef" :terminal-id="terminalId" :ctx="ctx" @paid="onPaid" @closed="refocus" />
    <ShiftModal ref="shiftRef" @changed="loadContext" @closed="refocus" />
    <HeldDrawer ref="heldRef" :terminal-id="terminalId" @resume="restore" @closed="refocus" />
    <MemberModal ref="memberRef" :terminal-id="terminalId" :can-register="can('pos:member:add') || !!ctx" @select="setMember" @closed="refocus" />
    <LineEditModal ref="lineRef" :can-price="can('pos:sale:price')" :can-discount="can('pos:sale:discount')" @saved="schedulePreview" @closed="refocus" />
    <ReturnDialog ref="returnRef" :terminal-id="terminalId" :ctx="ctx" @done="onReturned" @closed="refocus" />
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { BarcodeOutlined, DeleteOutlined, IdcardOutlined, ShoppingCartOutlined } from '@ant-design/icons-vue'
import { cashierContext, holdSale, lookupGoods, previewSale, terminalOptions as fetchTerminals } from '@/api/pos'
import { useDict } from '@/composables/useDict'
import { load as loadLocal, save as saveLocal } from '@/utils/storage'
import { money, qty } from '@/utils/number'
import { PosPriceSource } from '@/constants/enums'
import { POS_DICTS, dictLabel } from '../pos'
import { printReceipt } from '../printing'
import HeldDrawer from './HeldDrawer.vue'
import LineEditModal from './LineEditModal.vue'
import MemberModal from './MemberModal.vue'
import PayDialog from './PayDialog.vue'
import ReturnDialog from './ReturnDialog.vue'
import ShiftModal from './ShiftModal.vue'

/**
 * 收银台（全屏、键盘优先）：扫码框始终获得焦点，扫码枪输入后回车加入购物车（同一商品同一单位累加数量）；
 * 每次修改后向后端试算（取价顺序、限购、折扣上限、抹零以后端为准），结算时选择多种支付方式、计算找零，结算后打印小票。
 * 功能键：F2 数量、F3 会员、F4 挂单、F5 取单、F6 删除行、F7 改价 / 折扣、F8 整单优惠、F9 退货、F12 结算（扫码框为空时回车也结算）、Esc 关闭弹窗
 */
const { t } = useI18n()
const router = useRouter()
useDict(...POS_DICTS)
const TERMINAL_KEY = 'pos.terminalId'

const terminals = ref([])
const terminalOptions = computed(() => terminals.value.map((x) => ({ value: x.id, label: `${x.name}（${x.storeName}）` })))
const terminalId = ref(loadLocal(TERMINAL_KEY, undefined))
const ctx = ref()
const shift = computed(() => ctx.value?.shift)
const can = (perm) => (ctx.value?.perms || []).includes(perm)

const scanRef = ref()
const scan = ref('')
const lines = ref([])
const current = ref(-1)
const qtyRefs = ref([])
const member = ref(null)
const orderDiscount = ref(null)
const heldId = ref(null)
const preview = ref(null)
const previewError = ref('')

const payRef = ref()
const shiftRef = ref()
const heldRef = ref()
const memberRef = ref()
const lineRef = ref()
const returnRef = ref()

const columns = computed(() => [
  { title: '#', key: 'index', width: 46 },
  { title: t('pos.cashier.goods'), key: 'name' },
  { title: t('common.field.unit'), dataIndex: 'unit', width: 70 },
  { title: t('common.field.quantity'), key: 'qty', width: 120 },
  { title: t('pos.cashier.price'), key: 'price', width: 150, align: 'right' },
  { title: t('pos.cashier.lineAmount'), key: 'amount', width: 110, align: 'right' },
  { title: '', key: 'action', width: 46 }
])

const totalQty = computed(() => lines.value.reduce((s, l) => s + Number(l.qty || 0), 0))
const localAmount = computed(() => lines.value.reduce((s, l) => s + Number(l.amount || 0), 0))
const payable = computed(() => (preview.value ? Number(preview.value.payable) : Math.max(0, localAmount.value - Number(orderDiscount.value || 0))))

const sourceColor = (s) => ({ [PosPriceSource.SPECIAL]: 'red', [PosPriceSource.MEMBER]: 'gold', [PosPriceSource.MANUAL]: 'purple' })[s] || 'default'

const keys = computed(() => [
  { key: 'F2', label: t('pos.cashier.keyQty'), run: focusQty, disabled: current.value < 0 },
  { key: 'F3', label: t('pos.cashier.keyMember'), run: () => memberRef.value.open() },
  { key: 'F4', label: t('pos.cashier.keyHold'), run: hold, disabled: !lines.value.length },
  { key: 'F5', label: t('pos.cashier.keyResume'), run: () => heldRef.value.open(), disabled: !ctx.value },
  { key: 'F6', label: t('pos.cashier.keyDelete'), run: () => removeLine(current.value), disabled: current.value < 0, danger: true },
  { key: 'F7', label: t('pos.cashier.keyEdit'), run: editLine, disabled: current.value < 0 || !(can('pos:sale:price') || can('pos:sale:discount')) },
  { key: 'F8', label: t('pos.cashier.keyOrderDiscount'), run: focusOrderDiscount, disabled: !can('pos:sale:discount') },
  { key: 'F9', label: t('pos.cashier.keyReturn'), run: () => returnRef.value.open(), disabled: !shift.value || !(can('pos:sale:return') || can('pos:sale:noReceiptReturn')) },
  { key: 'F12', label: t('pos.cashier.keyCheckout'), run: checkout, disabled: !lines.value.length || !shift.value, primary: true }
])

onMounted(async () => {
  window.addEventListener('keydown', onKeydown)
  terminals.value = await fetchTerminals().catch(() => [])
  if (!terminals.value.some((x) => x.id === terminalId.value)) {
    terminalId.value = terminals.value[0]?.id
  }
  if (terminalId.value) await loadContext()
  refocus()
})
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))

async function loadContext() {
  if (!terminalId.value) return
  saveLocal(TERMINAL_KEY, terminalId.value)
  ctx.value = await cashierContext(terminalId.value)
  refocus()
}

/** 弹窗都关闭时把焦点放回扫码框 */
function refocus() {
  if (document.querySelector('.ant-modal-wrap:not([style*="display: none"]), .ant-drawer-open')) return
  nextTick(() => scanRef.value?.focus())
}

// ---------------------------------------------------------------- 购物车

async function onScan() {
  const code = scan.value?.trim()
  if (!code) {
    if (lines.value.length) checkout()
    return
  }
  scan.value = ''
  // 数量 × 条码：如 3*6901234567890
  const m = code.match(/^(\d+(?:\.\d+)?)\*(.+)$/)
  const count = m ? Number(m[1]) : 1
  const goodsCode = m ? m[2] : code
  let g
  try {
    g = await lookupGoods(terminalId.value, goodsCode, member.value?.id)
  } catch {
    return // 错误提示由请求拦截器处理
  }
  const exist = lines.value.findIndex((l) => l.materialId === g.materialId && l.unit === g.unit && l.manualPrice == null)
  if (exist >= 0) {
    lines.value[exist].qty = Number(lines.value[exist].qty) + count
    current.value = exist
  } else {
    lines.value.push({
      key: `${g.materialId}-${Date.now()}`,
      materialId: g.materialId,
      code: g.code,
      name: g.name,
      spec: g.spec,
      unit: g.unit,
      qty: count,
      price: g.price,
      priceSource: g.priceSource,
      amount: Number(g.price || 0) * count,
      manualPrice: null,
      discountRate: null,
      stockQty: g.stockQty
    })
    current.value = lines.value.length - 1
  }
  if (Number(g.stockQty) <= 0) message.warning(t('pos.cashier.noStock', { name: g.name }))
  schedulePreview()
}

function removeLine(index) {
  if (index < 0 || index >= lines.value.length) return
  lines.value.splice(index, 1)
  current.value = Math.min(index, lines.value.length - 1)
  schedulePreview()
}

function focusQty() {
  if (current.value < 0) return
  nextTick(() => qtyRefs.value[current.value]?.focus())
}

function focusOrderDiscount() {
  nextTick(() => document.querySelector('.pos-totals .ant-input-number-input')?.focus())
}

function editLine() {
  if (current.value >= 0) lineRef.value.open(lines.value[current.value])
}

function setMember(m) {
  member.value = m
  schedulePreview()
}

function payload(extra = {}) {
  return {
    terminalId: terminalId.value,
    memberId: member.value?.id,
    orderDiscount: orderDiscount.value || null,
    heldId: heldId.value,
    lines: lines.value.map((l) => ({ materialId: l.materialId, unit: l.unit, qty: l.qty, price: l.manualPrice, discountRate: l.discountRate })),
    ...extra
  }
}

let timer
/** 修改后 200ms 试算一次：价格、行金额、应收以后端为准 */
function schedulePreview() {
  clearTimeout(timer)
  if (!lines.value.length) {
    preview.value = null
    previewError.value = ''
    return
  }
  timer = setTimeout(runPreview, 200)
}

async function runPreview() {
  try {
    const p = await previewSale(payload())
    preview.value = p
    previewError.value = ''
    p.lines.forEach((pl, i) => {
      const l = lines.value[i]
      if (!l || l.materialId !== pl.materialId) return
      Object.assign(l, { price: pl.price, priceSource: pl.priceSource, amount: pl.amount, belowMin: pl.belowMin })
    })
    return p
  } catch (e) {
    preview.value = null
    previewError.value = e?.message || t('pos.cashier.previewFailed')
    throw e
  }
}

// ---------------------------------------------------------------- 结算、挂单、退货

async function checkout() {
  if (!lines.value.length || !shift.value) return
  clearTimeout(timer)
  const p = await runPreview()
  payRef.value.open(p.payable, payload())
}

function onPaid(sale) {
  if (sale) {
    printReceipt(sale, ctx.value?.terminal?.receiptPaper).catch(() => {})
  }
  reset()
}

function reset() {
  lines.value = []
  member.value = null
  orderDiscount.value = null
  heldId.value = null
  preview.value = null
  previewError.value = ''
  current.value = -1
  refocus()
}

async function hold() {
  if (!lines.value.length) return
  const content = JSON.stringify({ lines: lines.value, member: member.value, orderDiscount: orderDiscount.value })
  await holdSale({ terminalId: terminalId.value, memberId: member.value?.id, amount: payable.value, itemCount: lines.value.length, content })
  message.success(t('pos.cashier.held'))
  reset()
}

function restore(held) {
  const c = JSON.parse(held.content)
  lines.value = c.lines || []
  member.value = c.member || null
  orderDiscount.value = c.orderDiscount ?? null
  heldId.value = null
  current.value = lines.value.length - 1
  schedulePreview()
  refocus()
}

function onReturned(sale) {
  printReceipt(sale, ctx.value?.terminal?.receiptPaper).catch(() => {})
  refocus()
}

function exit() {
  router.push('/pos/sale').catch(() => router.push('/'))
}

function onKeydown(e) {
  const map = {
    F2: focusQty,
    F3: () => memberRef.value.open(),
    F4: hold,
    F5: () => heldRef.value.open(),
    F6: () => removeLine(current.value),
    F7: editLine,
    F8: focusOrderDiscount,
    F9: () => keys.value.find((k) => k.key === 'F9')?.disabled || returnRef.value.open(),
    F12: checkout
  }
  if (map[e.key]) {
    e.preventDefault()
    map[e.key]()
    return
  }
  const inField = ['INPUT', 'TEXTAREA'].includes(document.activeElement?.tagName) && document.activeElement !== scanRef.value?.input
  if (!inField && (e.key === 'ArrowDown' || e.key === 'ArrowUp') && lines.value.length) {
    e.preventDefault()
    current.value = Math.max(0, Math.min(lines.value.length - 1, current.value + (e.key === 'ArrowDown' ? 1 : -1)))
  } else if (e.key === 'Delete' && !inField && document.activeElement === scanRef.value?.input && !scan.value) {
    removeLine(current.value)
  }
}
</script>

<style scoped>
.pos-screen {
  position: fixed;
  inset: 0;
  z-index: 999;
  display: flex;
  flex-direction: column;
  min-width: 1200px;
  background: var(--app-bg, #f5f6f8);
  padding-top: env(safe-area-inset-top, 0px);
}
.pos-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 16px;
  background: #1f2937;
  color: #fff;
}
.pos-brand,
.pos-shift {
  display: flex;
  align-items: center;
  gap: 10px;
}
.pos-store {
  font-size: 16px;
  font-weight: 600;
}
.pos-terminal {
  width: 220px;
}
.pos-body {
  flex: 1;
  display: flex;
  gap: 12px;
  padding: 12px;
  min-height: 0;
}
.pos-cart {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--app-card-bg, #fff);
  border-radius: 8px;
  padding: 12px;
}
.pos-scan {
  margin-bottom: 8px;
}
.pos-side {
  width: 360px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.pos-member,
.pos-totals,
.pos-keys {
  background: var(--app-card-bg, #fff);
  border-radius: 8px;
  padding: 12px;
}
.member-name {
  font-weight: 600;
}
.pos-totals .row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 0;
}
.pos-totals .payable {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 700;
  color: #d4380d;
}
.preview-error {
  margin-top: 8px;
}
.pos-keys {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}
.pos-keys .ant-btn {
  height: 52px;
  white-space: normal;
}
.fkey {
  display: block;
  font-size: 11px;
  opacity: 0.65;
}
.qty-input {
  width: 100px;
}
.src-tag {
  margin-left: 4px;
}
.below-min {
  color: #d4380d;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
:deep(.pos-row-current) td {
  background: #e6f4ff !important;
}
</style>

<style>
html[data-theme='dark'] .pos-screen {
  background: #141414;
}
html[data-theme='dark'] .pos-screen .pos-row-current td {
  background: #111d2c !important;
}
</style>
