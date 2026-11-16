<template>
  <a-select
    :value="value ?? undefined"
    :mode="multiple ? 'multiple' : undefined"
    show-search
    :allow-clear="allowClear"
    :placeholder="placeholder"
    :options="options"
    :loading="loading"
    :disabled="disabled"
    option-filter-prop="label"
    :dropdown-match-select-width="280"
    @change="(v) => emit('update:value', v ?? (multiple ? [] : null))"
  />
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { storeOptions } from '@/api/store/store'
import { t } from '@/locales'
import { cached } from '@/utils/cache'

/**
 * 门店下拉：默认只列出当前用户可管辖的门店；all 为 true 时列出全部门店，trading 为 true 时只列出可以开单的门店。
 * types 限定门店类型（如 [StoreType.HQ, StoreType.DC]），exclude 排除某个门店（如编辑中的门店本身），multiple 多选
 */
const props = defineProps({
  value: [Number, Array],
  placeholder: { type: String, default: () => t('store.common.selectStore') },
  allowClear: { type: Boolean, default: true },
  disabled: Boolean,
  multiple: Boolean,
  all: Boolean,
  trading: Boolean,
  types: Array,
  exclude: Number
})
const emit = defineEmits(['update:value'])

const list = ref([])
const loading = ref(false)
const options = computed(() =>
  list.value
    .filter((s) => !props.types?.length || props.types.includes(s.type))
    .filter((s) => s.id !== props.exclude)
    .map((s) => ({ value: s.id, label: `${s.name}（${s.code}）` }))
)

onMounted(async () => {
  loading.value = true
  try {
    const params = { all: props.all || undefined, trading: props.trading || undefined }
    list.value = await cached(`store-options:${!!props.all}:${!!props.trading}`, () => storeOptions(params))
  } finally {
    loading.value = false
  }
})
</script>
