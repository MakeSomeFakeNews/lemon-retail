<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card" :title="$t('chain.alloc.ruleTitle')">
      <a-form :model="rule" layout="inline" class="rule-form">
        <a-form-item :label="$t('chain.alloc.basis')">
          <a-radio-group v-model:value="rule.basis" :disabled="!canEdit">
            <a-radio v-for="o in basisOptions" :key="o.value" :value="o.value">{{ o.label }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item>
          <a-checkbox v-model:checked="rule.includeDc" :disabled="!canEdit">{{ $t('chain.alloc.includeDc') }}</a-checkbox>
          <a-checkbox v-model:checked="rule.includeFranchise" :disabled="!canEdit">{{ $t('chain.alloc.includeFranchise') }}</a-checkbox>
        </a-form-item>
        <a-form-item :label="$t('chain.alloc.region')">
          <a-tree-select
            v-model:value="rule.regionId"
            :tree-data="regions"
            :field-names="{ label: 'name', value: 'id', children: 'children' }"
            allow-clear
            tree-default-expand-all
            :placeholder="$t('chain.alloc.allStores')"
            :disabled="!canEdit"
            class="w200"
          />
        </a-form-item>
        <a-form-item v-if="canEdit">
          <a-button type="primary" :loading="savingRule" @click="saveRule">{{ $t('common.action.save') }}</a-button>
        </a-form-item>
      </a-form>
      <div class="sub-text tip">{{ $t('chain.alloc.ruleTip') }}</div>
    </a-card>
    <a-card :bordered="false" class="page-card table-card">
      <a-space wrap class="mb12">
        <a-date-picker v-model:value="period" picker="month" value-format="YYYY-MM" :allow-clear="false" />
        <a-button v-perm="'chain:alloc:run'" type="primary" :loading="running" @click="run">
          <template #icon><PlayCircleOutlined /></template>{{ $t('chain.alloc.run') }}
        </a-button>
      </a-space>
      <a-table row-key="id" size="middle" :columns="columns" :data-source="list" :loading="loading" :pagination="pagination" :scroll="{ x: 900, y: '100%' }" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'period'"><a @click="openDetail(record.id)">{{ record.period }}</a></template>
          <template v-else-if="column.key === 'basis'"><DictTag type="chain_alloc_basis" :value="record.basis" /></template>
          <template v-else-if="column.key === 'poolAmount'">{{ money(record.poolAmount) }}</template>
          <template v-else-if="column.key === 'action'">
            <TableActions
              :actions="[
                { type: 'detail', onClick: () => openDetail(record.id) },
                { type: 'delete', perm: 'chain:alloc:run', confirm: $t('chain.alloc.confirmDelete', { period: record.period }), onClick: () => remove(record.id) }
              ]"
            />
          </template>
        </template>
      </a-table>
    </a-card>
    <a-drawer v-model:open="detailOpen" :title="detail ? $t('chain.alloc.detailTitle', { period: detail.period }) : ''" width="min(760px, 100vw)">
      <template v-if="detail">
        <a-descriptions size="small" :column="3" class="mb12">
          <a-descriptions-item :label="$t('chain.alloc.basis')"><DictTag type="chain_alloc_basis" :value="detail.basis" /></a-descriptions-item>
          <a-descriptions-item :label="$t('chain.alloc.poolAmount')">{{ money(detail.poolAmount) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('chain.alloc.storeCount')">{{ detail.storeCount }}</a-descriptions-item>
        </a-descriptions>
        <a-table row-key="id" size="small" :columns="recordColumns" :data-source="detail.records" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'ratio'">{{ (Number(record.ratio) * 100).toFixed(2) }}%</template>
            <template v-else-if="column.money">{{ money(record[column.dataIndex]) }}</template>
          </template>
        </a-table>
      </template>
    </a-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { PlayCircleOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { deleteAllocRun, getAllocRule, getAllocRun, pageAllocRun, runAlloc, saveAllocRule } from '@/api/chain'
import { regionTree } from '@/api/store/region'
import { AllocBasis } from '@/constants/enums'
import { useAction } from '@/composables/useAction'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { money } from '@/utils/number'

/** 总部费用分摊：设置分摊依据（销售额 / 面积 / 平均）与参与门店，按月运行，结果计入门店损益 */
const { t } = useI18n()
const userStore = useUserStore()
const canEdit = computed(() => userStore.hasPerm('chain:alloc:edit'))
const dict = useDict('chain_alloc_basis')
const basisOptions = computed(() => (dict.chain_alloc_basis || []).map((d) => ({ value: d.value, label: d.label })))
const rule = reactive({ basis: AllocBasis.SALES, includeDc: false, includeFranchise: false, regionId: null, remark: '' })
const regions = ref([])
const savingRule = ref(false)
const period = ref(dayjs().subtract(1, 'month').format('YYYY-MM'))
const running = ref(false)

const columns = [
  { title: t('chain.alloc.period'), key: 'period', dataIndex: 'period', width: 100 },
  { title: t('chain.alloc.basis'), key: 'basis', width: 110 },
  { title: t('chain.alloc.poolAmount'), key: 'poolAmount', width: 140, align: 'right' },
  { title: t('chain.alloc.storeCount'), dataIndex: 'storeCount', width: 100, align: 'right' },
  { title: t('chain.alloc.runTime'), dataIndex: 'createTime', width: 170 },
  actionColumn(2)
]
const recordColumns = [
  { title: t('chain.common.store'), dataIndex: 'storeName' },
  { title: t('chain.alloc.basisValue'), dataIndex: 'basisValue', width: 130, align: 'right', money: true },
  { title: t('chain.alloc.ratio'), key: 'ratio', width: 100, align: 'right' },
  { title: t('chain.common.amount'), dataIndex: 'amount', width: 130, align: 'right', money: true }
]
const { list, loading, pagination, load, onTableChange } = useTable(pageAllocRun, {})

async function saveRule() {
  savingRule.value = true
  try {
    await saveAllocRule({ ...rule, regionId: rule.regionId ?? null })
    message.success(t('common.msg.saveSuccess'))
  } finally {
    savingRule.value = false
  }
}

async function run() {
  running.value = true
  try {
    const id = await runAlloc(period.value)
    message.success(t('chain.alloc.runDone'))
    load()
    openDetail(id)
  } finally {
    running.value = false
  }
}

const detailOpen = ref(false)
const detail = ref()
async function openDetail(id) {
  detail.value = await getAllocRun(id)
  detailOpen.value = true
}
const remove = useAction(deleteAllocRun, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

onMounted(async () => {
  const [r, tree] = await Promise.all([getAllocRule(), regionTree().catch(() => [])])
  Object.assign(rule, { basis: r.basis, includeDc: !!r.includeDc, includeFranchise: !!r.includeFranchise, regionId: r.regionId ?? null, remark: r.remark || '' })
  regions.value = tree
  load()
})
</script>

<style scoped>
.w200 {
  width: 200px;
}
.mb12 {
  margin-bottom: 12px;
}
.rule-form {
  row-gap: 8px;
}
.tip {
  margin-top: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
