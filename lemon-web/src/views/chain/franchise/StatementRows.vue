<template>
  <a-table :row-key="(r, i) => i" size="small" :columns="columns" :data-source="rows" :loading="loading" :pagination="false" :scroll="{ x: 900 }">
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'type'">{{ typeText(record) }}</template>
      <template v-else-if="column.key === 'source'">{{ sourceText(record) }}</template>
      <template v-else-if="column.money">{{ moneyOrBlank(record[column.dataIndex]) }}</template>
    </template>
  </a-table>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { PartnerLedgerType } from '@/constants/enums'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'
import { moneyOrBlank } from '@/utils/number'

/** 往来逐笔明细（财务往来对账单的 rows）：应收单增加余额，收款、预收、对冲减少余额 */
defineProps({ rows: { type: Array, default: () => [] }, loading: Boolean })
const { t } = useI18n()
const dict = useDict('fin_bill_source', 'fin_settle_method')
const columns = [
  { title: t('chain.common.date'), dataIndex: 'bizDate', width: 110 },
  { title: t('chain.common.kind'), key: 'type', width: 100 },
  { title: t('chain.statement.docNo'), dataIndex: 'docNo', width: 160 },
  { title: t('chain.statement.source'), key: 'source', width: 220 },
  { title: t('chain.statement.increase'), dataIndex: 'increase', width: 110, align: 'right', money: true },
  { title: t('chain.statement.decrease'), dataIndex: 'decrease', width: 110, align: 'right', money: true },
  { title: t('chain.statement.balance'), dataIndex: 'balance', width: 110, align: 'right', money: true },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true }
]

const TYPE_KEYS = {
  [PartnerLedgerType.BILL]: 'chain.statement.typeBill',
  [PartnerLedgerType.SETTLEMENT]: 'chain.statement.typeSettlement',
  [PartnerLedgerType.ADVANCE]: 'chain.statement.typeAdvance',
  [PartnerLedgerType.NETTING]: 'chain.statement.typeNetting'
}
const typeText = (r) => (TYPE_KEYS[r.type] ? t(TYPE_KEYS[r.type]) : r.type)
function sourceText(r) {
  if (r.type === PartnerLedgerType.BILL) {
    return [findDict(dict.fin_bill_source, r.sourceType)?.label || r.sourceType || '', r.sourceNo].filter(Boolean).join(' ')
  }
  if (r.type === PartnerLedgerType.NETTING) return ''
  return findDict(dict.fin_settle_method, r.sourceType)?.label || r.sourceType || ''
}

defineExpose({ typeText, sourceText })
</script>
