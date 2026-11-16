<template>
  <FormDrawer :drawer="drawer" :create-title="$t('store.store.createTitle')" :edit-title="$t('store.store.editTitle')" width="min(760px, 100vw)">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <!-- 基本信息 -->
      <div class="section-title">{{ $t('store.store.sectionBasic') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('store.store.code')" name="code">
            <a-input v-model:value="form.code" :maxlength="32" :placeholder="$t('store.store.codePh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('store.store.name')" name="name">
            <a-input v-model:value="form.name" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('store.store.shortName')" name="shortName">
            <a-input v-model:value="form.shortName" :maxlength="32" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('store.store.type')" name="type">
            <a-select
              v-model:value="form.type"
              :options="toOptions(dict.store_type)"
              :placeholder="$t('store.store.selectType')"
              :disabled="isEdit && originalType === StoreType.HQ"
            />
          </a-form-item>
        </a-col>
        <a-col v-if="!isEdit" :xs="24" :sm="12">
          <a-form-item :label="$t('store.store.status')" name="status">
            <a-select v-model:value="form.status" :options="statusOptions" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('store.store.contact')" name="contact">
            <a-input v-model:value="form.contact" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('store.store.phone')" name="phone">
            <a-input v-model:value="form.phone" :maxlength="32" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('store.store.address')" name="address">
            <a-input v-model:value="form.address" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>

      <!-- 经营设置 -->
      <div class="section-title">{{ $t('store.store.sectionOperation') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('store.common.region')" name="regionId">
            <a-tree-select
              v-model:value="form.regionId"
              :tree-data="regionOptions"
              :field-names="{ label: 'name', value: 'id', children: 'children' }"
              :placeholder="$t('store.common.selectRegion')"
              tree-default-expand-all
              show-search
              tree-node-filter-prop="name"
              allow-clear
            />
          </a-form-item>
        </a-col>
        <a-col v-if="form.type !== StoreType.HQ" :xs="24" :sm="12">
          <a-form-item :label="$t('store.store.supplyStore')" name="supplyStoreId" :extra="$t('store.store.supplyStoreExtra')">
            <StoreSelect v-model:value="form.supplyStoreId" all :types="SUPPLY_TYPES" :exclude="form.id" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('store.store.priceScheme')" name="priceSchemeId" :extra="$t('store.store.priceSchemeExtra')">
            <a-select v-model:value="form.priceSchemeId" :options="schemeOptions" show-search option-filter-prop="label" allow-clear />
          </a-form-item>
        </a-col>
        <template v-if="isEdit">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('store.store.defaultWarehouse')" name="defaultWarehouseId" :extra="$t('store.store.defaultWarehouseExtra')">
              <a-select v-model:value="form.defaultWarehouseId" :options="warehouseList" show-search option-filter-prop="label" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('store.store.defaultAccount')" name="defaultAccountId" :extra="$t('store.store.defaultAccountExtra')">
              <a-select v-model:value="form.defaultAccountId" :options="accountList" show-search option-filter-prop="label" allow-clear />
            </a-form-item>
          </a-col>
        </template>
        <a-col v-if="canLoadDept" :xs="24" :sm="12">
          <a-form-item :label="$t('store.store.dept')" name="deptId" :extra="$t('store.store.deptExtra')">
            <a-tree-select
              v-model:value="form.deptId"
              :tree-data="deptOptions"
              :field-names="{ label: 'name', value: 'id', children: 'children' }"
              :placeholder="$t('common.placeholder.selectName', { name: $t('common.field.dept') })"
              tree-default-expand-all
              show-search
              tree-node-filter-prop="name"
              allow-clear
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('store.store.businessHours')" name="businessHours">
            <a-input v-model:value="form.businessHours" :maxlength="64" :placeholder="$t('store.store.businessHoursPh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('store.store.area')" name="area">
            <a-input-number v-model:value="form.area" :min="0" :precision="2" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('store.store.longitude')" name="longitude">
            <a-input-number v-model:value="form.longitude" :min="-180" :max="180" :precision="6" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('store.store.latitude')" name="latitude">
            <a-input-number v-model:value="form.latitude" :min="-90" :max="90" :precision="6" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('common.field.sort')" name="sort">
            <a-input-number v-model:value="form.sort" :min="0" class="full" />
          </a-form-item>
        </a-col>
      </a-row>

      <!-- 加盟信息：只有加盟店 -->
      <template v-if="form.type === StoreType.FRANCHISE">
        <div class="section-title">{{ $t('store.store.sectionFranchise') }}</div>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('store.store.franchisee')" name="franchisee">
              <a-input v-model:value="form.franchisee" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('store.store.settleMode')" name="settleMode">
              <a-select v-model:value="form.settleMode" :options="toOptions(dict.store_settle_mode)" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('store.store.contractStart')" name="contractStart">
              <a-date-picker v-model:value="form.contractStart" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('store.store.contractEnd')" name="contractEnd">
              <a-date-picker v-model:value="form.contractEnd" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('store.store.deposit')" name="deposit">
              <a-input-number v-model:value="form.deposit" :min="0" :precision="2" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('store.store.deliveryScheme')" name="deliverySchemeId" :extra="$t('store.store.deliverySchemeExtra')">
              <a-select v-model:value="form.deliverySchemeId" :options="schemeOptions" show-search option-filter-prop="label" allow-clear />
            </a-form-item>
          </a-col>
        </a-row>
      </template>

      <a-form-item :label="$t('common.field.remark')" name="remark">
        <a-textarea v-model:value="form.remark" :rows="3" :maxlength="255" show-count />
      </a-form-item>

      <!-- 新增时同时创建仓库、现金账户 -->
      <template v-if="!isEdit">
        <a-form-item name="createWarehouse" class="check-item">
          <a-checkbox v-model:checked="form.createWarehouse">{{ $t('store.store.createWarehouse') }}</a-checkbox>
        </a-form-item>
        <a-form-item name="createAccount" class="check-item" :extra="$t('store.store.createAccountTip')">
          <a-checkbox v-model:checked="form.createAccount">{{ $t('store.store.createAccount') }}</a-checkbox>
        </a-form-item>
      </template>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import StoreSelect from '@/components/store/StoreSelect.vue'
import { cashAccountOptions } from '@/api/fin'
import { regionTree } from '@/api/store/region'
import { addStore, getStore, priceSchemeOptions, updateStore } from '@/api/store/store'
import { deptTree } from '@/api/system/dept'
import { warehouseOptions } from '@/api/wms/warehouse'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { StoreStatus, StoreType } from '@/constants/enums'
import { useUserStore } from '@/store/user'
import { STATUS_DISABLED } from '@/utils/constants'
import { toOptions } from '@/utils/dict'
import { mapTree } from '@/utils/tree'

/**
 * 新增 / 编辑门店。通过 ref 调用 openCreate(defaults) / openEdit(record)，保存成功后触发 success
 */
const emit = defineEmits(['success'])
const { t } = useI18n()
const userStore = useUserStore()
const dict = useDict('store_type', 'store_status', 'store_settle_mode')

/** 上级配送门店只能是总部或配送中心 */
const SUPPLY_TYPES = [StoreType.HQ, StoreType.DC]

// 新增时可选的初始状态（停业、闭店没有意义）
const statusOptions = computed(() =>
  toOptions(dict.store_status).filter((o) => [StoreStatus.PREPARING, StoreStatus.OPEN].includes(o.value))
)

// ---------- 下拉选项 ----------
const regions = ref([])
const depts = ref([])
const schemes = ref([])
const warehouseList = ref([])
const accountList = ref([])
/** 部门树接口需要部门、用户等权限，没有时不显示「对应部门」 */
const canLoadDept = computed(() => ['sys:dept:list', 'sys:user:list', 'sys:role:list'].some((p) => userStore.hasPerm(p)))

const disabledTree = (tree) => mapTree(tree, (n) => ({ id: n.id, name: n.name, disabled: n.status === STATUS_DISABLED }))
const regionOptions = computed(() => disabledTree(regions.value))
const deptOptions = computed(() => disabledTree(depts.value))
const schemeOptions = computed(() => schemes.value.map((s) => ({ value: s.id, label: s.code ? `${s.name}（${s.code}）` : s.name })))

async function loadOptions(record) {
  const [r, d, s, w, a] = await Promise.all([
    regionTree(),
    canLoadDept.value ? deptTree() : [],
    // 价格方案下拉失败（如没有该接口的权限）时不影响门店资料的维护
    priceSchemeOptions().catch(() => []),
    record ? warehouseOptions({ storeId: record.id }) : [],
    record ? cashAccountOptions(record.id) : []
  ])
  regions.value = r
  depts.value = d
  schemes.value = s
  warehouseList.value = w.map((x) => ({ value: x.id, label: `${x.name}（${x.code}）` }))
  accountList.value = a.map((x) => ({ value: x.id, label: `${x.code} ${x.name}` }))
}

// ---------- 表单 ----------
const originalType = ref()

const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined,
    code: '',
    name: '',
    shortName: '',
    type: StoreType.DIRECT,
    status: StoreStatus.OPEN,
    regionId: undefined,
    supplyStoreId: undefined,
    priceSchemeId: undefined,
    contact: '',
    phone: '',
    address: '',
    longitude: undefined,
    latitude: undefined,
    businessHours: '',
    area: undefined,
    deptId: undefined,
    defaultWarehouseId: undefined,
    defaultAccountId: undefined,
    franchisee: '',
    contractStart: undefined,
    contractEnd: undefined,
    deposit: undefined,
    settleMode: undefined,
    deliverySchemeId: undefined,
    sort: 0,
    remark: '',
    createWarehouse: true,
    createAccount: false
  }),
  beforeOpen: async (record) => {
    originalType.value = record?.type
    await loadOptions(record)
  },
  fetch: (record) => getStore(record.id),
  create: addStore,
  update: updateStore,
  toPayload: (f) => {
    const franchise = f.type === StoreType.FRANCHISE
    const data = {
      code: f.code?.trim() || null,
      name: f.name,
      shortName: f.shortName,
      type: f.type,
      regionId: f.regionId ?? null,
      supplyStoreId: f.type === StoreType.HQ ? null : f.supplyStoreId ?? null,
      priceSchemeId: f.priceSchemeId ?? null,
      contact: f.contact,
      phone: f.phone,
      address: f.address,
      longitude: f.longitude ?? null,
      latitude: f.latitude ?? null,
      businessHours: f.businessHours,
      area: f.area ?? null,
      deptId: f.deptId ?? null,
      franchisee: franchise ? f.franchisee : null,
      contractStart: franchise ? f.contractStart ?? null : null,
      contractEnd: franchise ? f.contractEnd ?? null : null,
      deposit: franchise ? f.deposit ?? null : null,
      settleMode: franchise ? f.settleMode ?? null : null,
      deliverySchemeId: franchise ? f.deliverySchemeId ?? null : null,
      sort: f.sort,
      remark: f.remark
    }
    if (f.id != null) {
      return { ...data, defaultWarehouseId: f.defaultWarehouseId ?? null, defaultAccountId: f.defaultAccountId ?? null }
    }
    return { ...data, status: f.status, createWarehouse: f.createWarehouse, createAccount: f.createAccount }
  },
  onSuccess: () => emit('success')
})
const { form, formRef, isEdit } = drawer

const rules = {
  code: [{ pattern: /^[A-Za-z0-9_-]{0,32}$/, message: t('store.region.codeRule') }],
  name: [{ required: true, whitespace: true, message: t('store.store.inputName') }],
  type: [{ required: true, message: t('store.store.selectType') }],
  contractEnd: [
    {
      validator: (_rule, value) =>
        value && form.value.contractStart && value < form.value.contractStart
          ? Promise.reject(t('store.store.contractEndInvalid'))
          : Promise.resolve()
    }
  ]
}

defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.section-title {
  margin: 4px 0 12px;
  padding-left: 8px;
  border-left: 3px solid var(--app-primary);
  font-weight: 600;
  line-height: 1.2;
}
.full {
  width: 100%;
}
.check-item {
  margin-bottom: 8px;
}
</style>
