<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card">
      <a-space wrap>
        <StoreSelect v-model:value="storeId" trading :allow-clear="false" class="w200" @update:value="onStoreChange" />
        <a-input-search v-model:value="keyword" allow-clear :placeholder="$t('chain.replenish.keywordPh')" class="w200" @search="load" />
        <a-checkbox v-model:checked="onlySuggest" @change="load">{{ $t('chain.replenish.onlySuggest') }}</a-checkbox>
        <template v-if="params">
          <span class="sub-text">{{ $t('chain.replenish.salesDays', { n: params.salesDays }) }}</span>
          <span>{{ $t('chain.replenish.leadDays') }}</span>
          <a-input-number v-model:value="lead" :min="0" :max="365" :precision="0" :placeholder="String(params.leadDays)" class="w90" />
          <span>{{ $t('chain.replenish.safetyDays') }}</span>
          <a-input-number v-model:value="safety" :min="0" :max="365" :precision="0" :placeholder="String(params.safetyDays)" class="w90" />
          <a-button v-perm="'chain:replenish:param'" :loading="savingParams" @click="saveParams">{{ $t('chain.replenish.saveParams') }}</a-button>
        </template>
        <a-tooltip :title="$t('chain.replenish.tip')"><InfoCircleOutlined class="sub-text" /></a-tooltip>
      </a-space>
    </a-card>
    <a-card :bordered="false" class="page-card table-card">
      <a-alert v-if="isSupplier" type="info" show-icon class="mb12">
        <template #message>
          {{ $t('chain.replenish.supplierTip') }}
          <router-link v-if="userStore.hasPerm('scm:reorder:list')" to="/scm/reorder">{{ $t('chain.replenish.toReorder') }}</router-link>
        </template>
      </a-alert>
      <a-space class="mb12">
        <a-button
          v-perm="'chain:replenish:requisition'"
          type="primary"
          :disabled="isSupplier || !selectedKeys.length"
          :loading="creating"
          @click="toRequisition"
        >
          {{ $t('chain.replenish.toRequisition', { n: selectedKeys.length }) }}
        </a-button>
      </a-space>
      <a-table
        :row-key="rowKey"
        size="middle"
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="false"
        :row-selection="isSupplier ? undefined : { selectedRowKeys: selectedKeys, onChange: (k) => (selectedKeys = k) }"
        :scroll="{ x: 1500, y: '100%' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">
            {{ record.materialName }}
            <div class="sub-text">{{ record.materialCode }} {{ record.spec || '' }}</div>
          </template>
          <template v-else-if="column.key === 'qty'">
            <a-input-number v-model:value="record.qty" :min="0" :precision="4" size="small" class="w100" />
          </template>
          <template v-else-if="column.key === 'suggestQty'"><b>{{ qty(record.suggestQty) }}</b></template>
          <template v-else-if="column.qty">{{ qty(record[column.dataIndex]) }}</template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { InfoCircleOutlined } from '@ant-design/icons-vue'
import StoreSelect from '@/components/store/StoreSelect.vue'
import { replenishParams, replenishSuggest, replenishToRequisition, saveReplenishParams } from '@/api/chain'
import { StoreType } from '@/constants/enums'
import { useUserStore } from '@/store/user'
import { qty } from '@/utils/number'

/**
 * 门店补货建议：日均销量（近 N 天 POS 零售 + 销售出库，扣退货）× (到货天数 + 安全天数) − 现存 − 在途 − 未完成的要货；
 * 直营店、加盟店勾选后一键生成要货单（每个仓库一张草稿），总部、配送中心使用供应链的补货建议生成采购订单
 */
const { t } = useI18n()
const userStore = useUserStore()
const storeId = ref()
const keyword = ref('')
const onlySuggest = ref(true)
const rows = ref([])
const loading = ref(false)
const selectedKeys = ref([])
const params = ref()
const lead = ref(null)
const safety = ref(null)
const savingParams = ref(false)
const creating = ref(false)
const rowKey = (r) => `${r.warehouseId}-${r.materialId}`
const isSupplier = computed(() => [StoreType.HQ, StoreType.DC].includes(rows.value[0]?.storeType))

const columns = [
  { title: t('chain.replenish.warehouse'), dataIndex: 'warehouseName', width: 140, fixed: 'left' },
  { title: t('chain.replenish.material'), key: 'material', width: 220 },
  { title: t('chain.replenish.unit'), dataIndex: 'unit', width: 70 },
  { title: t('chain.replenish.soldQty'), dataIndex: 'soldQty', width: 100, align: 'right', qty: true },
  { title: t('chain.replenish.avgDaily'), dataIndex: 'avgDaily', width: 100, align: 'right', qty: true },
  { title: t('chain.replenish.stockQty'), dataIndex: 'stockQty', width: 100, align: 'right', qty: true },
  { title: t('chain.replenish.transitQty'), dataIndex: 'transitQty', width: 100, align: 'right', qty: true },
  { title: t('chain.replenish.requisitionQty'), dataIndex: 'requisitionQty', width: 110, align: 'right', qty: true },
  { title: t('chain.replenish.targetQty'), dataIndex: 'targetQty', width: 100, align: 'right', qty: true },
  { title: t('chain.replenish.suggestQty'), key: 'suggestQty', width: 100, align: 'right' },
  { title: t('chain.replenish.qty'), key: 'qty', width: 130, align: 'right' }
]

async function load() {
  if (!storeId.value) return
  loading.value = true
  try {
    const list = await replenishSuggest({ storeId: storeId.value, keyword: keyword.value || undefined, onlySuggest: onlySuggest.value })
    rows.value = list.map((r) => ({ ...r, qty: Number(r.suggestQty) || null }))
    selectedKeys.value = rows.value.filter((r) => Number(r.suggestQty) > 0).map(rowKey)
  } finally {
    loading.value = false
  }
}

async function onStoreChange() {
  params.value = await replenishParams(storeId.value)
  lead.value = params.value.storeLeadDays
  safety.value = params.value.storeSafetyDays
  load()
}

async function saveParams() {
  savingParams.value = true
  try {
    await saveReplenishParams(storeId.value, { leadDays: lead.value ?? null, safetyDays: safety.value ?? null })
    message.success(t('common.msg.saveSuccess'))
    load()
  } finally {
    savingParams.value = false
  }
}

/** 按仓库分组，每个仓库生成一张要货单草稿 */
async function toRequisition() {
  const picked = rows.value.filter((r) => selectedKeys.value.includes(rowKey(r)) && Number(r.qty) > 0)
  if (!picked.length) {
    message.warning(t('chain.replenish.noQty'))
    return
  }
  const groups = new Map()
  picked.forEach((r) => groups.set(r.warehouseId, [...(groups.get(r.warehouseId) || []), r]))
  creating.value = true
  try {
    for (const [warehouseId, items] of groups) {
      await replenishToRequisition({ warehouseId, items: items.map((r) => ({ materialId: r.materialId, qty: r.qty, suggestQty: r.suggestQty })) })
    }
    message.success(t('chain.replenish.created', { n: groups.size }))
    load()
  } finally {
    creating.value = false
  }
}
</script>

<style scoped>
.w200 {
  width: 200px;
}
.w90 {
  width: 90px;
}
.w100 {
  width: 100px;
}
.mb12 {
  margin-bottom: 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
