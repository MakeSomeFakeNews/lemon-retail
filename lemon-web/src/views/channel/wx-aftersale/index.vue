<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('channel.common.shop')" name="shopId">
          <WxShopSelect v-model:value="query.shopId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('channel.aftersale.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('channel.aftersale.type')" name="type">
          <a-select v-model:value="query.type" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ch_wx_aftersale_type)" />
        </a-form-item>
        <a-form-item :label="$t('channel.aftersale.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ch_wx_aftersale_status)" />
        </a-form-item>
        <a-form-item :label="$t('channel.aftersale.erpStatus')" name="erpStatus">
          <a-select
            v-model:value="query.erpStatus"
            :placeholder="$t('common.placeholder.all')"
            allow-clear
            :options="toOptions(dict.ch_wx_aftersale_erp_status)"
          />
        </a-form-item>
        <a-form-item :label="$t('channel.aftersale.applyDate')" name="dateRange">
          <a-range-picker v-model:value="query.dateRange" value-format="YYYY-MM-DD" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <a-alert type="info" show-icon class="tip" :message="$t('channel.aftersale.tip')" />
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-tooltip :title="query.shopId ? '' : $t('channel.common.selectShopFirst')">
          <a-button v-perm="'channel:wxShop:sync'" type="primary" :disabled="!query.shopId" :loading="syncing" @click="syncAftersales">
            <template #icon><CloudSyncOutlined /></template>{{ $t('channel.aftersale.sync') }}
          </a-button>
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
          <template v-if="column.key === 'aftersaleId'">
            <div>{{ record.aftersaleId }}</div>
            <div class="sub-text">{{ record.applyTime }}</div>
          </template>
          <template v-else-if="column.key === 'order'">
            <a v-if="record.wxOrderId" @click="openOrder(record)">{{ record.orderId }}</a>
            <span v-else>{{ record.orderId }}</span>
            <div class="sub-text">{{ record.shopName }} · {{ record.storeName }}</div>
          </template>
          <template v-else-if="column.key === 'goods'">
            <div class="ellipsis">{{ record.title || record.skuId }}</div>
            <div class="sub-text">{{ $t('channel.aftersale.qtyText', { n: record.qty }) }}</div>
          </template>
          <template v-else-if="column.key === 'type'"><DictTag type="ch_wx_aftersale_type" :value="record.type" /></template>
          <template v-else-if="column.key === 'refundAmount'">{{ money(record.refundAmount) }}</template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="ch_wx_aftersale_status" :value="record.status" />
            <div class="sub-text">{{ record.platformStatus }}</div>
          </template>
          <template v-else-if="column.key === 'erpStatus'">
            <DictTag type="ch_wx_aftersale_erp_status" :value="record.erpStatus" />
            <a-tooltip v-if="record.errorMsg" :title="record.errorMsg">
              <ExclamationCircleOutlined class="note-icon" />
            </a-tooltip>
            <div><SourceDocLink :type="BillSource.SALES_RETURN" :id="record.salesReturnId" :no="record.salesReturnNo" /></div>
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 拒绝 -->
    <a-modal v-model:open="rejectOpen" :title="$t('channel.aftersale.rejectTitle')" :confirm-loading="rejecting" destroy-on-close @ok="submitReject">
      <a-form layout="vertical">
        <a-form-item :label="$t('channel.aftersale.rejectReason')" required>
          <a-textarea v-model:value="rejectReason" :rows="3" :maxlength="200" show-count />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  CloudSyncOutlined,
  ExclamationCircleOutlined,
  InboxOutlined,
  RedoOutlined,
  SyncOutlined
} from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import SourceDocLink from '@/components/common/SourceDocLink.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import WxShopSelect from '@/components/channel/WxShopSelect.vue'
import {
  acceptWxAftersale,
  confirmWxAftersale,
  pageWxAftersale,
  refreshWxAftersale,
  rejectWxAftersale,
  retryWxAftersale,
  syncWxShop
} from '@/api/channel/wxShop'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { BillSource, WxAftersaleErpStatus, WxAftersaleStatus, WxAftersaleType, WxSyncType } from '@/constants/enums'
import { toOptions } from '@/utils/dict'
import { money } from '@/utils/number'

/**
 * 售后单：同意 / 拒绝、退货退款确认收货（调用小店接口）。退款完成后：发货前的退款不再出库（全部退款时关闭销售订单），
 * 发货后的退货退款生成销售退货入库；退款金额只记录，资金由小店结算
 */
const { t } = useI18n()
const router = useRouter()
const dict = useDict('ch_wx_aftersale_type', 'ch_wx_aftersale_status', 'ch_wx_aftersale_erp_status')

const columns = [
  { title: t('channel.aftersale.aftersaleId'), key: 'aftersaleId', width: 180 },
  { title: t('channel.aftersale.order'), key: 'order', width: 200 },
  { title: t('channel.order.goods'), key: 'goods', width: 200 },
  { title: t('channel.aftersale.type'), key: 'type', width: 100, dict: 'ch_wx_aftersale_type', dataIndex: 'type' },
  { title: t('channel.aftersale.refundAmount'), key: 'refundAmount', width: 100, align: 'right' },
  { title: t('channel.aftersale.reason'), dataIndex: 'reason', width: 150, ellipsis: true },
  { title: t('channel.aftersale.status'), key: 'status', width: 170, dict: 'ch_wx_aftersale_status', dataIndex: 'status' },
  { title: t('channel.aftersale.erpStatus'), key: 'erpStatus', width: 160, dict: 'ch_wx_aftersale_erp_status', dataIndex: 'erpStatus' },
  actionColumn(3, [t('channel.aftersale.accept'), t('channel.aftersale.reject')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'channel-wx-aftersale')
const showSearch = ref(true)

const emptyQuery = () => ({ shopId: undefined, keyword: undefined, type: undefined, status: undefined, erpStatus: undefined, dateRange: undefined })
const query = reactive(emptyQuery())
const fetcher = ({ dateRange, ...params }) => pageWxAftersale({ ...params, beginDate: dateRange?.[0], endDate: dateRange?.[1] })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(fetcher, query)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

onMounted(load)

const openOrder = (record) => router.push({ path: '/channel/wx-order', query: { id: record.wxOrderId } })

const reload = () => load()
const accept = useAction((a) => acceptWxAftersale(a.id), {
  confirm: (a) => ({ title: t('channel.aftersale.confirmAccept'), content: a.type === WxAftersaleType.RETURN ? t('channel.aftersale.acceptReturnTip') : t('channel.aftersale.acceptRefundTip') }),
  reload
})
const confirmReceive = useAction((a) => confirmWxAftersale(a.id), {
  confirm: () => ({ title: t('channel.aftersale.confirmReceive'), content: t('channel.aftersale.confirmReceiveTip') }),
  reload
})
const refresh = useAction((a) => refreshWxAftersale(a.id), { successKey: 'channel.order.refreshed', reload })
const retry = useAction((a) => retryWxAftersale(a.id), { reload })

const rowActions = (record) => [
  {
    key: 'accept',
    icon: CheckCircleOutlined,
    label: t('channel.aftersale.accept'),
    perm: 'channel:wxAftersale:handle',
    show: record.status === WxAftersaleStatus.PENDING,
    onClick: () => accept(record)
  },
  {
    key: 'reject',
    icon: CloseCircleOutlined,
    label: t('channel.aftersale.reject'),
    perm: 'channel:wxAftersale:handle',
    danger: true,
    show: record.status === WxAftersaleStatus.PENDING,
    onClick: () => openReject(record)
  },
  {
    key: 'receive',
    icon: InboxOutlined,
    label: t('channel.aftersale.receive'),
    perm: 'channel:wxAftersale:handle',
    show: record.type === WxAftersaleType.RETURN && record.status === WxAftersaleStatus.WAIT_RECEIPT,
    onClick: () => confirmReceive(record)
  },
  {
    key: 'retry',
    icon: RedoOutlined,
    label: t('channel.aftersale.retry'),
    perm: 'channel:wxAftersale:handle',
    show: record.erpStatus === WxAftersaleErpStatus.FAILED,
    onClick: () => retry(record)
  },
  { key: 'refresh', icon: SyncOutlined, label: t('channel.order.refresh'), perm: 'channel:wxAftersale:handle', more: true, onClick: () => refresh(record) }
]

// ---------- 拒绝 ----------
const rejectOpen = ref(false)
const rejecting = ref(false)
const rejectTarget = ref()
const rejectReason = ref('')

function openReject(record) {
  rejectTarget.value = record
  rejectReason.value = ''
  rejectOpen.value = true
}

async function submitReject() {
  if (!rejectReason.value.trim()) {
    message.warning(t('channel.aftersale.inputReason'))
    return
  }
  rejecting.value = true
  try {
    await rejectWxAftersale(rejectTarget.value.id, rejectReason.value.trim())
    message.success(t('common.msg.operationSuccess'))
    rejectOpen.value = false
    load()
  } finally {
    rejecting.value = false
  }
}

const syncing = ref(false)
async function syncAftersales() {
  syncing.value = true
  try {
    const r = await syncWxShop(query.shopId, WxSyncType.AFTERSALE)
    const content = t('channel.shop.syncResult', { total: r.total, succeeded: r.succeeded, failed: r.failed })
    if (r.success && !r.failed) message.success(content)
    else Modal.warning({ title: content, content: r.message || '' })
    load()
  } finally {
    syncing.value = false
  }
}
</script>

<style scoped>
.tip {
  margin-bottom: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.note-icon {
  margin-left: 4px;
  color: var(--app-text-3);
}
</style>
