<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('channel.common.shop')" name="shopId">
          <WxShopSelect v-model:value="query.shopId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('channel.log.syncType')" name="syncType">
          <a-select v-model:value="query.syncType" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ch_wx_sync_type)" />
        </a-form-item>
        <a-form-item :label="$t('channel.log.result')" name="success">
          <a-select v-model:value="query.success" :placeholder="$t('common.placeholder.all')" allow-clear :options="resultOptions" />
        </a-form-item>
        <a-form-item :label="$t('channel.log.date')" name="dateRange">
          <a-range-picker v-model:value="query.dateRange" value-format="YYYY-MM-DD" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load" />

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: scrollX, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'syncType'"><DictTag type="ch_wx_sync_type" :value="record.syncType" /></template>
          <template v-else-if="column.key === 'syncTrigger'"><DictTag type="ch_wx_sync_trigger" :value="record.syncTrigger" /></template>
          <template v-else-if="column.key === 'success'">
            <a-tag :color="record.success ? 'green' : 'red'">{{ record.success ? $t('channel.log.ok') : $t('channel.log.failed') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'counts'">
            {{ $t('channel.log.counts', { total: record.totalCount, succeeded: record.successCount, failed: record.failCount }) }}
          </template>
          <template v-else-if="column.key === 'duration'">{{ $t('channel.log.ms', { n: record.durationMs }) }}</template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import WxShopSelect from '@/components/channel/WxShopSelect.vue'
import { pageWxSyncLog } from '@/api/channel/wxShop'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

/** 微信小店同步日志：每次同步（店铺、类型）一条，含触发方式、条数、耗时和第一条失败的原因 */
const { t } = useI18n()
const dict = useDict('ch_wx_sync_type', 'ch_wx_sync_trigger')

const resultOptions = [
  { value: true, label: t('channel.log.ok') },
  { value: false, label: t('channel.log.failed') }
]
const columns = [
  { title: t('channel.log.time'), dataIndex: 'createTime', width: 170 },
  { title: t('channel.common.shop'), dataIndex: 'shopName', width: 160, ellipsis: true },
  { title: t('channel.log.syncType'), key: 'syncType', width: 90, dict: 'ch_wx_sync_type', dataIndex: 'syncType' },
  { title: t('channel.log.trigger'), key: 'syncTrigger', width: 100, dict: 'ch_wx_sync_trigger', dataIndex: 'syncTrigger' },
  { title: t('channel.log.result'), key: 'success', width: 80 },
  { title: t('channel.log.countsTitle'), key: 'counts', width: 200 },
  { title: t('channel.log.duration'), key: 'duration', width: 100 },
  { title: t('channel.log.message'), dataIndex: 'message', ellipsis: true }
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'channel-wx-sync-log')
const showSearch = ref(true)

const emptyQuery = () => ({ shopId: undefined, syncType: undefined, success: undefined, dateRange: undefined })
const query = reactive(emptyQuery())
const fetcher = ({ dateRange, ...params }) => pageWxSyncLog({ ...params, beginDate: dateRange?.[0], endDate: dateRange?.[1] })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(fetcher, query)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

onMounted(load)
</script>
