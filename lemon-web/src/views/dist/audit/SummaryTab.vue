<template>
  <div class="tab-body">
    <div class="toolbar">
      <a-space wrap>
        <WarehouseSelect v-model:value="supplyWarehouseId" all :placeholder="$t('dist.common.supplyWarehouse')" class="w200" @update:value="load" />
        <a-input-search v-model:value="keyword" allow-clear :placeholder="$t('dist.common.material')" class="w200" @search="load" />
        <a-checkbox v-model:checked="onlyShortage">{{ $t('dist.audit.onlyShortage') }}</a-checkbox>
      </a-space>
      <a-space>
        <a-button @click="load"><template #icon><ReloadOutlined /></template></a-button>
        <a-button v-perm="'dist:summary:purchase'" type="primary" :disabled="!selectedKeys.length" :loading="creating" @click="openPurchase">
          {{ $t('dist.audit.toPurchase') }}
        </a-button>
      </a-space>
    </div>
    <a-table
      :row-key="rowKey"
      size="small"
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="rows.length > 50 ? { pageSize: 50 } : false"
      :row-selection="{ selectedRowKeys: selectedKeys, onChange: (k) => (selectedKeys = k) }"
      :scroll="{ x: 1100 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'material'">
          <div>{{ record.materialCode }} {{ record.materialName }}</div>
          <div class="sub-text">{{ record.spec }}</div>
        </template>
        <template v-else-if="column.key === 'shortage'">
          <span :class="{ danger: Number(record.shortage) > 0 }">{{ qty(record.shortage) }}</span>
        </template>
        <template v-else-if="column.qty">{{ qty(record[column.dataIndex]) }}</template>
      </template>
    </a-table>

    <a-modal v-model:open="purchaseOpen" :title="$t('dist.audit.purchaseTitle')" width="min(860px, 100vw)" :confirm-loading="creating" @ok="createOrders">
      <a-alert type="info" show-icon :message="$t('dist.audit.purchaseTip')" class="block" />
      <a-table row-key="key" size="small" :columns="purchaseColumns" :data-source="purchaseRows" :pagination="false" :scroll="{ x: 700, y: 400 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
          <template v-else-if="column.key === 'qty'">
            <a-input-number v-model:value="record.qty" :min="0.0001" :precision="4" class="full" />
          </template>
          <template v-else-if="column.key === 'supplier'">
            <PartnerSelect v-model:value="record.supplierId" type="supplier" :label="record.supplierName" />
          </template>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, h, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Modal, message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { requisitionSummary, requisitionToPurchase } from '@/api/dist'
import { qty } from '@/utils/number'

/**
 * 要货汇总：已审核未配送的要货按 供货仓库 + 物料 汇总，对照供货仓库可用库存得出缺货；
 * 勾选后按供应商拆分生成采购订单（同一供货仓库，收货仓库为供货仓库）
 */
const { t } = useI18n()
const router = useRouter()
const supplyWarehouseId = ref()
const keyword = ref()
const onlyShortage = ref(false)
const loading = ref(false)
const list = ref([])
const selectedKeys = ref([])
const rowKey = (r) => `${r.supplyWarehouseId}-${r.materialId}`
const rows = computed(() => (onlyShortage.value ? list.value.filter((r) => Number(r.shortage) > 0) : list.value))

const q = (dataIndex, title, width = 100) => ({ title, dataIndex, width, align: 'right', qty: true })
const columns = computed(() => [
  { title: t('dist.common.supplyWarehouse'), dataIndex: 'supplyWarehouseName', width: 140, ellipsis: true },
  { title: t('dist.common.material'), key: 'material', width: 240 },
  { title: t('common.field.unit'), dataIndex: 'unit', width: 60 },
  { title: t('dist.audit.requisitionCount'), dataIndex: 'requisitionCount', width: 90, align: 'right' },
  { title: t('dist.audit.storeCount'), dataIndex: 'storeCount', width: 80, align: 'right' },
  q('approvedQty', t('dist.requisition.approvedQty')),
  q('deliveredQty', t('dist.requisition.deliveredQty')),
  q('pendingQty', t('dist.requisition.pendingQty')),
  q('available', t('dist.audit.available')),
  { title: t('dist.audit.shortage'), key: 'shortage', dataIndex: 'shortage', width: 90, align: 'right' },
  { title: t('dist.audit.supplier'), dataIndex: 'supplierName', width: 160, ellipsis: true }
])

async function load() {
  loading.value = true
  try {
    list.value = await requisitionSummary({ supplyWarehouseId: supplyWarehouseId.value, keyword: keyword.value || undefined })
    selectedKeys.value = []
  } finally {
    loading.value = false
  }
}
onMounted(load)

const purchaseOpen = ref(false)
const purchaseRows = ref([])
const creating = ref(false)
const purchaseColumns = computed(() => [
  { title: t('dist.common.material'), key: 'material', width: 240, ellipsis: true },
  { title: t('dist.audit.purchaseQty'), key: 'qty', width: 140 },
  { title: t('dist.audit.supplier'), key: 'supplier', width: 260 }
])

function openPurchase() {
  const picked = list.value.filter((r) => selectedKeys.value.includes(rowKey(r)))
  if (new Set(picked.map((r) => r.supplyWarehouseId)).size > 1) {
    message.warning(t('dist.audit.purchaseWarehouseMixed'))
    return
  }
  purchaseRows.value = picked.map((r) => ({
    key: rowKey(r),
    warehouseId: r.supplyWarehouseId,
    materialId: r.materialId,
    materialCode: r.materialCode,
    materialName: r.materialName,
    qty: Number(r.shortage) > 0 ? Number(r.shortage) : Number(r.pendingQty),
    supplierId: r.supplierId ?? undefined,
    supplierName: r.supplierName
  }))
  purchaseOpen.value = true
}

async function createOrders() {
  if (purchaseRows.value.some((r) => !r.supplierId)) {
    message.warning(t('dist.audit.noSupplier'))
    return
  }
  creating.value = true
  try {
    const ids = await requisitionToPurchase({
      warehouseId: purchaseRows.value[0].warehouseId,
      items: purchaseRows.value.map((r) => ({ materialId: r.materialId, qty: r.qty, supplierId: r.supplierId }))
    })
    purchaseOpen.value = false
    Modal.confirm({
      title: t('dist.common.created', { count: ids.length }),
      content: h('div', t('dist.audit.purchaseTip')),
      onOk: () => router.push(ids.length === 1 ? { path: '/scm/purchase', query: { id: ids[0] } } : '/scm/purchase')
    })
  } finally {
    creating.value = false
  }
}

defineExpose({ load })
</script>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: space-between;
  margin-bottom: 12px;
}
.w200 {
  width: 200px;
}
.full {
  width: 100%;
}
.block {
  margin-bottom: 12px;
}
.danger {
  color: var(--ant-color-error, #ff4d4f);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
