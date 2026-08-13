<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'

import {
  createStorageDirectory,
  deleteStorageObject,
  fetchEnabledStorageInstances,
  fetchStorageObjects,
} from '../../../api/file'
import type { DataRecord } from '../../../api/admin'
import { usePermission } from '../../../composables/usePermission'
import AdminLayout from '../../../layouts/admin/index.vue'

const { hasPermission } = usePermission()
const loading = ref(false)
const instanceId = ref('')
const currentPath = ref('')
const newDirectory = ref('')
const instances = ref<DataRecord[]>([])
const rows = ref<DataRecord[]>([])

const hasInstance = computed(() => instanceOptions.value.length > 0)
const instanceOptions = computed(() => instances.value.map(item => ({
  label: String(item.instanceName ?? item.instanceId ?? '存储实例'),
  value: String(item.instanceId ?? ''),
})).filter(item => item.value.length > 0))
const emptyDescription = computed(() => {
  if (!hasInstance.value) {
    return '暂无启用的存储实例。请先在“存储实例”页面新增并启用实例。'
  }
  return '当前路径暂无对象。可切换路径、刷新列表，或创建一个目录。'
})
const columns = [
  { title: '对象 Key', dataIndex: 'objectKey', ellipsis: true },
  { title: '名称', dataIndex: 'name', ellipsis: true },
  { title: '大小', dataIndex: 'size', width: 120 },
  { title: '类型', dataIndex: 'type', width: 120 },
  { title: '最后修改时间', dataIndex: 'lastModified', width: 190 },
  { title: '操作', dataIndex: 'actions', width: 120, fixed: 'right' as const },
]

async function loadInstances() {
  instances.value = await fetchEnabledStorageInstances()
  if (!instanceId.value && instanceOptions.value.length > 0) {
    instanceId.value = instanceOptions.value[0].value
  }
}

async function loadObjects() {
  if (!instanceId.value) {
    rows.value = []
    return
  }
  loading.value = true
  try {
    rows.value = await fetchStorageObjects(instanceId.value, currentPath.value)
  } catch (error: unknown) {
    message.error(error instanceof Error ? error.message : '加载对象列表失败')
  } finally {
    loading.value = false
  }
}

async function createDirectory() {
  const directory = newDirectory.value.trim()
  if (!instanceId.value) {
    message.warning('请先选择存储实例')
    return
  }
  if (!directory) {
    message.warning('请填写新目录')
    return
  }
  await createStorageDirectory(instanceId.value, directory)
  message.success('目录已创建')
  newDirectory.value = ''
  await loadObjects()
}

async function removeObject(record: DataRecord) {
  const objectKey = String(record.objectKey ?? record.key ?? '')
  if (!objectKey) {
    message.warning('缺少对象 Key')
    return
  }
  await deleteStorageObject(instanceId.value, objectKey)
  message.success('对象已删除')
  await loadObjects()
}

onMounted(async () => {
  await loadInstances()
  await loadObjects()
})
</script>

<template>
  <AdminLayout>
    <a-card class="admin-page-card">
      <template #title>
        <div class="page-heading">
          <div>
            <h2>对象浏览</h2>
            <p>浏览已启用存储实例中的对象和目录，支持创建目录与删除对象。</p>
          </div>
          <a-space>
            <a-button @click="loadObjects">刷新</a-button>
          </a-space>
        </div>
      </template>
      <a-form layout="inline" class="admin-inline-form">
        <a-form-item label="存储实例">
          <a-select
            v-model:value="instanceId"
            class="context-select"
            :options="instanceOptions"
            placeholder="选择存储实例"
            @change="loadObjects"
          />
        </a-form-item>
        <a-form-item label="路径">
          <a-input v-model:value="currentPath" placeholder="/" @press-enter="loadObjects" />
        </a-form-item>
        <a-form-item label="新目录">
          <a-input v-model:value="newDirectory" placeholder="例如 reports/2026" @press-enter="createDirectory" />
        </a-form-item>
        <a-form-item>
          <a-button
            v-if="hasPermission('objectBrowser:add')"
            type="primary"
            :disabled="!hasInstance"
            @click="createDirectory"
          >
            创建目录
          </a-button>
        </a-form-item>
      </a-form>
      <a-table
        class="admin-section admin-data-table"
        :loading="loading"
        :data-source="rows"
        row-key="objectKey"
        :columns="columns"
        :scroll="{ x: 'max-content' }"
      >
        <template #emptyText>
          <a-empty :description="emptyDescription" />
        </template>
        <template #bodyCell="{ column, record }">
          <a-popconfirm
            v-if="column.dataIndex === 'actions' && hasPermission('objectBrowser:delete')"
            title="确认删除该对象？"
            @confirm="removeObject(record as DataRecord)"
          >
            <a-button type="link" danger>删除</a-button>
          </a-popconfirm>
        </template>
      </a-table>
    </a-card>
  </AdminLayout>
</template>
