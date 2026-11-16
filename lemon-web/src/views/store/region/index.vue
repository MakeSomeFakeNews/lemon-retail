<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <div class="toolbar">
        <a-space wrap>
          <a-input-search v-model:value="keyword" :placeholder="$t('store.region.keywordPh')" allow-clear class="search" />
          <a-button @click="toggleExpand">{{ expandedKeys.length ? $t('store.region.collapseAll') : $t('store.region.expandAll') }}</a-button>
          <a-button @click="load">
            <template #icon><ReloadOutlined /></template>{{ $t('common.action.refresh') }}
          </a-button>
        </a-space>
        <a-button v-perm="'store:region:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('store.region.addTop') }}
        </a-button>
      </div>
      <a-table
        v-model:expanded-row-keys="expandedKeys"
        row-key="id"
        size="middle"
        :columns="columns"
        :data-source="visibleTree"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: scrollX, y: '100%' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <DictTag type="sys_common_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('store.region.createTitle')" :edit-title="$t('store.region.editTitle')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item :label="$t('store.region.parent')" name="parentId">
              <a-tree-select
                v-model:value="form.parentId"
                :tree-data="parentOptions"
                :field-names="{ label: 'name', value: 'id', children: 'children' }"
                :placeholder="$t('store.region.topLevel')"
                tree-default-expand-all
                show-search
                tree-node-filter-prop="name"
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('store.region.code')" name="code">
              <a-input v-model:value="form.code" :maxlength="32" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('store.region.name')" name="name">
              <a-input v-model:value="form.name" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.sort')" name="sort">
              <a-input-number v-model:value="form.sort" :min="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.status')" name="status">
              <a-radio-group v-model:value="form.status" :options="toOptions(dict.sys_common_status, true)" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')" name="remark">
              <a-textarea v-model:value="form.remark" :rows="3" :maxlength="255" show-count />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { addRegion, deleteRegion, getRegion, regionTree, updateRegion } from '@/api/store/region'
import { useAction } from '@/composables/useAction'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { STATUS_DISABLED, STATUS_ENABLED } from '@/utils/constants'
import { toOptions } from '@/utils/dict'
import { filterTree, mapTree, selfAndDescendantIds } from '@/utils/tree'

const { t } = useI18n()
const dict = useDict('sys_common_status')

const columns = [
  { title: t('store.region.name'), dataIndex: 'name', width: 260 },
  { title: t('store.region.code'), dataIndex: 'code', width: 160 },
  { title: t('common.field.sort'), dataIndex: 'sort', width: 80 },
  { title: t('common.field.status'), key: 'status', width: 90 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 200, ellipsis: true },
  actionColumn(3)
]
const scrollX = columns.reduce((sum, c) => sum + c.width, 0)

const keyword = ref('')
const tree = ref([])
const loading = ref(false)
const expandedKeys = ref([])


/** 按编码、名称过滤，保留命中节点的上级 */
const visibleTree = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return tree.value
  return filterTree(tree.value, (n) => n.name.toLowerCase().includes(kw) || n.code.toLowerCase().includes(kw))
})

const parentKeys = (nodes) => nodes.flatMap((n) => (n.children ? [n.id, ...parentKeys(n.children)] : []))

async function load() {
  loading.value = true
  try {
    tree.value = await regionTree()
    expandedKeys.value = parentKeys(tree.value)
  } finally {
    loading.value = false
  }
}
function toggleExpand() {
  expandedKeys.value = expandedKeys.value.length ? [] : parentKeys(visibleTree.value)
}
onMounted(load)

const rowActions = (record) => [
  { type: 'edit', perm: 'store:region:edit', onClick: () => drawer.openEdit(record) },
  { type: 'add', label: t('store.region.addChild'), perm: 'store:region:add', onClick: () => drawer.openCreate({ parentId: record.id }) },
  {
    type: 'delete',
    perm: 'store:region:remove',
    confirm: { title: t('store.region.confirmDelete', { name: record.name }), description: t('store.region.deleteTip') },
    onClick: () => remove(record.id)
  }
]

const remove = useAction(deleteRegion, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

// ---------- 表单 ----------
const rules = {
  code: [
    { required: true, whitespace: true, message: t('store.region.inputCode') },
    { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: t('store.region.codeRule') }
  ],
  name: [{ required: true, whitespace: true, message: t('store.region.inputName') }]
}

const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, parentId: undefined, code: '', name: '', sort: 0, status: STATUS_ENABLED, remark: '' }),
  // 一级区域的上级为 0，表单中显示为空
  fetch: async (record) => {
    const region = await getRegion(record.id)
    return { ...region, parentId: region.parentId || undefined }
  },
  create: addRegion,
  update: updateRegion,
  toPayload: ({ parentId, code, name, sort, status, remark }) => ({ parentId: parentId || 0, code, name, sort, status, remark }),
  onSuccess: load
})
const { form, formRef } = drawer

/** 上级区域：不能选自身及其下级，停用的区域不能选 */
const parentOptions = computed(() => {
  const excluded = form.value.id ? new Set(selfAndDescendantIds(tree.value, form.value.id)) : new Set()
  return mapTree(
    filterTree(tree.value, (n) => !excluded.has(n.id)),
    (n) => ({ id: n.id, name: n.name, disabled: n.status === STATUS_DISABLED })
  ).filter((n) => !excluded.has(n.id))
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}
.search {
  width: 240px;
}
.full {
  width: 100%;
}
</style>
