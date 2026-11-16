<template>
  <ScreenRunner
    :screens="screens"
    :interval="interval"
    :fetcher="fetch"
    :company="company"
    closable
    @close="close"
  />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { screenData } from '@/api/bi/screen'
import { SCREENS } from '@/components/screen/screen'
import { useConfigStore } from '@/store/config'
import { useUserStore } from '@/store/user'
import ScreenRunner from './ScreenRunner.vue'

/**
 * 登录后打开的大屏：/screen/sales，多个大屏轮播 /screen/sales?rotate=sales,warehouse&interval=60
 */
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const configStore = useConfigStore()

const screens = computed(() => {
  const keys = String(route.query.rotate || route.params.key || '').toUpperCase().split(',')
  const list = keys.map((k) => SCREENS.find((s) => s.key === k)).filter(Boolean)
  return list.length ? list : [SCREENS[0]]
})
const interval = computed(() => Number(route.query.interval) || 60)
const company = computed(() => userStore.tenantName || configStore.systemName)
const fetch = (key) => screenData(key)

function close() {
  if (window.history.length > 1) router.back()
  else router.push('/bi/screen')
}
</script>
