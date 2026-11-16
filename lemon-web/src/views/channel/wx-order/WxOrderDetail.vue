<template>
  <SubPage :open="open" :title="detail ? $t('channel.order.detailTitle', { no: detail.orderId }) : ''" @close="open = false">
    <template v-if="detail" #extra>
      <a-space>
        <a-button v-perm="'channel:wxOrder:ship'" @click="refresh(detail)">{{ $t('channel.order.refresh') }}</a-button>
        <a-button v-if="canRetry(detail)" v-perm="'channel:wxOrder:ship'" @click="retry(detail)">{{ retryLabel(detail) }}</a-button>
        <a-button v-if="canPush(detail)" v-perm="'channel:wxOrder:ship'" @click="push(detail)">{{ $t('channel.order.pushDelivery') }}</a-button>
        <a-button v-if="canShip(detail)" v-perm="'channel:wxOrder:ship'" type="primary" @click="emit('ship', detail)">
          {{ $t('channel.order.ship') }}
        </a-button>
      </a-space>
    </template>
    <template v-if="detail">
      <a-alert v-if="detail.errorMsg" type="error" show-icon class="block" :message="detail.errorMsg" />
      <a-alert v-else-if="detail.erpStatus === WxErpStatus.UNMAPPED" type="warning" show-icon class="block" :message="$t('channel.order.unmappedTip')" />

      <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
        <a-descriptions-item :label="$t('channel.common.shop')">{{ detail.shopName }}</a-descriptions-item>
        <a-descriptions-item :label="$t('channel.common.store')">{{ detail.storeName }}</a-descriptions-item>
        <a-descriptions-item :label="$t('channel.order.buyer')">{{ detail.buyer || '-' }}</a-descriptions-item>
        <a-descriptions-item :label="$t('channel.order.status')"><DictTag type="ch_wx_order_status" :value="detail.status" /></a-descriptions-item>
        <a-descriptions-item :label="$t('channel.order.orderTime')">{{ detail.orderTime || '-' }}</a-descriptions-item>
        <a-descriptions-item :label="$t('channel.order.payTime')">{{ detail.payTime || '-' }}</a-descriptions-item>
        <a-descriptions-item :label="$t('channel.order.goodsAmount')">{{ money(detail.goodsAmount) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('channel.order.freightDiscount')">{{ money(detail.freight) }} / {{ money(detail.discountAmount) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('channel.order.paidAmount')"><b>{{ money(detail.paidAmount) }}</b></a-descriptions-item>
        <a-descriptions-item :label="$t('channel.order.receiver')" :span="3">
          {{ [detail.receiverName, detail.receiverPhone, detail.receiverAddress].filter(Boolean).join('  ') || '-' }}
          <a-tag v-if="!detail.sensitiveVisible" class="masked-tag">{{ $t('channel.order.masked') }}</a-tag>
        </a-descriptions-item>
      </a-descriptions>

      <a-descriptions :title="$t('channel.order.erpSection')" :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
        <a-descriptions-item :label="$t('channel.order.erpStatus')"><DictTag type="ch_wx_erp_status" :value="detail.erpStatus" /></a-descriptions-item>
        <a-descriptions-item :label="$t('channel.order.erpOrder')">
          <a v-if="detail.erpOrderId && canViewSales" @click="openSalesOrder(detail.erpOrderId)">{{ detail.erpOrderNo }}</a>
          <span v-else>{{ detail.erpOrderNo || '-' }}</span>
        </a-descriptions-item>
        <a-descriptions-item :label="$t('channel.order.salesOut')">
          <SourceDocLink :type="BillSource.SALES_OUT" :id="detail.salesOutId" :no="detail.salesOutNo" />
        </a-descriptions-item>
        <a-descriptions-item :label="$t('channel.order.deliveryStatus')"><DictTag type="ch_wx_delivery_status" :value="detail.deliveryStatus" /></a-descriptions-item>
        <a-descriptions-item :label="$t('channel.order.delivery')">
          {{ [detail.deliveryCompanyName || detail.deliveryCompanyId, detail.waybillNo].filter(Boolean).join(' ') || '-' }}
        </a-descriptions-item>
        <a-descriptions-item :label="$t('channel.order.shipTime')">{{ detail.shipTime || '-' }}</a-descriptions-item>
      </a-descriptions>

      <div class="section-title">{{ $t('channel.order.items') }}</div>
      <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="false" :scroll="{ x: 860 }" class="block">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'">
            <div>{{ record.title }}</div>
            <div class="sub-text">{{ record.skuAttrs || '' }}</div>
          </template>
          <template v-else-if="column.key === 'material'">
            <span v-if="record.materialCode">{{ record.materialCode }} {{ record.materialName }}</span>
            <a-tag v-else color="volcano">{{ $t('channel.product.unmapped') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'salePrice'">{{ money(record.salePrice) }}</template>
          <template v-else-if="column.key === 'paidAmount'">{{ money(record.paidAmount) }}</template>
        </template>
      </a-table>

      <template v-if="detail.aftersales?.length">
        <div class="section-title">{{ $t('channel.order.aftersales') }}</div>
        <a-table row-key="id" size="small" :columns="aftersaleColumns" :data-source="detail.aftersales" :pagination="false" :scroll="{ x: 860 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'type'"><DictTag type="ch_wx_aftersale_type" :value="record.type" /></template>
            <template v-else-if="column.key === 'status'"><DictTag type="ch_wx_aftersale_status" :value="record.status" /></template>
            <template v-else-if="column.key === 'erpStatus'"><DictTag type="ch_wx_aftersale_erp_status" :value="record.erpStatus" /></template>
            <template v-else-if="column.key === 'refundAmount'">{{ money(record.refundAmount) }}</template>
            <template v-else-if="column.key === 'salesReturn'">
              <SourceDocLink :type="BillSource.SALES_RETURN" :id="record.salesReturnId" :no="record.salesReturnNo" />
            </template>
          </template>
        </a-table>
      </template>
    </template>
  </SubPage>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import SourceDocLink from '@/components/common/SourceDocLink.vue'
import { getWxOrder } from '@/api/channel/wxShop'
import { useDict } from '@/composables/useDict'
import { BillSource, WxErpStatus } from '@/constants/enums'
import { useUserStore } from '@/store/user'
import { money } from '@/utils/number'
import { canPush, canRetry, canShip, useOrderActions } from './useOrderActions'

/** 渠道订单详情：商品明细（映射的物料）、ERP 单据（销售订单、销售出库、退货）、发货回传、售后单；发货、重试 */
const emit = defineEmits(['ship', 'changed'])
const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
useDict('ch_wx_order_status', 'ch_wx_erp_status', 'ch_wx_delivery_status', 'ch_wx_aftersale_type', 'ch_wx_aftersale_status', 'ch_wx_aftersale_erp_status')

const open = ref(false)
const detail = ref()
const canViewSales = computed(() => userStore.hasPerm('scm:sales:list'))

const itemColumns = computed(() => [
  { title: t('channel.order.goods'), key: 'title', width: 260 },
  { title: t('channel.product.material'), key: 'material', width: 220 },
  { title: t('channel.order.qty'), dataIndex: 'qty', width: 70, align: 'right' },
  { title: t('channel.order.refundedQty'), dataIndex: 'refundedQty', width: 90, align: 'right' },
  { title: t('channel.order.salePrice'), key: 'salePrice', width: 100, align: 'right' },
  { title: t('channel.order.linePaid'), key: 'paidAmount', width: 110, align: 'right' }
])
const aftersaleColumns = computed(() => [
  { title: t('channel.aftersale.aftersaleId'), dataIndex: 'aftersaleId', width: 170 },
  { title: t('channel.aftersale.type'), key: 'type', width: 100 },
  { title: t('channel.aftersale.status'), key: 'status', width: 110 },
  { title: t('channel.order.qty'), dataIndex: 'qty', width: 70, align: 'right' },
  { title: t('channel.aftersale.refundAmount'), key: 'refundAmount', width: 100, align: 'right' },
  { title: t('channel.aftersale.erpStatus'), key: 'erpStatus', width: 100 },
  { title: t('channel.aftersale.salesReturn'), key: 'salesReturn', width: 150 }
])

const retryLabel = (o) => (o.erpOrderId ? t('channel.order.reprocess') : t('channel.order.createErpOrder'))

function openSalesOrder(id) {
  router.push({ path: '/scm/sales', query: { id } })
}

async function show(id) {
  detail.value = await getWxOrder(id)
  open.value = true
}
const reload = () => {
  emit('changed')
  return detail.value ? show(detail.value.id) : undefined
}
const { retry, refresh, push } = useOrderActions(reload)

defineExpose({ open: show, reload, isOpen: (id) => open.value && detail.value?.id === id })
</script>

<style scoped>
.block {
  margin-bottom: 12px;
}
.section-title {
  margin: 4px 0 8px;
  font-weight: 600;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.masked-tag {
  margin-left: 8px;
}
</style>
