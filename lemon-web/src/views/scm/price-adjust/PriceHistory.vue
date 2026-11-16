<template>
  <SubPage :open="open" :title="$t('scm.priceAdjust.history')" width="min(1000px, 100vw)" @close="open = false">
    <a-alert type="info" show-icon :message="$t('scm.priceAdjust.historyTip')" class="tip" />
    <a-space wrap class="filters">
      <MaterialSelect v-model:value="materialId" :label="materialLabel" class="material" @select="(m) => (materialLabel = m.label)" />
      <a-select v-model:value="scope" :options="scopeOptions" show-search option-filter-prop="label" class="scope" />
    </a-space>
    <HistoryTable v-if="materialId" :material-id="materialId" :scheme-id="scopeSchemeId" :target="scopeTarget" />
    <a-empty v-else :description="$t('scm.priceAdjust.historySelectMaterial')" />
  </SubPage>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import SubPage from '@/components/SubPage.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { priceSchemeOptions } from '@/api/scm/priceScheme'
import { PriceAdjustTarget } from '@/constants/enums'
import HistoryTable from './HistoryTable.vue'

/** 按商品查看调价历史：全部、只看总部价、只看某个价格方案 */
const { t } = useI18n()
const open = ref(false)
const materialId = ref()
const materialLabel = ref()
const schemes = ref([])
// 'ALL' / 'HQ' / 方案 ID
const scope = ref('ALL')
const scopeOptions = computed(() => [
  { value: 'ALL', label: t('scm.priceAdjust.allTargets') },
  { value: PriceAdjustTarget.HQ, label: t('scm.priceAdjust.hqPrice') },
  ...schemes.value.map((s) => ({ value: s.id, label: `${s.code} ${s.name}` }))
])
const scopeSchemeId = computed(() => (typeof scope.value === 'number' ? scope.value : undefined))
const scopeTarget = computed(() => (scope.value === PriceAdjustTarget.HQ ? PriceAdjustTarget.HQ : undefined))

async function show(material) {
  materialId.value = material?.id
  materialLabel.value = material?.label
  open.value = true
  schemes.value = await priceSchemeOptions().catch(() => [])
}

defineExpose({ open: show })
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.filters {
  margin-bottom: 12px;
}
.material {
  width: 360px;
  max-width: 100%;
}
.scope {
  width: 220px;
}
</style>
