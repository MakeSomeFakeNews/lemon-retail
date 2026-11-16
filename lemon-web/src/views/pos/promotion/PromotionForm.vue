<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('pos.promotion.add')" :edit-title="$t('pos.promotion.edit')">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('pos.promotion.name')" name="name">
            <a-input v-model:value="form.name" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="10">
          <a-form-item :label="$t('pos.special.period')" name="period">
            <a-range-picker v-model:value="form.period" show-time value-format="YYYY-MM-DD HH:mm:ss" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="6">
          <a-form-item :label="$t('pos.special.channel')" name="channel">
            <a-select v-model:value="form.channel" :options="toOptions(dict.pos_sale_channel)" />
          </a-form-item>
        </a-col>

        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('pos.promotion.kind')" name="kind">
            <a-radio-group v-model:value="form.kind" :options="toOptions(dict.pos_promotion_kind)" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('pos.promotion.threshold')" name="threshold">
            <a-input-group compact class="threshold">
              <a-select v-model:value="form.thresholdType" :options="toOptions(dict.pos_promotion_threshold)" class="threshold-type" />
              <a-input-number v-model:value="form.threshold" :min="0.0001" :precision="form.thresholdType === PromotionThreshold.QTY ? 4 : 2" class="threshold-value" />
            </a-input-group>
          </a-form-item>
        </a-col>
        <a-col v-if="form.kind === PromotionKind.REDUCE" :xs="24" :sm="4">
          <a-form-item :label="$t('pos.promotion.reduceAmount')" name="reduceAmount">
            <a-input-number v-model:value="form.reduceAmount" :min="0.01" :precision="2" class="full" />
          </a-form-item>
        </a-col>
        <a-col v-if="form.kind === PromotionKind.DISCOUNT" :xs="24" :sm="4">
          <a-form-item :label="$t('pos.promotion.discountRate')" name="discountRate">
            <a-input-number v-model:value="form.discountRate" :min="1" :max="99.99" :precision="2" addon-after="%" class="full" />
          </a-form-item>
        </a-col>
        <a-col v-if="form.kind !== PromotionKind.DISCOUNT" :xs="24" :sm="4">
          <a-form-item :label="$t('pos.promotion.stackable')" :extra="$t('pos.promotion.stackableTip')">
            <a-switch v-model:checked="form.stackable" />
          </a-form-item>
        </a-col>

        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('pos.special.storeScope')" name="storeScope">
            <a-radio-group v-model:value="form.storeScope" :options="toOptions(dict.pos_special_scope)" @change="form.targetIds = []" />
          </a-form-item>
        </a-col>
        <a-col v-if="form.storeScope === SpecialScope.STORES" :xs="24" :sm="16">
          <a-form-item :label="$t('pos.special.stores')" name="targetIds">
            <StoreSelect v-model:value="form.targetIds" multiple all />
          </a-form-item>
        </a-col>
        <a-col v-if="form.storeScope === SpecialScope.REGIONS" :xs="24" :sm="16">
          <a-form-item :label="$t('pos.special.regions')" name="targetIds" :extra="$t('pos.special.regionsTip')">
            <a-tree-select
              v-model:value="form.targetIds"
              :tree-data="regions"
              :field-names="{ label: 'name', value: 'id', children: 'children' }"
              multiple
              tree-default-expand-all
              show-search
              tree-node-filter-prop="name"
              allow-clear
            />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="4">
          <a-form-item :label="$t('pos.special.memberOnly')"><a-switch v-model:checked="form.memberOnly" /></a-form-item>
        </a-col>
        <a-col :xs="12" :sm="4">
          <a-form-item :label="$t('pos.promotion.excludeSpecial')" :extra="$t('pos.promotion.excludeSpecialTip')">
            <a-switch v-model:checked="form.excludeSpecial" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="4">
          <a-form-item :label="$t('pos.promotion.priority')" :extra="$t('pos.promotion.priorityTip')">
            <a-input-number v-model:value="form.priority" :precision="0" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-head">
        <a-space>
          <span class="section-title">{{ $t('pos.promotion.goodsScope') }}</span>
          <a-radio-group v-model:value="form.goodsScope" :options="toOptions(dict.pos_promotion_range)" size="small" @change="form.goods = []" />
        </a-space>
        <a-button v-if="form.goodsScope !== PromotionRange.ALL" size="small" @click="form.goods.push(emptyGoods(form.goodsScope === PromotionRange.CATEGORIES))">
          <template #icon><PlusOutlined /></template>{{ $t('common.action.addRow') }}
        </a-button>
      </div>
      <GoodsRows v-if="form.goodsScope !== PromotionRange.ALL" :rows="form.goods" :fixed="form.goodsScope === PromotionRange.CATEGORIES ? 'category' : 'material'" />

      <template v-if="form.kind === PromotionKind.GIFT">
        <div class="section-head">
          <span class="section-title">{{ $t('pos.promotion.gifts') }}</span>
          <a-space>
            <span class="sub-text">{{ $t('pos.promotion.giftsTip') }}</span>
            <a-button size="small" @click="form.gifts.push(emptyGift())"><template #icon><PlusOutlined /></template>{{ $t('common.action.addRow') }}</a-button>
          </a-space>
        </div>
        <a-table row-key="key" size="small" :columns="giftColumns" :data-source="form.gifts" :pagination="false">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'material'">
              <MaterialSelect v-model:value="record.materialId" :label="record.materialLabel" @select="(m) => Object.assign(record, { materialLabel: m.label, unit: m.unit })" />
            </template>
            <template v-else-if="column.key === 'unit'">{{ record.unit }}</template>
            <template v-else-if="column.key === 'qty'"><a-input-number v-model:value="record.qty" :min="0.0001" :precision="4" class="full" /></template>
            <template v-else-if="column.key === 'action'">
              <a-button type="text" danger size="small" @click="form.gifts.splice(index, 1)"><DeleteOutlined /></a-button>
            </template>
          </template>
        </a-table>
      </template>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import StoreSelect from '@/components/store/StoreSelect.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { addPromotion, getPromotion, updatePromotion } from '@/api/pos'
import { regionTree } from '@/api/store/region'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { toOptions } from '@/utils/dict'
import { PromotionKind, PromotionRange, PromotionThreshold, SaleChannel, SpecialScope } from '@/constants/enums'
import { emptyGoods, goodsPayload, toGoodsRow } from '../marketing'
import GoodsRows from './GoodsRows.vue'

/**
 * 满减 / 满折 / 满赠活动的新增、修改（草稿）：门槛（金额或件数）、优惠（减额 / 折扣 / 赠品，满减和满赠可每满叠加）、
 * 参与商品（全部 / 指定商品 / 指定分类含下级）、与特价互斥、优先级，适用门店、渠道、仅限会员与特价活动相同
 */
const emit = defineEmits(['saved'])
const { t } = useI18n()
const dict = useDict('pos_sale_channel', 'pos_special_scope', 'pos_promotion_kind', 'pos_promotion_threshold', 'pos_promotion_range', 'pos_goods_target_type')
const regions = ref([])

const emptyGift = () => ({ key: `${Date.now()}-${Math.random()}`, materialId: undefined, materialLabel: undefined, unit: '', qty: 1 })
const giftColumns = computed(() => [
  { title: t('pos.special.material'), key: 'material', width: 320 },
  { title: t('common.field.unit'), key: 'unit', width: 80 },
  { title: t('pos.promotion.giftQty'), key: 'qty', width: 150 },
  { title: '', key: 'action', width: 50 }
])
const rules = computed(() => ({
  name: [{ required: true, message: t('pos.promotion.nameRequired') }],
  period: [{ required: true, type: 'array', len: 2, message: t('pos.special.periodRequired') }],
  threshold: [{ required: true, message: t('pos.promotion.thresholdRequired') }],
  reduceAmount: [{ required: form.value.kind === PromotionKind.REDUCE, message: t('pos.promotion.reduceRequired') }],
  discountRate: [{ required: form.value.kind === PromotionKind.DISCOUNT, message: t('pos.promotion.rateRequired') }],
  targetIds: [{ required: form.value.storeScope !== SpecialScope.ALL, type: 'array', min: 1, message: t('pos.special.targetsRequired') }]
}))

const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined, name: '', period: [], channel: SaleChannel.POS, kind: PromotionKind.REDUCE, thresholdType: PromotionThreshold.AMOUNT,
    threshold: undefined, reduceAmount: undefined, discountRate: undefined, stackable: false, goodsScope: PromotionRange.ALL, goods: [],
    excludeSpecial: false, priority: 0, storeScope: SpecialScope.ALL, targetIds: [], memberOnly: false, remark: '', gifts: []
  }),
  beforeOpen: async () => {
    if (!regions.value.length) regions.value = await regionTree().catch(() => [])
  },
  fetch: async (record) => {
    const d = await getPromotion(record.id)
    const num = (v) => (v == null ? undefined : Number(v))
    return {
      id: d.id,
      name: d.name,
      period: [d.beginTime, d.endTime],
      channel: d.channel,
      kind: d.kind,
      thresholdType: d.thresholdType,
      threshold: num(d.threshold),
      reduceAmount: num(d.reduceAmount),
      discountRate: num(d.discountRate),
      stackable: !!d.stackable,
      goodsScope: d.goodsScope,
      goods: (d.goods || []).map((g) => toGoodsRow(g, d.goodsScope === PromotionRange.CATEGORIES)),
      excludeSpecial: !!d.excludeSpecial,
      priority: d.priority ?? 0,
      storeScope: d.storeScope,
      targetIds: d.targetIds || [],
      memberOnly: !!d.memberOnly,
      remark: d.remark || '',
      gifts: (d.gifts || []).map((g) => ({ key: g.id, materialId: g.materialId, materialLabel: `${g.materialCode} ${g.materialName}`, unit: g.unit, qty: Number(g.qty) }))
    }
  },
  create: async (data) => emit('saved', await addPromotion(data)),
  update: async (id, data) => {
    await updatePromotion(id, data)
    emit('saved', id)
  },
  toPayload: (f) => ({
    name: f.name,
    kind: f.kind,
    thresholdType: f.thresholdType,
    threshold: f.threshold,
    reduceAmount: f.kind === PromotionKind.REDUCE ? f.reduceAmount : null,
    discountRate: f.kind === PromotionKind.DISCOUNT ? f.discountRate : null,
    stackable: f.kind !== PromotionKind.DISCOUNT && f.stackable,
    goodsScope: f.goodsScope,
    goods: f.goodsScope === PromotionRange.ALL ? [] : goodsPayload(f.goods),
    excludeSpecial: f.excludeSpecial,
    priority: f.priority ?? 0,
    beginTime: f.period[0],
    endTime: f.period[1],
    channel: f.channel,
    storeScope: f.storeScope,
    targetIds: f.storeScope === SpecialScope.ALL ? [] : f.targetIds,
    memberOnly: f.memberOnly,
    remark: f.remark || null,
    gifts: f.kind === PromotionKind.GIFT ? f.gifts.filter((g) => g.materialId).map((g) => ({ materialId: g.materialId, qty: g.qty })) : []
  })
})
const { formRef, form } = drawer

defineExpose({ openCreate: () => drawer.openCreate(), openEdit: (r) => drawer.openEdit(r) })
</script>

<style scoped>
.full {
  width: 100%;
}
.threshold {
  display: flex;
}
.threshold-type {
  width: 100px;
}
.threshold-value {
  flex: 1;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 12px 0;
}
.section-title {
  font-weight: 500;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
