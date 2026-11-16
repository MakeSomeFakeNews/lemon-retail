<template>
  <SubPage :open="open" :title="member ? $t('pos.member.detailTitle', { name: member.name || member.memberCode }) : ''" @close="open = false">
    <template v-if="member">
      <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
        <a-descriptions-item :label="$t('pos.member.memberCode')">{{ member.memberCode }}</a-descriptions-item>
        <a-descriptions-item :label="$t('pos.member.phone')">{{ member.phone }}</a-descriptions-item>
        <a-descriptions-item :label="$t('pos.member.level')">{{ member.levelName || '-' }}</a-descriptions-item>
        <a-descriptions-item :label="$t('pos.member.gender')"><DictTag type="pos_member_gender" :value="member.gender" /></a-descriptions-item>
        <a-descriptions-item :label="$t('pos.member.birthday')">{{ member.birthday || '-' }}</a-descriptions-item>
        <a-descriptions-item :label="$t('pos.member.store')">{{ member.storeName || '-' }}</a-descriptions-item>
        <a-descriptions-item :label="$t('pos.member.totalAmount')">{{ money(member.totalAmount) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('pos.member.visitCount')">{{ member.visitCount }}</a-descriptions-item>
        <a-descriptions-item :label="$t('pos.member.lastVisit')">{{ member.lastVisitTime || '-' }}</a-descriptions-item>
        <a-descriptions-item :label="$t('pos.member.points')">{{ money(member.points) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('pos.member.balance')">{{ money(member.balance) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('common.field.createTime')">{{ member.createTime }}</a-descriptions-item>
      </a-descriptions>
      <div class="section-title">{{ $t('pos.member.consumption') }}</div>
      <a-table row-key="id" size="small" :columns="columns" :data-source="sales" :loading="loading" :pagination="pagination" @change="onChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'kind'"><DictTag type="pos_sale_kind" :value="record.kind" /></template>
          <template v-else-if="column.key === 'status'"><DictTag type="pos_sale_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'payableAmount'">{{ money(record.payableAmount) }}</template>
        </template>
      </a-table>
    </template>
  </SubPage>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import { getMember, memberSales } from '@/api/pos'
import { useDict } from '@/composables/useDict'
import { money } from '@/utils/number'

/** 会员详情：资料、累计消费，消费记录（POS 单据，按门店范围） */
const { t } = useI18n()
useDict('pos_member_gender', 'pos_sale_kind', 'pos_sale_status')

const open = ref(false)
const member = ref()
const sales = ref([])
const loading = ref(false)
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const columns = computed(() => [
  { title: t('pos.sale.saleNo'), dataIndex: 'saleNo', width: 190 },
  { title: t('pos.sale.kind'), key: 'kind', width: 70 },
  { title: t('common.field.status'), key: 'status', width: 80 },
  { title: t('pos.sale.saleTime'), dataIndex: 'saleTime', width: 160 },
  { title: t('store.common.store'), dataIndex: 'storeName', ellipsis: true },
  { title: t('pos.sale.payable'), key: 'payableAmount', width: 110, align: 'right' }
])

async function show(id) {
  member.value = await getMember(id)
  pagination.current = 1
  open.value = true
  loadSales()
}

async function loadSales() {
  loading.value = true
  try {
    const res = await memberSales(member.value.id, { current: pagination.current, size: pagination.pageSize })
    sales.value = res.records
    pagination.total = res.total
  } finally {
    loading.value = false
  }
}

function onChange(p) {
  pagination.current = p.current
  loadSales()
}

defineExpose({ open: show })
</script>

<style scoped>
.block {
  margin-bottom: 12px;
}
.section-title {
  margin: 12px 0 8px;
  font-weight: 500;
}
</style>
