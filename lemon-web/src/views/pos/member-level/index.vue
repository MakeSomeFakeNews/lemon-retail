<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <div class="toolbar">
        <a-button v-perm="'pos:memberLevel:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('pos.level.add') }}
        </a-button>
        <span class="sub-text">{{ $t('pos.level.tip') }}</span>
      </div>
      <a-table row-key="id" :columns="columns" :data-source="list" :loading="loading" :pagination="false" :scroll="{ x: 800, y: '100%' }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'discountRate'">{{ Number(record.discountRate) >= 100 ? $t('pos.level.noDiscount') : `${record.discountRate}%` }}</template>
          <template v-else-if="column.key === 'upgradeAmount'">{{ record.upgradeAmount == null ? $t('pos.level.manual') : money(record.upgradeAmount) }}</template>
          <template v-else-if="column.key === 'pointsRate'">{{ record.pointsRate == null ? 1 : Number(record.pointsRate) }}</template>
          <template v-else-if="column.key === 'status'"><DictTag type="sys_common_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'"><TableActions :actions="rowActions(record)" /></template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('pos.level.add')" :edit-title="$t('pos.level.edit')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item :label="$t('pos.level.name')" name="name">
          <a-input v-model:value="form.name" :maxlength="32" />
        </a-form-item>
        <a-form-item :label="$t('pos.level.discountRate')" name="discountRate" :extra="$t('pos.level.discountTip')">
          <a-input-number v-model:value="form.discountRate" :min="1" :max="100" :precision="2" addon-after="%" class="full" />
        </a-form-item>
        <a-form-item :label="$t('pos.level.upgradeAmount')" name="upgradeAmount" :extra="$t('pos.level.upgradeTip')">
          <a-input-number v-model:value="form.upgradeAmount" :min="0" :precision="2" class="full" />
        </a-form-item>
        <a-form-item :label="$t('pos.level.pointsRate')" name="pointsRate" :extra="$t('pos.level.pointsRateTip')">
          <a-input-number v-model:value="form.pointsRate" :min="0" :max="100" :precision="2" class="full" />
        </a-form-item>
        <a-form-item :label="$t('common.field.sort')" name="sort">
          <a-input-number v-model:value="form.sort" :min="0" class="full" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-radio-group v-model:value="form.status" :options="toOptions(dict.sys_common_status, true)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.remark')" name="remark">
          <a-input v-model:value="form.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </FormDrawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { addMemberLevel, deleteMemberLevel, listMemberLevel, updateMemberLevel } from '@/api/pos'
import { useAction } from '@/composables/useAction'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { toOptions } from '@/utils/dict'
import { money } from '@/utils/number'
import { STATUS_ENABLED } from '@/utils/constants'

/** 会员等级：会员折扣（没有方案会员价的商品按 门店价 × 折扣），累计消费达到升级门槛时自动升级（只升不降） */
const { t } = useI18n()
const dict = useDict('sys_common_status')
const list = ref([])
const loading = ref(false)
const columns = [
  { title: t('pos.level.name'), dataIndex: 'name', width: 160 },
  { title: t('pos.level.discountRate'), key: 'discountRate', width: 120 },
  { title: t('pos.level.upgradeAmount'), key: 'upgradeAmount', width: 140, align: 'right' },
  { title: t('pos.level.pointsRate'), key: 'pointsRate', width: 100, align: 'right' },
  { title: t('pos.level.memberCount'), dataIndex: 'memberCount', width: 100, align: 'right' },
  { title: t('common.field.sort'), dataIndex: 'sort', width: 70 },
  { title: t('common.field.status'), key: 'status', width: 80 },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true },
  actionColumn(2)
]

async function load() {
  loading.value = true
  try {
    list.value = await listMemberLevel()
  } finally {
    loading.value = false
  }
}
onMounted(load)

const rules = {
  name: [{ required: true, message: t('pos.level.nameRequired') }],
  discountRate: [{ required: true, message: t('pos.level.discountRequired') }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, name: '', discountRate: 100, upgradeAmount: undefined, pointsRate: 1, sort: 0, status: STATUS_ENABLED, remark: '' }),
  create: addMemberLevel,
  update: updateMemberLevel,
  toPayload: (f) => ({ name: f.name, discountRate: f.discountRate, upgradeAmount: f.upgradeAmount ?? null, pointsRate: f.pointsRate ?? 1, sort: f.sort, status: f.status, remark: f.remark || null }),
  onSuccess: () => load()
})
const { formRef, form } = drawer
const remove = useAction(deleteMemberLevel, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
const rowActions = (record) => [
  { type: 'edit', perm: 'pos:memberLevel:edit', onClick: () => drawer.openEdit(record) },
  { type: 'delete', perm: 'pos:memberLevel:remove', confirm: t('pos.level.confirmDelete', { name: record.name }), onClick: () => remove(record.id) }
]
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
