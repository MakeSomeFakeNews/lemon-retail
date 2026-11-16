<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('channel.common.shop')" name="shopId">
          <WxShopSelect v-model:value="query.shopId" :placeholder="$t('common.placeholder.all')" @shop="(s) => (shop = s)" />
        </a-form-item>
        <a-form-item :label="$t('channel.product.mapped')" name="mapped">
          <a-select v-model:value="query.mapped" :placeholder="$t('common.placeholder.all')" allow-clear :options="mappedOptions" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('channel.product.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item name="removed">
          <a-checkbox v-model:checked="query.removed">{{ $t('channel.product.includeRemoved') }}</a-checkbox>
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-tooltip :title="query.shopId ? '' : $t('channel.common.selectShopFirst')">
          <a-space>
            <a-button v-perm="'channel:wxShop:sync'" type="primary" :disabled="!query.shopId" :loading="syncing" @click="syncProducts">
              <template #icon><CloudSyncOutlined /></template>{{ $t('channel.product.syncProducts') }}
            </a-button>
            <a-button v-perm="'channel:wxProduct:edit'" :disabled="!query.shopId" @click="autoMatch(query.shopId)">
              <template #icon><ThunderboltOutlined /></template>{{ $t('channel.product.autoMatch') }}
            </a-button>
            <a-button v-if="isMock" v-perm="'channel:wxShop:edit'" @click="mockProducts(query.shopId)">
              <template #icon><ExperimentOutlined /></template>{{ $t('channel.product.mockProducts') }}
            </a-button>
          </a-space>
        </a-tooltip>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: scrollX, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'product'">
            <div>
              {{ record.title }}
              <a-tag v-if="record.removed" color="red">{{ $t('channel.product.removed') }}</a-tag>
            </div>
            <div class="sub-text">{{ record.skuAttrs || '-' }} · {{ record.shopName }}</div>
          </template>
          <template v-else-if="column.key === 'codes'">
            <div>{{ record.outSkuId || '-' }}</div>
            <div class="sub-text">{{ record.skuCode || '' }}</div>
          </template>
          <template v-else-if="column.key === 'price'">
            <div>{{ money(record.price) }}</div>
            <div v-if="record.retailPrice != null" class="sub-text">{{ $t('channel.product.retailPrice', { price: money(record.retailPrice) }) }}</div>
          </template>
          <template v-else-if="column.key === 'material'">
            <template v-if="record.materialId">
              <div>
                {{ record.materialCode }} {{ record.materialName }}
                <a-tag v-if="record.matchManual" color="blue">{{ $t('channel.product.manual') }}</a-tag>
              </div>
              <div class="sub-text">
                {{ record.materialSpec || '' }}
                <span v-if="Number(record.unitRatio) !== 1">{{ $t('channel.product.ratioText', { ratio: qty(record.unitRatio), unit: record.materialUnit || '' }) }}</span>
              </div>
            </template>
            <a-tag v-else color="volcano">{{ $t('channel.product.unmapped') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'stock'">
            <div>{{ $t('channel.product.platformStock', { n: record.platformStock ?? '-' }) }}</div>
            <div class="sub-text">{{ $t('channel.product.pushedStock', { n: record.pushedStock ?? '-' }) }}</div>
            <a-typography-text v-if="record.stockError" type="danger" :content="record.stockError" :ellipsis="{ tooltip: record.stockError }" class="error-text" />
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 手工映射 -->
    <a-modal v-model:open="mapOpen" :title="$t('channel.product.mapTitle')" :confirm-loading="mapSaving" destroy-on-close @ok="submitMap">
      <a-form layout="vertical">
        <a-form-item :label="$t('channel.product.sku')">
          {{ mapTarget?.title }} <span class="sub-text">{{ mapTarget?.skuAttrs }}</span>
        </a-form-item>
        <a-form-item :label="$t('channel.product.material')" required>
          <MaterialSelect v-model:value="mapForm.materialId" :label="mapForm.materialLabel" />
        </a-form-item>
        <a-form-item :label="$t('channel.product.unitRatio')" :extra="$t('channel.product.unitRatioExtra')">
          <a-input-number v-model:value="mapForm.unitRatio" :min="0.0001" :precision="4" class="full" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Modal, message } from 'ant-design-vue'
import { CloudSyncOutlined, DisconnectOutlined, ExperimentOutlined, LinkOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import WxShopSelect from '@/components/channel/WxShopSelect.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { autoMatchWxProduct, mapWxProduct, mockWxProducts, pageWxProduct, syncWxShop, unmapWxProduct } from '@/api/channel/wxShop'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useTable } from '@/composables/useTable'
import { WxShopMode, WxSyncType } from '@/constants/enums'
import { money, qty } from '@/utils/number'

/**
 * 商品映射：小店 SKU ↔ 物料。同步商品时按商家编码 / SKU 编码自动匹配物料编码、条码、包装条码；匹配不到的在这里手工映射，
 * 有未映射商品的订单为「待映射」，映射后在渠道订单中重试
 */
const { t } = useI18n()

const mappedOptions = [
  { value: true, label: t('channel.product.mappedYes') },
  { value: false, label: t('channel.product.unmapped') }
]

const columns = [
  { title: t('channel.product.product'), key: 'product', width: 260 },
  { title: t('channel.product.codes'), key: 'codes', width: 160 },
  { title: t('channel.product.price'), key: 'price', width: 130, align: 'right' },
  { title: t('channel.product.material'), key: 'material', width: 260 },
  { title: t('channel.product.stock'), key: 'stock', width: 180 },
  { title: t('channel.product.syncTime'), dataIndex: 'syncTime', width: 160 },
  actionColumn(2, [t('channel.product.map'), t('channel.product.unmap')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'channel-wx-product')
const showSearch = ref(true)

const emptyQuery = () => ({ shopId: undefined, mapped: undefined, keyword: undefined, removed: false })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageWxProduct, query)
const shop = ref()
const isMock = computed(() => query.shopId && shop.value?.mode === WxShopMode.MOCK)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

onMounted(load)

const rowActions = (record) => [
  { key: 'map', icon: LinkOutlined, label: t('channel.product.map'), perm: 'channel:wxProduct:edit', onClick: () => openMap(record) },
  {
    key: 'unmap',
    icon: DisconnectOutlined,
    label: t('channel.product.unmap'),
    perm: 'channel:wxProduct:edit',
    disabled: !record.materialId,
    confirm: t('channel.product.confirmUnmap'),
    onClick: () => unmap(record.id)
  }
]

const unmap = useAction(unmapWxProduct, { reload: () => load() })
const autoMatch = useAction(autoMatchWxProduct, { successKey: (n) => t('channel.product.autoMatched', { n }), reload: () => load() })
const mockProducts = useAction(mockWxProducts, { successKey: (n) => t('channel.product.mockCreated', { n }) })

const syncing = ref(false)
async function syncProducts() {
  syncing.value = true
  try {
    const r = await syncWxShop(query.shopId, WxSyncType.PRODUCT)
    const content = t('channel.shop.syncResult', { total: r.total, succeeded: r.succeeded, failed: r.failed })
    if (r.success && !r.failed) message.success(content)
    else Modal.warning({ title: content, content: r.message || '' })
    load()
  } finally {
    syncing.value = false
  }
}

// ---------- 手工映射 ----------
const mapOpen = ref(false)
const mapSaving = ref(false)
const mapTarget = ref()
const mapForm = reactive({ materialId: undefined, materialLabel: undefined, unitRatio: 1 })

function openMap(record) {
  mapTarget.value = record
  Object.assign(mapForm, {
    materialId: record.materialId ?? undefined,
    materialLabel: record.materialId ? `${record.materialCode} ${record.materialName}` : undefined,
    unitRatio: Number(record.unitRatio) || 1
  })
  mapOpen.value = true
}

async function submitMap() {
  if (!mapForm.materialId) {
    message.warning(t('channel.product.selectMaterial'))
    return
  }
  mapSaving.value = true
  try {
    await mapWxProduct(mapTarget.value.id, { materialId: mapForm.materialId, unitRatio: mapForm.unitRatio || 1 })
    message.success(t('common.msg.saveSuccess'))
    mapOpen.value = false
    load()
  } finally {
    mapSaving.value = false
  }
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
.error-text {
  font-size: 12px;
  max-width: 160px;
}
</style>
