<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'

import { fetchRuntimeSnapshot } from '../../../api/monitor'
import type { DataRecord } from '../../../api/admin'
import AdminLayout from '../../../layouts/admin/index.vue'

const loading = ref(false)
const snapshot = ref<DataRecord | null>(null)
const errorMessage = ref('')

const hasSnapshot = computed(() => Boolean(snapshot.value && Object.keys(snapshot.value).length > 0))
const sections = computed(() => {
  const value = snapshot.value ?? {}
  return [
    { title: '宿主机', data: value.hostInfo },
    { title: 'JVM', data: value.jvmInfo },
    { title: '数据库', data: value.databaseInfo },
    { title: '中间件', data: value.middlewareInfo },
  ]
})

function stringify(value: unknown): string {
  if (value === null || value === undefined) {
    return '-'
  }
  if (typeof value === 'object') {
    return JSON.stringify(value, null, 2)
  }
  return String(value)
}

async function loadSnapshot() {
  loading.value = true
  errorMessage.value = ''
  try {
    snapshot.value = await fetchRuntimeSnapshot()
  } catch (error: unknown) {
    snapshot.value = null
    errorMessage.value = error instanceof Error ? error.message : '加载监控快照失败'
    message.error(errorMessage.value)
  } finally {
    loading.value = false
  }
}

onMounted(loadSnapshot)
</script>

<template>
  <AdminLayout>
    <a-card class="admin-page-card" :loading="loading">
      <template #title>
        <div class="page-heading">
          <div>
            <h2>运行监控</h2>
            <p>查看宿主机、JVM、数据库和中间件运行快照，用于快速判断系统运行状态。</p>
          </div>
          <a-button type="primary" @click="loadSnapshot">刷新</a-button>
        </div>
      </template>
      <a-result
        v-if="errorMessage"
        status="warning"
        title="监控快照加载失败"
        :sub-title="errorMessage"
      >
        <template #extra>
          <a-button type="primary" @click="loadSnapshot">重新加载</a-button>
        </template>
      </a-result>
      <a-empty
        v-else-if="!hasSnapshot"
        description="暂无运行监控快照。请确认监控接口已启用，或点击刷新重新拉取。"
      />
      <a-row v-else :gutter="[16, 16]" class="monitor-grid">
        <a-col v-for="section in sections" :key="section.title" :xs="24" :xl="12">
          <a-card class="admin-section-card" :title="section.title" size="small">
            <pre class="monitor-json">{{ stringify(section.data) }}</pre>
          </a-card>
        </a-col>
      </a-row>
    </a-card>
  </AdminLayout>
</template>
