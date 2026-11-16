<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('channel.shop.name')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('channel.shop.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('channel.shop.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="statusOptions" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'channel:wxShop:add'" type="primary" @click="shopForm.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('channel.shop.createTitle') }}
        </a-button>
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
          <template v-if="column.key === 'name'">
            <div>{{ record.name }}</div>
            <div v-if="record.appId" class="sub-text">{{ record.appId }}</div>
          </template>
          <template v-else-if="column.key === 'mode'">
            <DictTag type="ch_wx_shop_mode" :value="record.mode" />
          </template>
          <template v-else-if="column.key === 'store'">
            <div>{{ record.storeName }}</div>
            <div class="sub-text">{{ record.warehouseName }}</div>
          </template>
          <template v-else-if="column.key === 'sync'">
            <div>
              <a-tag :color="record.orderSyncEnabled ? 'green' : 'default'">{{ $t('channel.shop.orderSyncShort') }}</a-tag>
              <a-tag :color="record.stockSyncEnabled ? 'green' : 'default'">{{ $t('channel.shop.stockSyncShort') }}</a-tag>
            </div>
            <div class="sub-text">{{ $t('channel.shop.orderSyncedTo', { time: record.orderSyncedTo || '-' }) }}</div>
            <div class="sub-text">{{ $t('channel.shop.stockSyncedAt', { time: record.stockSyncedAt || '-' }) }}</div>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === STATUS_ENABLED ? 'green' : 'default'">
              {{ record.status === STATUS_ENABLED ? $t('common.status.enabled') : $t('common.status.disabled') }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <WxShopFormDrawer ref="shopForm" @success="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Modal, message } from 'ant-design-vue'
import { ApiOutlined, CloudSyncOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import StoreSelect from '@/components/store/StoreSelect.vue'
import WxShopFormDrawer from './WxShopFormDrawer.vue'
import { deleteWxShop, pageWxShop, syncWxShop, testWxShop } from '@/api/channel/wxShop'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { WxSyncType } from '@/constants/enums'
import { STATUS_DISABLED, STATUS_ENABLED } from '@/utils/constants'

/** 微信小店店铺设置：绑定门店与发货仓库、对接方式（模拟 / 正式）、消息推送、同步设置；立即同步商品、库存、订单、售后 */
const { t } = useI18n()
useDict('ch_wx_shop_mode')

const statusOptions = [
  { value: STATUS_ENABLED, label: t('common.status.enabled') },
  { value: STATUS_DISABLED, label: t('common.status.disabled') }
]
const SYNC_ITEMS = [
  { type: WxSyncType.PRODUCT, label: t('channel.shop.syncProducts') },
  { type: WxSyncType.STOCK, label: t('channel.shop.syncStock') },
  { type: WxSyncType.ORDER, label: t('channel.shop.syncOrders') },
  { type: WxSyncType.AFTERSALE, label: t('channel.shop.syncAftersales') }
]

const columns = [
  { title: t('channel.shop.name'), key: 'name', width: 200 },
  { title: t('channel.shop.mode'), key: 'mode', width: 110, dict: 'ch_wx_shop_mode', dataIndex: 'mode' },
  { title: t('channel.shop.storeWarehouse'), key: 'store', width: 200 },
  { title: t('channel.shop.customer'), dataIndex: 'customerName', width: 150, ellipsis: true },
  { title: t('channel.shop.sync'), key: 'sync', width: 260 },
  { title: t('common.field.status'), key: 'status', width: 90 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  actionColumn(4, [t('channel.shop.test'), t('common.action.sync')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'channel-wx-shop')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, storeId: undefined, status: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageWxShop, query)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

onMounted(load)

const shopForm = ref()

const rowActions = (record) => [
  { type: 'edit', perm: 'channel:wxShop:edit', onClick: () => shopForm.value.openEdit(record) },
  { key: 'test', icon: ApiOutlined, label: t('channel.shop.test'), perm: 'channel:wxShop:edit', onClick: () => test(record.id) },
  ...SYNC_ITEMS.map((s) => ({
    key: `sync-${s.type}`,
    icon: CloudSyncOutlined,
    label: s.label,
    perm: 'channel:wxShop:sync',
    disabled: record.status !== STATUS_ENABLED,
    onClick: () => sync(record.id, s.type)
  })),
  {
    type: 'delete',
    perm: 'channel:wxShop:remove',
    confirm: { title: t('channel.shop.confirmDelete', { name: record.name }), description: t('channel.shop.deleteTip') },
    onClick: () => remove(record.id)
  }
]

const remove = useAction(deleteWxShop, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
const test = useAction(testWxShop, { successKey: (text) => text })

async function sync(id, type) {
  const hide = message.loading(t('channel.shop.syncing'), 0)
  try {
    const r = await syncWxShop(id, type)
    const content = t('channel.shop.syncResult', { total: r.total, succeeded: r.succeeded, failed: r.failed })
    if (r.success && !r.failed) message.success(content)
    else Modal.warning({ title: content, content: r.message || '' })
    load()
  } finally {
    hide()
  }
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
