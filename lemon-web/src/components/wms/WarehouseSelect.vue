<template>
  <a-select
    :value="value ?? undefined"
    show-search
    :allow-clear="allowClear"
    :placeholder="placeholder"
    :options="options"
    :loading="loading"
    :disabled="disabled"
    option-filter-prop="label"
    :dropdown-match-select-width="280"
    @change="(v) => emit('update:value', v ?? null)"
  />
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { warehouseOptions } from '@/api/wms/warehouse'
import { t } from '@/locales'

/**
 * 仓库下拉：启用的仓库，同一页面内只请求一次；types 限定仓库类型（如 ['DEFECT'] 只显示不良品仓），excludeTypes 排除仓库类型。
 * 默认只列出可管辖门店的仓库；all 为 true 时列出全部门店的仓库（如调拨单的调入仓库）；storeId 只列出该门店的仓库（在已加载的列表中筛选）
 */
const props = defineProps({
  value: Number,
  placeholder: { type: String, default: () => t('wms.common.selectWarehouse') },
  allowClear: { type: Boolean, default: true },
  disabled: Boolean,
  types: Array,
  excludeTypes: Array,
  all: Boolean,
  storeId: Number
})
const emit = defineEmits(['update:value'])

const list = ref([])
const loading = ref(false)
const options = computed(() =>
  list.value
    .filter((w) => !props.types?.length || props.types.includes(w.type || 'NORMAL'))
    .filter((w) => !props.excludeTypes?.length || !props.excludeTypes.includes(w.type || 'NORMAL'))
    .filter((w) => !props.storeId || w.storeId === props.storeId)
    .map((w) => ({ value: w.id, label: `${w.name}（${w.code}）` }))
)

onMounted(async () => {
  loading.value = true
  try {
    list.value = await warehouseOptions(props.all ? { all: true } : undefined)
  } finally {
    loading.value = false
  }
})
</script>
