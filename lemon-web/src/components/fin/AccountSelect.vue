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
    :dropdown-match-select-width="400"
    @change="(v) => emit('update:value', v ?? null)"
  />
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { accountOptions, accountScopes, allAccounts, cashAccountOptions } from '@/api/fin'
import { cached } from './fin'
import { t } from '@/locales'
import { AccountRole } from '@/constants/enums'

const CASH_ROLES = [AccountRole.CASH, AccountRole.BANK]

/**
 * 科目选择：默认只能选启用的末级科目（凭证、结算账户）；all 为 true 时可选上级科目（账簿）。
 * prefix：只列出编码以其中之一开头的科目；
 * roles：只列出科目范围用途（与后端 AccountRole 一致）下的科目，如 ['CASH', 'BANK'] 限定为现金、银行，科目按「凭证科目设置」；
 * 不是 all 且限定了现金、银行时（结算账户）只列出当前用户可用的账户（各门店共用的 + 可管辖门店的）
 */
const props = defineProps({
  value: Number,
  all: Boolean,
  prefix: Array,
  roles: Array,
  placeholder: { type: String, default: () => t('fin.accountSelect.placeholder') },
  allowClear: Boolean,
  disabled: Boolean
})
const emit = defineEmits(['update:value'])

const list = ref([])
const scopes = ref(null)
const loading = ref(false)
/** 限定的编码前缀：prefix 与 roles 对应的科目编码；roles 未加载完时不列出科目 */
const prefixes = computed(() => {
  if (!props.roles) return props.prefix
  if (!scopes.value) return []
  return [...(props.prefix || []), ...props.roles.map((r) => scopes.value[r]).filter(Boolean)]
})
/** 结算账户：限定的科目范围只有现金、银行 */
const settlement = computed(() => !!props.roles?.length && props.roles.every((r) => CASH_ROLES.includes(r)))
const options = computed(() =>
  list.value
    .filter((a) => !prefixes.value || prefixes.value.some((p) => a.code.startsWith(p)))
    .map((a) => ({ value: a.id, label: `${a.code} ${props.all ? '\u3000'.repeat((a.code.length - 4) / 2) : ''}${a.name}` }))
)

onMounted(async () => {
  loading.value = true
  try {
    const [accounts, codes] = await Promise.all([
      props.all
        ? cached('accounts-all', allAccounts)
        : settlement.value
          ? cached('accounts-cash', cashAccountOptions)
          : cached('accounts-leaf', accountOptions),
      props.roles ? cached('account-scopes', accountScopes) : null
    ])
    list.value = accounts
    scopes.value = codes
  } finally {
    loading.value = false
  }
})
</script>
