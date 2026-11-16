<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('store.common.store')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('store.store.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('store.store.type')" name="type">
          <a-select v-model:value="query.type" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.store_type)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.store_status)" />
        </a-form-item>
        <a-form-item :label="$t('store.common.region')" name="regionId">
          <a-tree-select
            v-model:value="query.regionId"
            :tree-data="regions"
            :field-names="{ label: 'name', value: 'id', children: 'children' }"
            :placeholder="$t('common.placeholder.all')"
            tree-default-expand-all
            show-search
            tree-node-filter-prop="name"
            allow-clear
          />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll"
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :columns="columns"
        @refresh="load"
      >
        <a-button v-perm="'store:store:add'" type="primary" @click="storeForm.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('store.store.createTitle') }}
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
            <div>{{ record.name }}<span v-if="record.shortName" class="sub-text"> ({{ record.shortName }})</span></div>
            <div class="sub-text">{{ record.code }}</div>
          </template>
          <template v-else-if="column.key === 'type'">
            <DictTag type="store_type" :value="record.type" />
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="store_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'contact'">
            <div>{{ record.contact || '-' }}</div>
            <div v-if="record.phone" class="sub-text">{{ record.phone }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <StoreFormDrawer ref="storeForm" @success="load" />

    <!-- 修改状态 -->
    <a-modal
      v-model:open="statusOpen"
      :title="$t('store.store.changeStatusTitle', { name: statusTarget?.name })"
      :confirm-loading="statusSaving"
      destroy-on-close
      @ok="submitStatus"
    >
      <a-radio-group v-model:value="statusValue" class="status-group">
        <a-radio v-for="o in statusOptions" :key="o.value" :value="o.value" :disabled="o.disabled">{{ o.label }}</a-radio>
      </a-radio-group>
      <a-alert type="info" show-icon :message="$t('store.store.statusTip')" />
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { PlusOutlined, SwapOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import StoreFormDrawer from './components/StoreFormDrawer.vue'
import { regionTree } from '@/api/store/region'
import { changeStoreStatus, deleteStore, pageStore } from '@/api/store/store'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { StoreStatus, StoreType } from '@/constants/enums'
import { toOptions } from '@/utils/dict'

const { t } = useI18n()
const dict = useDict('store_type', 'store_status')

const columns = [
  { title: t('store.common.store'), key: 'name', width: 220, exportColumns: [
    { title: t('store.store.code'), dataIndex: 'code' },
    { title: t('store.store.name'), dataIndex: 'name' },
    { title: t('store.store.shortName'), dataIndex: 'shortName' }
  ] },
  { title: t('store.store.type'), key: 'type', width: 100, dict: 'store_type', dataIndex: 'type' },
  { title: t('store.common.region'), dataIndex: 'regionName', width: 140, ellipsis: true },
  { title: t('store.store.supplyStore'), dataIndex: 'supplyStoreName', width: 160, ellipsis: true },
  { title: t('common.field.status'), key: 'status', width: 90, dict: 'store_status', dataIndex: 'status' },
  { title: t('store.store.contactPhone'), key: 'contact', width: 140, exportColumns: [
    { title: t('store.store.contact'), dataIndex: 'contact' },
    { title: t('store.store.phone'), dataIndex: 'phone' }
  ] },
  { title: t('store.store.address'), dataIndex: 'address', width: 220, ellipsis: true },
  { title: t('common.field.sort'), dataIndex: 'sort', width: 70 },
  actionColumn(3, [t('store.store.changeStatus')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'store-store')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, type: undefined, status: undefined, regionId: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageStore, query)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const regions = ref([])
onMounted(async () => {
  load()
  regions.value = await regionTree()
})

const storeForm = ref()

const rowActions = (record) => [
  { type: 'edit', perm: 'store:store:edit', onClick: () => storeForm.value.openEdit(record) },
  { key: 'status', icon: SwapOutlined, label: t('store.store.changeStatus'), perm: 'store:store:edit', onClick: () => openStatus(record) },
  {
    type: 'delete',
    perm: 'store:store:remove',
    disabled: record.type === StoreType.HQ,
    confirm: { title: t('store.store.confirmDelete', { name: record.name }), description: t('store.store.deleteTip') },
    onClick: () => remove(record.id)
  }
]

const remove = useAction(deleteStore, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

// ---------- 修改状态 ----------
const statusOpen = ref(false)
const statusSaving = ref(false)
const statusTarget = ref()
const statusValue = ref()
/** 总部不能停业、闭店 */
const statusOptions = computed(() =>
  toOptions(dict.store_status).map((o) => ({
    ...o,
    disabled: statusTarget.value?.type === StoreType.HQ && [StoreStatus.SUSPENDED, StoreStatus.CLOSED].includes(o.value)
  }))
)

function openStatus(record) {
  statusTarget.value = record
  statusValue.value = record.status
  statusOpen.value = true
}

async function submitStatus() {
  if (statusValue.value === statusTarget.value.status) {
    statusOpen.value = false
    return
  }
  statusSaving.value = true
  try {
    await changeStoreStatus(statusTarget.value.id, statusValue.value)
    message.success(t('common.msg.operationSuccess'))
    statusOpen.value = false
    load()
  } finally {
    statusSaving.value = false
  }
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.status-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 0;
  margin-bottom: 16px;
}
</style>
