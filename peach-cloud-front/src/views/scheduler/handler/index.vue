<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import AdminLayout from '../components/SchedulerLayout.vue'
import { fetchHandlers } from '../../../api/scheduler'
import type { SchedulerHandler } from '../../../types/scheduler'
import { displayTime, errorText } from '../shared'

const rows = ref<SchedulerHandler[]>([])
const loading = ref(false)
const failure = ref('')
const keyword = ref('')
const application = ref('')
const status = ref<string>()
let sequence = 0
onBeforeUnmount(() => { sequence++ })
const columns = [
  { title: '应用名称', dataIndex: 'applicationName', width: 200 }, { title: 'Handler', dataIndex: 'handlerName', width: 200 },
  { title: '说明', dataIndex: 'description', width: 240 }, { title: '在线状态', key: 'status', width: 110 },
  { title: '实例', dataIndex: 'instanceId', width: 240 }, { title: '最近心跳', key: 'lastHeartbeatTime', width: 190 },
]
async function load() {
  const current = ++sequence
  loading.value = true
  failure.value = ''
  try { const result = await fetchHandlers({ keyword: keyword.value.trim(), applicationName: application.value, status: status.value }); if (current === sequence) rows.value = result }
  catch (error: unknown) { if (current === sequence) { rows.value = []; failure.value = errorText(error) } }
  finally { if (current === sequence) loading.value = false }
}
onMounted(load)
</script>

<template>
  <AdminLayout>
    <a-card class="admin-page-card">
      <template #title><div class="page-heading"><div><h2>Handler 列表</h2><p>查看业务服务自动注册的处理器及其心跳在线状态。</p></div></div></template>
      <a-space wrap class="scheduler-toolbar">
        <a-input v-model:value="keyword" aria-label="搜索应用或 Handler" placeholder="搜索应用、Handler 或说明" allow-clear @press-enter="load" />
        <a-input v-model:value="application" aria-label="应用" placeholder="应用名称（精确匹配）" allow-clear @press-enter="load" />
        <a-select v-model:value="status" aria-label="在线状态" placeholder="全部在线状态" allow-clear :options="[{ value: 'ONLINE', label: '在线' }, { value: 'OFFLINE', label: '离线' }]" style="width: 150px" />
        <a-button @click="keyword = ''; application = ''; status = undefined; load()">重置</a-button>
        <a-button type="primary" :loading="loading" @click="load">查询</a-button>
        <a-button :disabled="loading" @click="load">刷新</a-button>
      </a-space>
      <a-alert v-if="failure" type="error" show-icon :message="failure"><template #action><a-button size="small" @click="load">重试</a-button></template></a-alert>
      <a-table row-key="id" :columns="columns" :data-source="rows" :loading="loading" :scroll="{ x: 1170 }" :pagination="{ pageSize: 20, showSizeChanger: false }" :locale="{ emptyText: failure ? '加载失败，请重试' : keyword ? '没有匹配的 Handler' : '暂无 Handler' }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'"><a-tag :color="record.status === 'ONLINE' ? 'success' : 'default'">{{ record.status === 'ONLINE' ? '在线' : '离线' }}</a-tag></template>
          <template v-else-if="column.key === 'lastHeartbeatTime'">{{ displayTime(record.lastHeartbeatTime) }}</template>
        </template>
      </a-table>
    </a-card>
  </AdminLayout>
</template>

<style scoped>
.scheduler-toolbar { margin-bottom: var(--space-4); }
</style>
