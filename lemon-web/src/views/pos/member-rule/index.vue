<template>
  <div class="rule-page">
    <a-spin :spinning="loading">
      <a-form ref="formRef" :model="rule" layout="vertical">
        <a-card :bordered="false" class="page-card" :title="$t('pos.rule.points')">
          <a-row :gutter="16">
            <a-col :xs="24" :sm="6">
              <a-form-item :label="$t('pos.rule.pointsEnabled')"><a-switch v-model:checked="rule.pointsEnabled" /></a-form-item>
            </a-col>
            <a-col :xs="24" :sm="9">
              <a-form-item :label="$t('pos.rule.earn')" :extra="$t('pos.rule.earnTip')">
                <a-space>
                  <span>{{ $t('pos.rule.earnPer') }}</span>
                  <a-input-number v-model:value="rule.earnAmount" :min="0.01" :precision="2" />
                  <span>{{ $t('pos.rule.earnGet') }}</span>
                  <a-input-number v-model:value="rule.earnPoints" :min="0" :precision="2" />
                </a-space>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :xs="24" :sm="6">
              <a-form-item :label="$t('pos.rule.redeemEnabled')"><a-switch v-model:checked="rule.redeemEnabled" /></a-form-item>
            </a-col>
            <a-col :xs="24" :sm="9">
              <a-form-item :label="$t('pos.rule.redeemPoints')" :extra="$t('pos.rule.redeemPointsTip')">
                <a-input-number v-model:value="rule.redeemPoints" :min="1" :max="1000000" :precision="0" class="full" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="9">
              <a-form-item :label="$t('pos.rule.redeemMaxRate')">
                <a-input-number v-model:value="rule.redeemMaxRate" :min="0" :max="100" :precision="2" addon-after="%" class="full" />
              </a-form-item>
            </a-col>
          </a-row>
          <div class="section-head">
            <span class="section-title">{{ $t('pos.rule.excludes') }}</span>
            <a-space>
              <span class="sub-text">{{ $t('pos.rule.excludesTip') }}</span>
              <a-button size="small" @click="excludes.push(emptyGoods())"><template #icon><PlusOutlined /></template>{{ $t('common.action.addRow') }}</a-button>
            </a-space>
          </div>
          <GoodsRows :rows="excludes" />
        </a-card>

        <a-card :bordered="false" class="page-card" :title="$t('pos.rule.storedValue')">
          <a-form-item :label="$t('pos.rule.storedValueEnabled')"><a-switch v-model:checked="rule.storedValueEnabled" /></a-form-item>
          <div class="section-head">
            <span class="section-title">{{ $t('pos.rule.tiers') }}</span>
            <a-space>
              <span class="sub-text">{{ $t('pos.rule.tiersTip') }}</span>
              <a-button size="small" @click="tiers.push({ key: Date.now() + Math.random(), minAmount: undefined, bonusAmount: undefined })">
                <template #icon><PlusOutlined /></template>{{ $t('common.action.addRow') }}
              </a-button>
            </a-space>
          </div>
          <a-table row-key="key" size="small" :columns="tierColumns" :data-source="tiers" :pagination="false">
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.key === 'minAmount'"><a-input-number v-model:value="record.minAmount" :min="0.01" :precision="2" class="full" /></template>
              <template v-else-if="column.key === 'bonusAmount'"><a-input-number v-model:value="record.bonusAmount" :min="0" :precision="2" class="full" /></template>
              <template v-else-if="column.key === 'action'">
                <a-button type="text" danger size="small" @click="tiers.splice(index, 1)"><DeleteOutlined /></a-button>
              </template>
            </template>
          </a-table>
          <a-form-item :label="$t('common.field.remark')" class="gap">
            <a-input v-model:value="rule.remark" :maxlength="255" />
          </a-form-item>
        </a-card>

        <div class="footer">
          <a-button v-perm="'pos:memberRule:edit'" type="primary" :loading="saving" @click="save">{{ $t('common.action.save') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { getMemberRule, saveMemberRule } from '@/api/pos'
import { GoodsTargetType } from '@/constants/enums'
import { useDict } from '@/composables/useDict'
import { emptyGoods, goodsPayload, toGoodsRow } from '../marketing'
import GoodsRows from '../promotion/GoodsRows.vue'

/**
 * 积分与储值规则（每个租户一条）：消费积分（每消费 X 元得 N 分，× 会员等级的积分倍率，不积分的商品 / 分类）、积分抵现（X 分抵 1 元，最多占应收的百分比）、
 * 储值（启用、充值赠送档位：充 X 送 Y，按达到的最高档位）
 */
const { t } = useI18n()
useDict('pos_goods_target_type')
const loading = ref(false)
const saving = ref(false)
const formRef = ref()
const rule = reactive({ pointsEnabled: true, earnAmount: 1, earnPoints: 1, redeemEnabled: true, redeemPoints: 100, redeemMaxRate: 50, storedValueEnabled: true, remark: '' })
const excludes = ref([])
const tiers = ref([])
const tierColumns = computed(() => [
  { title: t('pos.rule.minAmount'), key: 'minAmount', width: 200 },
  { title: t('pos.rule.bonusAmount'), key: 'bonusAmount', width: 200 },
  { title: '', key: 'action', width: 50 }
])

async function load() {
  loading.value = true
  try {
    const r = await getMemberRule()
    const x = r.rule || {}
    Object.assign(rule, {
      pointsEnabled: !!x.pointsEnabled,
      earnAmount: Number(x.earnAmount),
      earnPoints: Number(x.earnPoints),
      redeemEnabled: !!x.redeemEnabled,
      redeemPoints: Number(x.redeemPoints),
      redeemMaxRate: Number(x.redeemMaxRate),
      storedValueEnabled: !!x.storedValueEnabled,
      remark: x.remark || ''
    })
    excludes.value = (r.excludes || []).map((e) => toGoodsRow(e, e.targetType === GoodsTargetType.CATEGORY))
    tiers.value = (r.tiers || []).map((x2) => ({ key: x2.id, minAmount: Number(x2.minAmount), bonusAmount: Number(x2.bonusAmount) }))
  } finally {
    loading.value = false
  }
}
onMounted(load)

async function save() {
  saving.value = true
  try {
    await saveMemberRule({
      rule: { ...rule, remark: rule.remark || null },
      excludes: goodsPayload(excludes.value),
      tiers: tiers.value.filter((x) => x.minAmount).map((x) => ({ minAmount: x.minAmount, bonusAmount: x.bonusAmount ?? 0 }))
    })
    message.success(t('common.msg.saveSuccess'))
    load()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.rule-page {
  height: 100%;
  overflow: auto;
}
.page-card {
  margin-bottom: 12px;
}
.full {
  width: 100%;
}
.gap {
  margin-top: 12px;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0;
}
.section-title {
  font-weight: 500;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.footer {
  padding: 4px 0 16px;
}
</style>
