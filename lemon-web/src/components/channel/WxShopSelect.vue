<template>
  <a-select
    :value="value ?? undefined"
    :options="options"
    :placeholder="placeholder || $t('channel.common.selectShop')"
    :allow-clear="allowClear"
    show-search
    option-filter-prop="label"
    @change="onChange"
  />
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { wxShopOptions } from '@/api/channel/wxShop'

/**
 * 微信小店店铺下拉（可管辖门店的店铺）。v-model:value 为店铺 ID；@shop(店铺) 返回选中的店铺（含 mode，判断是否模拟店铺）
 */
const props = defineProps({
  value: Number,
  placeholder: String,
  allowClear: { type: Boolean, default: true }
})
const emit = defineEmits(['update:value', 'shop', 'loaded'])

const list = ref([])
const options = computed(() => list.value.map((s) => ({ value: s.id, label: s.name })))

function onChange(v) {
  emit('update:value', v)
  emit('shop', list.value.find((s) => s.id === v))
}

onMounted(async () => {
  list.value = await wxShopOptions()
  emit('loaded', list.value)
  if (props.value != null) emit('shop', list.value.find((s) => s.id === props.value))
})

defineExpose({ find: (id) => list.value.find((s) => s.id === id) })
</script>
