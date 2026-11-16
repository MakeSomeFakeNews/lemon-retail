<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card">
      <SearchForm :model="query" @search="load" @reset="resetQuery">
        <a-form-item :label="$t('pos.report.dateRange')" name="range">
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" :allow-clear="false" />
        </a-form-item>
        <a-form-item :label="$t('store.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
      </SearchForm>
      <a-alert type="info" show-icon :message="$t('pos.settle.formula')" class="tip" />
    </a-card>
    <a-card :bordered="false" class="page-card table-card">
      <a-table row-key="key" size="middle" :columns="columns" :data-source="rows" :loading="loading" :pagination="false" :scroll="{ x: 1200, y: '100%' }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'store'">
            <b v-if="record.total">{{ $t('pos.report.total') }}</b>
            <span v-else>{{ record.storeName }} <span class="sub-text">{{ record.storeCode }}</span></span>
          </template>
          <template v-else-if="column.drill && !record.total">
            <a @click="drill(record, column.drill)">{{ money(record[column.key]) }}</a>
          </template>
          <template v-else-if="column.key === 'settleAmount'">
            <span :class="Number(record.settleAmount) < 0 ? 'neg' : 'pos'">{{ money(record.settleAmount) }}</span>
            <div class="sub-text">{{ settleText(record.settleAmount) }}</div>
          </template>
          <template v-else-if="column.money">{{ money(record[column.key]) }}</template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import SearchForm from '@/components/SearchForm.vue'
import StoreSelect from '@/components/store/StoreSelect.vue'
import { storedValueSettle } from '@/api/pos'
import { money } from '@/utils/number'
import { StoredValueType } from '@/constants/enums'

/**
 * 储值跨店结算（按门店，营业日期范围）：结算金额 = 本店储值消费净额（消费 − 退回）− 本店充值实收，
 * 正数为总部应付门店、负数为门店应缴总部；赠送、调整单列不参与结算。金额可穿透到储值流水
 */
const { t } = useI18n()
const router = useRouter()
const local = (d) => new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10)
const emptyQuery = () => {
  const now = new Date()
  return { range: [local(new Date(now.getFullYear(), now.getMonth(), 1)), local(now)], storeId: undefined }
}
const query = reactive(emptyQuery())
const rows = ref([])
const loading = ref(false)

const columns = computed(() => [
  { title: t('store.common.store'), key: 'store', width: 200, fixed: 'left' },
  { title: t('pos.settle.topupCount'), dataIndex: 'topupCount', width: 90, align: 'right' },
  { title: t('pos.settle.topupAmount'), key: 'topupAmount', width: 120, align: 'right', money: true, drill: StoredValueType.TOPUP },
  { title: t('pos.settle.bonusAmount'), key: 'bonusAmount', width: 110, align: 'right', money: true, drill: StoredValueType.BONUS },
  { title: t('pos.settle.consumeAmount'), key: 'consumeAmount', width: 120, align: 'right', money: true, drill: StoredValueType.CONSUME },
  { title: t('pos.settle.refundAmount'), key: 'refundAmount', width: 110, align: 'right', money: true, drill: StoredValueType.REFUND },
  { title: t('pos.settle.netConsume'), key: 'netConsume', width: 120, align: 'right', money: true },
  { title: t('pos.settle.adjustAmount'), key: 'adjustAmount', width: 110, align: 'right', money: true, drill: StoredValueType.ADJUST },
  { title: t('pos.settle.settleAmount'), key: 'settleAmount', width: 150, align: 'right' }
])

const settleText = (v) => (Number(v) > 0 ? t('pos.settle.hqPays') : Number(v) < 0 ? t('pos.settle.storePays') : '')

async function load() {
  loading.value = true
  try {
    const r = await storedValueSettle({ storeId: query.storeId, begin: query.range?.[0], end: query.range?.[1] })
    rows.value = [...r.rows.map((x) => ({ ...x, key: x.storeId })), { ...r.total, key: '__total', total: true }]
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.assign(query, emptyQuery())
  load()
}

function drill(record, changeType) {
  router.push({ path: '/pos/stored-value-log', query: { storeId: record.storeId, changeType, begin: query.range[0], end: query.range[1] } })
}

onMounted(load)
</script>

<style scoped>
.tip {
  margin-top: 8px;
}
.pos {
  color: #389e0d;
}
.neg {
  color: #cf1322;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
