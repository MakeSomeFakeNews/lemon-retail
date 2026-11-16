<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('pos.member.member')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('pos.member.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('pos.member.level')" name="levelId">
          <a-select v-model:value="query.levelId" :placeholder="$t('common.placeholder.all')" allow-clear :options="levelOptions" />
        </a-form-item>
        <a-form-item :label="$t('pos.member.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" all :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.sys_common_status, true)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :exporter="exportAll" :columns="columns" @refresh="load">
        <a-button v-perm="'pos:member:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('pos.member.add') }}
        </a-button>
      </TableToolbar>
      <a-table row-key="id" :size="size" :columns="visibleColumns" :data-source="list" :loading="loading" :pagination="pagination" :scroll="{ x: scrollX, y: '100%' }" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'member'">
            <a @click="detailRef.open(record.id)">{{ record.name || '-' }}</a>
            <div class="sub-text">{{ record.memberCode }}</div>
          </template>
          <template v-else-if="column.key === 'gender'"><DictTag type="pos_member_gender" :value="record.gender" /></template>
          <template v-else-if="column.key === 'totalAmount'">{{ money(record.totalAmount) }}</template>
          <template v-else-if="column.key === 'status'">
            <StatusSwitch :value="record.status" :name="record.name || record.memberCode" :api="(s) => changeMemberStatus(record.id, s)" :disabled="!userStore.hasPerm('pos:member:edit')" @changed="load" />
          </template>
          <template v-else-if="column.key === 'action'"><TableActions :actions="rowActions(record)" /></template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('pos.member.add')" :edit-title="$t('pos.member.edit')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('pos.member.phone')" name="phone" :extra="isMasked ? $t('pos.member.maskedTip') : undefined">
              <a-input v-model:value="form.phone" :maxlength="20" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('pos.member.name')" name="name">
              <a-input v-model:value="form.name" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('pos.member.memberCode')" name="memberCode">
              <a-input v-model:value="form.memberCode" :maxlength="32" :placeholder="$t('pos.member.codePh')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('pos.member.level')" name="levelId">
              <a-select v-model:value="form.levelId" allow-clear :options="levelOptions" :placeholder="$t('pos.member.defaultLevel')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('pos.member.gender')" name="gender">
              <a-radio-group v-model:value="form.gender" :options="toOptions(dict.pos_member_gender)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('pos.member.birthday')" name="birthday">
              <a-date-picker v-model:value="form.birthday" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('pos.member.store')" name="storeId">
              <StoreSelect v-model:value="form.storeId" all />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')" name="remark">
              <a-input v-model:value="form.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>

    <MemberDetail ref="detailRef" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SearchForm from '@/components/SearchForm.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import StoreSelect from '@/components/store/StoreSelect.vue'
import { addMember, changeMemberStatus, deleteMember, getMember, listMemberLevel, pageMember, updateMember } from '@/api/pos'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { money } from '@/utils/number'
import { MemberGender } from '@/constants/enums'
import { useUserStore } from '@/store/user'
import MemberDetail from './MemberDetail.vue'

const userStore = useUserStore()

/** 会员：全部门店通用，手机号唯一；没有「查看会员手机号」权限时手机号脱敏（修改时保持原号码） */
const { t } = useI18n()
const dict = useDict('pos_member_gender', 'sys_common_status')
const levels = ref([])
const levelOptions = computed(() => levels.value.map((l) => ({ value: l.id, label: l.name })))

const columns = [
  { title: t('pos.member.member'), key: 'member', width: 160, exportColumns: [
    { title: t('pos.member.memberCode'), dataIndex: 'memberCode' },
    { title: t('pos.member.name'), dataIndex: 'name' }
  ] },
  { title: t('pos.member.phone'), dataIndex: 'phone', width: 130 },
  { title: t('pos.member.level'), dataIndex: 'levelName', width: 100 },
  { title: t('pos.member.gender'), key: 'gender', dataIndex: 'gender', width: 70, dict: 'pos_member_gender' },
  { title: t('pos.member.store'), dataIndex: 'storeName', width: 140, ellipsis: true },
  { title: t('pos.member.totalAmount'), key: 'totalAmount', dataIndex: 'totalAmount', width: 110, align: 'right' },
  { title: t('pos.member.visitCount'), dataIndex: 'visitCount', width: 90, align: 'right' },
  { title: t('pos.member.lastVisit'), dataIndex: 'lastVisitTime', width: 160 },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 80, dict: 'sys_common_status' },
  actionColumn(3)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'pos-member')
const showSearch = ref(true)
const emptyQuery = () => ({ keyword: undefined, levelId: undefined, storeId: undefined, status: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageMember, query)
onMounted(async () => {
  load()
  levels.value = await listMemberLevel().catch(() => [])
})

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const rules = { phone: [{ required: true, message: t('pos.member.phoneRequired') }] }
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, memberCode: '', name: '', phone: '', gender: MemberGender.UNKNOWN, birthday: undefined, levelId: undefined, storeId: undefined, remark: '' }),
  fetch: (r) => getMember(r.id),
  create: addMember,
  update: updateMember,
  toPayload: (f) => ({ memberCode: f.memberCode || null, name: f.name || null, phone: f.phone, gender: f.gender, birthday: f.birthday || null,
    levelId: f.levelId || null, storeId: f.storeId || null, remark: f.remark || null }),
  onSuccess: () => load()
})
const { formRef, form } = drawer
const isMasked = computed(() => form.value.phone?.includes('*'))

const detailRef = ref()
const remove = useAction(deleteMember, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
const rowActions = (record) => [
  { type: 'view', onClick: () => detailRef.value.open(record.id) },
  { type: 'edit', perm: 'pos:member:edit', onClick: () => drawer.openEdit(record) },
  { type: 'delete', perm: 'pos:member:remove', confirm: t('pos.member.confirmDelete', { name: record.name || record.memberCode }), onClick: () => remove(record.id) }
]
</script>

<style scoped>
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
