<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-alert type="info" show-icon :message="$t('dist.dateLock.tip')" class="block" />
      <div class="toolbar">
        <a-button v-perm="'dist:dateLock:edit'" type="primary" @click="openForm()">
          <template #icon><LockOutlined /></template>{{ $t('dist.dateLock.set') }}
        </a-button>
        <a-button @click="load"><template #icon><ReloadOutlined /></template></a-button>
      </div>
      <a-table row-key="id" size="middle" :columns="columns" :data-source="list" :loading="loading" :pagination="false" :locale="{ emptyText: $t('dist.dateLock.none') }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'scope'">{{ record.storeId ? record.storeName : $t('dist.dateLock.company') }}</template>
          <template v-else-if="column.key === 'action'">
            <a-space v-perm="'dist:dateLock:edit'">
              <a @click="openForm(record)">{{ $t('dist.dateLock.edit') }}</a>
              <a-popconfirm :title="$t('dist.dateLock.unlockTitle', { name: record.storeId ? record.storeName : $t('dist.dateLock.company') })" @confirm="unlock(record)">
                <a class="danger">{{ $t('dist.dateLock.unlock') }}</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="formOpen" :title="$t('dist.dateLock.set')" :confirm-loading="saving" @ok="save">
      <a-form layout="vertical">
        <a-form-item :label="$t('dist.dateLock.scope')">
          <StoreSelect v-model:value="form.storeId" :placeholder="$t('dist.dateLock.storePh')" :disabled="form.editing" />
        </a-form-item>
        <a-form-item :label="$t('dist.dateLock.lockDate')" required>
          <a-date-picker v-model:value="form.lockDate" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <a-form-item :label="$t('common.field.remark')">
          <a-input v-model:value="form.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { LockOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import StoreSelect from '@/components/store/StoreSelect.vue'
import { listDateLocks, setDateLock } from '@/api/dist'

/** 日期锁定（结账日）：全公司一条，门店可另设；锁定日期及以前的单据不能新增、修改、确认、作废 */
const { t } = useI18n()
const list = ref([])
const loading = ref(false)
const columns = [
  { title: t('dist.dateLock.scope'), key: 'scope', width: 200 },
  { title: t('dist.dateLock.lockDate'), dataIndex: 'lockDate', width: 130 },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true },
  { title: t('dist.dateLock.updateBy'), dataIndex: 'updateByName', width: 110 },
  { title: t('dist.dateLock.updateTime'), dataIndex: 'updateTime', width: 170 },
  { title: t('common.action.operation'), key: 'action', width: 140 }
]

async function load() {
  loading.value = true
  try {
    list.value = await listDateLocks()
  } finally {
    loading.value = false
  }
}
onMounted(load)

const formOpen = ref(false)
const saving = ref(false)
const form = reactive({ storeId: undefined, lockDate: undefined, remark: '', editing: false })
function openForm(record) {
  Object.assign(form, {
    storeId: record?.storeId || undefined,
    lockDate: record?.lockDate,
    remark: record?.remark || '',
    editing: !!record
  })
  formOpen.value = true
}

async function save() {
  if (!form.lockDate) {
    message.warning(t('dist.common.selectDate'))
    return
  }
  saving.value = true
  try {
    await setDateLock({ storeId: form.storeId || null, lockDate: form.lockDate, remark: form.remark || null })
    message.success(t('common.msg.saveSuccess'))
    formOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function unlock(record) {
  await setDateLock({ storeId: record.storeId || null, lockDate: null })
  message.success(t('common.msg.operationSuccess'))
  load()
}
</script>

<style scoped>
.block {
  margin-bottom: 12px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}
.full {
  width: 100%;
}
.danger {
  color: var(--ant-color-error, #ff4d4f);
}
</style>
