<template>
  <span v-if="old == null && newValue == null">-</span>
  <span v-else-if="same">{{ price(newValue) }}</span>
  <span v-else>
    <span class="old">{{ old == null ? '-' : price(old) }}</span>
    <ArrowRightOutlined class="arrow" />
    <b :class="up ? 'up' : 'down'">{{ newValue == null ? '-' : price(newValue) }}</b>
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { ArrowRightOutlined } from '@ant-design/icons-vue'
import { price } from '@/utils/number'

/** 调整前 → 调整后的价格，涨价红色、降价绿色，未变化时只显示价格 */
const props = defineProps({ old: [Number, String], new: [Number, String] })
const newValue = computed(() => props.new)
const same = computed(() => props.old != null && props.new != null && Number(props.old) === Number(props.new))
const up = computed(() => props.old == null || Number(props.new) > Number(props.old))
</script>

<style scoped>
.old {
  color: var(--app-text-3);
}
.arrow {
  margin: 0 6px;
  font-size: 11px;
  color: var(--app-text-3);
}
.up {
  color: #cf1322;
}
.down {
  color: #389e0d;
}
</style>
