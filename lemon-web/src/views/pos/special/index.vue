<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('pos.special.name')" name="keyword">
          <a-input v-model:value="query.keyword" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.pos_special_status)" />
        </a-form-item>
        <a-form-item :label="$t('pos.special.material')" name="materialId">
          <MaterialSelect v-model:value="query.materialId" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :exporter="exportAll" :columns="columns" @refresh="load">
        <a-button v-perm="'pos:special:add'" type="primary" @click="formRef.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('pos.special.add') }}
        </a-button>
      </TableToolbar>
      <a-table row-key="id" :size="size" :columns="visibleColumns" :data-source="list" :loading="loading" :pagination="pagination" :scroll="{ x: scrollX, y: '100%' }" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'"><a @click="view(record)">{{ record.name }}</a></template>
          <template v-else-if="column.key === 'period'">{{ record.beginTime }} ~ {{ record.endTime }}</template>
          <template v-else-if="column.key === 'scope'">
            <DictTag type="pos_special_scope" :value="record.storeScope" />
            <span class="sub-text">{{ (record.targetNames || []).join(', ') }}</span>
          </template>
          <template v-else-if="column.key === 'channel'"><DictTag type="pos_sale_channel" :value="record.channel" /></template>
          <template v-else-if="column.key === 'memberOnly'">{{ record.memberOnly ? $t('pos.special.yes') : '-' }}</template>
          <template v-else-if="column.key === 'status'"><DictTag type="pos_special_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'"><TableActions :actions="rowActions(record)" /></template>
        </template>
      </a-table>
    </a-card>

    <SpecialForm ref="formRef" @saved="load" />
    <a-modal v-model:open="viewOpen" :title="viewing?.name" width="760px" :footer="null">
      <template v-if="viewing">
        <a-descriptions :column="2" size="small" bordered>
          <a-descriptions-item :label="$t('pos.special.period')">{{ viewing.beginTime }} ~ {{ viewing.endTime }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')"><DictTag type="pos_special_status" :value="viewing.status" /></a-descriptions-item>
          <a-descriptions-item :label="$t('pos.special.storeScope')" :span="2">
            <DictTag type="pos_special_scope" :value="viewing.storeScope" /> {{ (viewing.targetNames || []).join(', ') }}
          </a-descriptions-item>
        </a-descriptions>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="viewing.items" :pagination="false" class="items">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
            <template v-else-if="column.key === 'specialPrice'">{{ record.specialPrice == null ? '-' : price(record.specialPrice) }}</template>
            <template v-else-if="column.key === 'discountRate'">{{ record.discountRate == null ? '-' : `${record.discountRate}%` }}</template>
            <template v-else-if="column.key === 'limitQty'">{{ record.limitQty == null ? $t('pos.special.unlimited') : qty(record.limitQty) }}</template>
          </template>
        </a-table>
      </template>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { PlayCircleOutlined, PlusOutlined, StopOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { activateSpecial, deleteSpecial, endSpecial, getSpecial, pageSpecial } from '@/api/pos'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { price, qty } from '@/utils/number'
import { SpecialStatus } from '@/constants/enums'
import SpecialForm from './SpecialForm.vue'

/** 特价活动：草稿 → 开始（进行中，活动时间内参与 POS 取价，最优先）→ 结束；同一商品命中多个活动时取最低价 */
const { t } = useI18n()
const dict = useDict('pos_special_status', 'pos_special_scope', 'pos_sale_channel')

const columns = [
  { title: t('pos.special.name'), key: 'name', dataIndex: 'name', width: 180 },
  { title: t('pos.special.period'), key: 'period', width: 320, exportValue: (r) => `${r.beginTime} ~ ${r.endTime}` },
  { title: t('pos.special.storeScope'), key: 'scope', width: 220, exportValue: (r) => (r.targetNames || []).join(', ') },
  { title: t('pos.special.channel'), key: 'channel', dataIndex: 'channel', width: 100, dict: 'pos_sale_channel' },
  { title: t('pos.special.memberOnly'), key: 'memberOnly', width: 90 },
  { title: t('pos.special.itemCount'), dataIndex: 'itemCount', width: 80, align: 'right' },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 90, dict: 'pos_special_status' },
  { title: t('common.field.createBy'), dataIndex: 'createByName', width: 100 },
  actionColumn(4)
]
const itemColumns = computed(() => [
  { title: t('pos.special.material'), key: 'material' },
  { title: t('pos.special.specialPrice'), key: 'specialPrice', width: 100, align: 'right' },
  { title: t('pos.special.discountRate'), key: 'discountRate', width: 100, align: 'right' },
  { title: t('pos.special.limitQty'), key: 'limitQty', width: 100, align: 'right' }
])
const { checked, visibleColumns, scrollX } = useColumns(columns, 'pos-special')
const showSearch = ref(true)
const emptyQuery = () => ({ keyword: undefined, status: undefined, materialId: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageSpecial, query)
onMounted(load)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const formRef = ref()
const viewOpen = ref(false)
const viewing = ref()
async function view(record) {
  viewing.value = await getSpecial(record.id)
  viewOpen.value = true
}

const activate = useAction((r) => activateSpecial(r.id), { confirm: (r) => t('pos.special.confirmActivate', { name: r.name }), reload: () => load() })
const end = useAction((r) => endSpecial(r.id), { confirm: (r) => ({ title: t('pos.special.confirmEnd', { name: r.name }), okType: 'danger' }), reload: () => load() })
const remove = useAction(deleteSpecial, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
const rowActions = (record) => [
  { type: 'view', onClick: () => view(record) },
  { type: 'edit', perm: 'pos:special:edit', show: record.status === SpecialStatus.DRAFT, onClick: () => formRef.value.openEdit(record) },
  { key: 'activate', icon: PlayCircleOutlined, label: t('pos.special.activate'), perm: 'pos:special:audit', show: record.status === SpecialStatus.DRAFT, onClick: () => activate(record) },
  { key: 'end', icon: StopOutlined, label: t('pos.special.end'), danger: true, perm: 'pos:special:audit', show: record.status === SpecialStatus.ACTIVE, onClick: () => end(record) },
  { type: 'delete', perm: 'pos:special:remove', show: record.status !== SpecialStatus.ACTIVE, confirm: t('pos.special.confirmDelete', { name: record.name }), onClick: () => remove(record.id) }
]
</script>

<style scoped>
.items {
  margin-top: 12px;
}
.sub-text {
  margin-left: 4px;
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
