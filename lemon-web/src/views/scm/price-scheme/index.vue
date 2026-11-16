<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('scm.priceScheme.scheme')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('scm.priceScheme.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.sys_common_status, true)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :exporter="exportAll"
        :columns="columns"
        @refresh="load"
      >
        <a-button v-perm="'scm:priceScheme:add'" type="primary" @click="formRef.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('scm.priceScheme.add') }}
        </a-button>
        <a-alert type="info" show-icon :message="$t('scm.priceScheme.tip')" />
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
            <a @click="formRef.openEdit(record)">{{ record.name }}</a>
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusSwitch
              :value="record.status"
              :name="record.name"
              :api="(s) => changePriceSchemeStatus(record.id, s)"
              :disabled="!canEdit"
              @changed="(s) => (record.status = s)"
            />
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <SchemeForm ref="formRef" @saved="load" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { changePriceSchemeStatus, deletePriceScheme, pagePriceScheme } from '@/api/scm/priceScheme'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import SchemeForm from './SchemeForm.vue'

/**
 * 门店价格方案：一组商品的零售价、会员价覆盖，门店绑定方案后（门店管理中选择），该门店仓库开单时方案价优先于总部零售价。
 * 方案价可在这里直接维护，需要定时、留痕的调整用调价单
 */
const { t } = useI18n()
const userStore = useUserStore()
const canEdit = computed(() => userStore.hasPerm('scm:priceScheme:edit'))
const dict = useDict('sys_common_status')

const columns = [
  { title: t('common.field.code'), dataIndex: 'code', width: 150 },
  { title: t('common.field.name'), key: 'name', dataIndex: 'name', width: 200, ellipsis: true },
  { title: t('scm.priceScheme.itemCount'), dataIndex: 'itemCount', width: 100, align: 'right' },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 90, dict: 'sys_common_status' },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 220, ellipsis: true },
  { title: t('common.field.updateTime'), dataIndex: 'updateTime', width: 170 },
  actionColumn(2)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'scm-price-scheme')
const showSearch = ref(true)
const query = reactive({ keyword: undefined, status: undefined })
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pagePriceScheme, query)
onMounted(load)

function resetQuery() {
  Object.assign(query, { keyword: undefined, status: undefined })
  search()
}

const formRef = ref()
const remove = useAction(deletePriceScheme, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
const rowActions = (record) => [
  { type: 'edit', perm: 'scm:priceScheme:edit', onClick: () => formRef.value.openEdit(record) },
  { type: 'delete', perm: 'scm:priceScheme:remove', confirm: t('scm.priceScheme.confirmDelete', { name: record.name }), onClick: () => remove(record.id) }
]
</script>
