<template>
  <div class="pos-member">
    <template v-if="member">
      <div class="member-name">
        <IdcardOutlined /> {{ member.name || member.memberCode }}
        <a-tag color="gold">{{ member.levelName || $t('pos.cashier.member') }}</a-tag>
        <a-button type="link" size="small" @click.stop="emit('clear')">{{ $t('common.action.clear') }}</a-button>
      </div>
      <div class="sub-text">{{ member.memberCode }} · {{ member.phone }}</div>
      <div class="account">
        <span>{{ $t('pos.cashier.points') }} <b>{{ qty(member.points) }}</b></span>
        <span>{{ $t('pos.cashier.balance') }} <b>{{ money(member.balance) }}</b></span>
        <a-button v-if="canTopup" size="small" @click.stop="emit('topup')">{{ $t('pos.cashier.keyTopup') }} (F10)</a-button>
      </div>
    </template>
    <a-button v-else block @click.stop="emit('identify')">{{ $t('pos.cashier.identifyMember') }} (F3)</a-button>
  </div>
</template>

<script setup>
import { IdcardOutlined } from '@ant-design/icons-vue'
import { money, qty } from '@/utils/number'

/** 收银台的会员信息：等级、积分、储值余额，可充值（pos:sale:topup） */
defineProps({ member: Object, canTopup: Boolean })
const emit = defineEmits(['clear', 'identify', 'topup'])
</script>

<style scoped>
.pos-member {
  background: var(--app-card-bg, #fff);
  border-radius: 8px;
  padding: 12px;
}
.member-name {
  font-weight: 600;
}
.account {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 6px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
