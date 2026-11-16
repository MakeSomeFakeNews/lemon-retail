<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('channel.common.shop')" name="shopId">
          <WxShopSelect v-model:value="query.shopId" :placeholder="$t('common.placeholder.all')" @shop="(s) => (shop = s)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('channel.order.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('channel.order.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ch_wx_order_status)" />
        </a-form-item>
        <a-form-item :label="$t('channel.order.erpStatus')" name="erpStatus">
          <a-select v-model:value="query.erpStatus" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ch_wx_erp_status)" />
        </a-form-item>
        <a-form-item :label="$t('channel.order.deliveryStatus')" name="deliveryStatus">
          <a-select
            v-model:value="query.deliveryStatus"
            :placeholder="$t('common.placeholder.all')"
            allow-clear
            :options="toOptions(dict.ch_wx_delivery_status)"
          />
        </a-form-item>
        <a-form-item :label="$t('channel.order.orderDate')" name="dateRange">
          <a-range-picker v-model:value="query.dateRange" value-format="YYYY-MM-DD" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-tooltip :title="query.shopId ? '' : $t('channel.common.selectShopFirst')">
          <a-button v-perm="'channel:wxShop:sync'" type="primary" :disabled="!query.shopId" :loading="syncing" @click="syncOrders">
            <template #icon><CloudSyncOutlined /></template>{{ $t('channel.order.syncOrders') }}
          </a-button>
        </a-tooltip>
        <template v-if="isMock">
          <a-tag color="orange">{{ $t('channel.common.mockShop') }}</a-tag>
          <a-button v-perm="'channel:wxShop:edit'" @click="mockOrders(query.shopId, 3)">
            <template #icon><ExperimentOutlined /></template>{{ $t('channel.order.mockOrders') }}
          </a-button>
        </template>
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
          <template v-if="column.key === 'orderId'">
            <a @click="detailRef.open(record.id)">{{ record.orderId }}</a>
            <div class="sub-text">{{ record.orderTime }}</div>
          </template>
          <template v-else-if="column.key === 'shop'">
            <div>{{ record.shopName }}</div>
            <div class="sub-text">{{ record.storeName }}</div>
          </template>
          <template v-else-if="column.key === 'goods'">
            <div class="ellipsis">{{ record.goodsSummary }}</div>
            <div v-if="record.itemCount > 1" class="sub-text">{{ $t('channel.order.itemCount', { n: record.itemCount }) }}</div>
          </template>
          <template v-else-if="column.key === 'paidAmount'">{{ money(record.paidAmount) }}</template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="ch_wx_order_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'erpStatus'">
            <DictTag type="ch_wx_erp_status" :value="record.erpStatus" />
            <a-tooltip v-if="record.errorMsg" :title="record.errorMsg">
              <ExclamationCircleOutlined class="error-icon" />
            </a-tooltip>
            <div class="sub-text">{{ record.erpOrderNo || '' }}</div>
          </template>
          <template v-else-if="column.key === 'deliveryStatus'">
            <DictTag type="ch_wx_delivery_status" :value="record.deliveryStatus" />
            <div class="sub-text">{{ record.waybillNo || '' }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <WxOrderDetail ref="detailRef" @ship="(o) => shipRef.open(o)" @changed="load" />
    <ShipModal ref="shipRef" @success="afterShip" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Modal, message } from 'ant-design-vue'
import { CarOutlined, CloudSyncOutlined, ExclamationCircleOutlined, ExperimentOutlined, RedoOutlined, SendOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import WxShopSelect from '@/components/channel/WxShopSelect.vue'
import ShipModal from './ShipModal.vue'
import WxOrderDetail from './WxOrderDetail.vue'
import { mockWxOrders, pageWxOrder, syncWxShop } from '@/api/channel/wxShop'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useRouteOpen } from '@/composables/useRouteOpen'
import { useTable } from '@/composables/useTable'
import { WxShopMode, WxSyncType } from '@/constants/enums'
import { toOptions } from '@/utils/dict'
import { money } from '@/utils/number'
import { canPush, canRetry, canShip, useOrderActions } from './useOrderActions'

/**
 * 渠道订单：微信小店的订单（增量同步 + 消息推送）。付款后生成并审核销售订单，有未映射商品时待映射；
 * 在这里发货（生成销售出库并回传运单号），也可以在小店后台发货（同步后自动出库）
 */
const { t } = useI18n()
const dict = useDict('ch_wx_order_status', 'ch_wx_erp_status', 'ch_wx_delivery_status')

const columns = [
  { title: t('channel.order.orderId'), key: 'orderId', width: 190 },
  { title: t('channel.common.shopStore'), key: 'shop', width: 170 },
  { title: t('channel.order.goods'), key: 'goods', width: 220 },
  { title: t('channel.order.paidAmount'), key: 'paidAmount', width: 110, align: 'right' },
  { title: t('channel.order.status'), key: 'status', width: 100, dict: 'ch_wx_order_status', dataIndex: 'status' },
  { title: t('channel.order.erpStatus'), key: 'erpStatus', width: 160, dict: 'ch_wx_erp_status', dataIndex: 'erpStatus' },
  { title: t('channel.order.deliveryStatus'), key: 'deliveryStatus', width: 150, dict: 'ch_wx_delivery_status', dataIndex: 'deliveryStatus' },
  { title: t('channel.order.payTime'), dataIndex: 'payTime', width: 160 },
  actionColumn(3, [t('channel.order.ship'), t('channel.order.createErpOrder'), t('channel.order.pushDelivery')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'channel-wx-order')
const showSearch = ref(true)

const emptyQuery = () => ({
  shopId: undefined,
  keyword: undefined,
  status: undefined,
  erpStatus: undefined,
  deliveryStatus: undefined,
  dateRange: undefined
})
const query = reactive(emptyQuery())
const fetcher = ({ dateRange, ...params }) => pageWxOrder({ ...params, beginDate: dateRange?.[0], endDate: dateRange?.[1] })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(fetcher, query)
const shop = ref()
const isMock = computed(() => query.shopId && shop.value?.mode === WxShopMode.MOCK)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const detailRef = ref()
const shipRef = ref()
useRouteOpen({ id: (id) => detailRef.value?.open(id), keyword: (kw) => (query.keyword = kw), reload: () => search() })
onMounted(load)

const { retry, push } = useOrderActions(() => load())
const rowActions = (record) => [
  { type: 'detail', onClick: () => detailRef.value.open(record.id) },
  {
    key: 'ship',
    icon: CarOutlined,
    label: t('channel.order.ship'),
    perm: 'channel:wxOrder:ship',
    show: canShip(record),
    onClick: () => shipRef.value.open(record)
  },
  {
    key: 'retry',
    icon: RedoOutlined,
    label: record.erpOrderId ? t('channel.order.reprocess') : t('channel.order.createErpOrder'),
    perm: 'channel:wxOrder:ship',
    show: canRetry(record),
    onClick: () => retry(record)
  },
  {
    key: 'push',
    icon: SendOutlined,
    label: t('channel.order.pushDelivery'),
    perm: 'channel:wxOrder:ship',
    show: canPush(record),
    onClick: () => push(record)
  }
]

function afterShip() {
  load()
  detailRef.value?.reload()
}

const syncing = ref(false)
async function syncOrders() {
  syncing.value = true
  try {
    const r = await syncWxShop(query.shopId, WxSyncType.ORDER)
    const content = t('channel.shop.syncResult', { total: r.total, succeeded: r.succeeded, failed: r.failed })
    if (r.success && !r.failed) message.success(content)
    else Modal.warning({ title: content, content: r.message || '' })
    load()
  } finally {
    syncing.value = false
  }
}

const mockOrders = useAction(mockWxOrders, { successKey: (n) => t('channel.order.mockCreated', { n }), reload: () => load() })
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.error-icon {
  margin-left: 4px;
  color: var(--app-error, #ff4d4f);
}
</style>
