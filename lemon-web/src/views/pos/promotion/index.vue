<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('pos.promotion.name')" name="keyword">
          <a-input v-model:value="query.keyword" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('pos.promotion.kind')" name="kind">
          <a-select v-model:value="query.kind" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.pos_promotion_kind)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.pos_special_status)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :exporter="exportAll" :columns="columns" @refresh="load">
        <a-button v-perm="'pos:promotion:add'" type="primary" @click="formRef.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('pos.promotion.add') }}
        </a-button>
        <span class="sub-text">{{ $t('pos.promotion.orderTip') }}</span>
      </TableToolbar>
      <a-table row-key="id" :size="size" :columns="visibleColumns" :data-source="list" :loading="loading" :pagination="pagination" :scroll="{ x: scrollX, y: '100%' }" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'"><a @click="view(record)">{{ record.name }}</a></template>
          <template v-else-if="column.key === 'kind'"><DictTag type="pos_promotion_kind" :value="record.kind" /></template>
          <template v-else-if="column.key === 'rule'">{{ ruleText(record) }}</template>
          <template v-else-if="column.key === 'period'">{{ record.beginTime }} ~ {{ record.endTime }}</template>
          <template v-else-if="column.key === 'scope'">
            <DictTag type="pos_special_scope" :value="record.storeScope" />
            <span class="sub-text">{{ (record.targetNames || []).join(', ') }}</span>
          </template>
          <template v-else-if="column.key === 'goodsScope'"><DictTag type="pos_promotion_range" :value="record.goodsScope" /></template>
          <template v-else-if="column.key === 'memberOnly'">{{ record.memberOnly ? $t('pos.special.yes') : '-' }}</template>
          <template v-else-if="column.key === 'status'"><DictTag type="pos_special_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'"><TableActions :actions="rowActions(record)" /></template>
        </template>
      </a-table>
    </a-card>

    <PromotionForm ref="formRef" @saved="load" />
    <a-modal v-model:open="viewOpen" :title="viewing?.name" width="760px" :footer="null">
      <template v-if="viewing">
        <a-descriptions :column="2" size="small" bordered>
          <a-descriptions-item :label="$t('pos.promotion.kind')"><DictTag type="pos_promotion_kind" :value="viewing.kind" /></a-descriptions-item>
          <a-descriptions-item :label="$t('pos.promotion.rule')">{{ ruleText(viewing) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('pos.special.period')">{{ viewing.beginTime }} ~ {{ viewing.endTime }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')"><DictTag type="pos_special_status" :value="viewing.status" /></a-descriptions-item>
          <a-descriptions-item :label="$t('pos.special.storeScope')" :span="2">
            <DictTag type="pos_special_scope" :value="viewing.storeScope" /> {{ (viewing.targetNames || []).join(', ') }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('pos.promotion.goodsScope')" :span="2">
            <DictTag type="pos_promotion_range" :value="viewing.goodsScope" />
            {{ (viewing.goods || []).map((g) => (g.materialId ? `${g.materialCode} ${g.materialName}` : categoryPath(g.categoryCode))).join(', ') }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('pos.promotion.excludeSpecial')">{{ viewing.excludeSpecial ? $t('pos.special.yes') : '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('pos.promotion.priority')">{{ viewing.priority }}</a-descriptions-item>
          <a-descriptions-item v-if="viewing.gifts?.length" :label="$t('pos.promotion.gifts')" :span="2">
            {{ viewing.gifts.map((g) => `${g.materialCode} ${g.materialName} × ${qty(g.qty)} ${g.unit || ''}`).join(', ') }}
          </a-descriptions-item>
        </a-descriptions>
      </template>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { PlayCircleOutlined, PlusOutlined, StopOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { activatePromotion, deletePromotion, endPromotion, getPromotion, pagePromotion } from '@/api/pos'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { money, qty } from '@/utils/number'
import { PromotionKind, PromotionThreshold, SpecialStatus } from '@/constants/enums'
import { dictLabel } from '../pos'
import PromotionForm from './PromotionForm.vue'

/**
 * 满减 / 满折 / 满赠：草稿 → 开始（进行中）→ 结束。收银时在特价（行级取价）之后按整单计算，每单只应用一个（先比优先级，再比优惠金额）
 */
const { t } = useI18n()
const dict = useDict('pos_special_status', 'pos_special_scope', 'pos_promotion_kind', 'pos_promotion_threshold', 'pos_promotion_range', 'wms_material_category')
const categoryPath = (code) => dictLabel('wms_material_category', code)

/** 规则摘要：满 X 元 / 件 减 Y、打 Z 折、赠商品（每满） */
function ruleText(r) {
  const th = r.thresholdType === PromotionThreshold.QTY ? t('pos.promotion.thresholdQty', { n: qty(r.threshold) }) : t('pos.promotion.thresholdAmount', { n: money(r.threshold) })
  const each = r.stackable ? t('pos.promotion.each') : ''
  if (r.kind === PromotionKind.REDUCE) return `${each}${th} ${t('pos.promotion.reduceN', { n: money(r.reduceAmount) })}`
  if (r.kind === PromotionKind.DISCOUNT) return `${th} ${t('pos.promotion.discountN', { n: Number(r.discountRate) })}`
  return `${each}${th} ${t('pos.promotion.giftText')}`
}

const columns = [
  { title: t('pos.promotion.name'), key: 'name', dataIndex: 'name', width: 180 },
  { title: t('pos.promotion.kind'), key: 'kind', dataIndex: 'kind', width: 80, dict: 'pos_promotion_kind' },
  { title: t('pos.promotion.rule'), key: 'rule', width: 200, exportValue: (r) => ruleText(r) },
  { title: t('pos.special.period'), key: 'period', width: 320, exportValue: (r) => `${r.beginTime} ~ ${r.endTime}` },
  { title: t('pos.special.storeScope'), key: 'scope', width: 200, exportValue: (r) => (r.targetNames || []).join(', ') },
  { title: t('pos.promotion.goodsScope'), key: 'goodsScope', dataIndex: 'goodsScope', width: 100, dict: 'pos_promotion_range' },
  { title: t('pos.promotion.priority'), dataIndex: 'priority', width: 80, align: 'right' },
  { title: t('pos.special.memberOnly'), key: 'memberOnly', width: 90 },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 90, dict: 'pos_special_status' },
  { title: t('common.field.createBy'), dataIndex: 'createByName', width: 100 },
  actionColumn(4)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'pos-promotion')
const showSearch = ref(true)
const emptyQuery = () => ({ keyword: undefined, kind: undefined, status: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pagePromotion, query)
onMounted(load)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const formRef = ref()
const viewOpen = ref(false)
const viewing = ref()
async function view(record) {
  viewing.value = await getPromotion(record.id)
  viewOpen.value = true
}

const activate = useAction((r) => activatePromotion(r.id), { confirm: (r) => t('pos.special.confirmActivate', { name: r.name }), reload: () => load() })
const end = useAction((r) => endPromotion(r.id), { confirm: (r) => ({ title: t('pos.special.confirmEnd', { name: r.name }), okType: 'danger' }), reload: () => load() })
const remove = useAction(deletePromotion, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
const rowActions = (record) => [
  { type: 'view', onClick: () => view(record) },
  { type: 'edit', perm: 'pos:promotion:edit', show: record.status === SpecialStatus.DRAFT, onClick: () => formRef.value.openEdit(record) },
  { key: 'activate', icon: PlayCircleOutlined, label: t('pos.special.activate'), perm: 'pos:promotion:audit', show: record.status === SpecialStatus.DRAFT, onClick: () => activate(record) },
  { key: 'end', icon: StopOutlined, label: t('pos.special.end'), danger: true, perm: 'pos:promotion:audit', show: record.status === SpecialStatus.ACTIVE, onClick: () => end(record) },
  { type: 'delete', perm: 'pos:promotion:remove', show: record.status !== SpecialStatus.ACTIVE, confirm: t('pos.special.confirmDelete', { name: record.name }), onClick: () => remove(record.id) }
]
</script>

<style scoped>
.sub-text {
  margin-left: 4px;
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
