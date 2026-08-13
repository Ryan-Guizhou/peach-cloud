<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'

import { deleteStorageInstance, fetchStorageInstances, saveStorageInstance } from '../../../api/file'
import type { DataRecord } from '../../../api/admin'
import { usePermission } from '../../../composables/usePermission'
import AdminLayout from '../../../layouts/admin/index.vue'

const { hasPermission } = usePermission()
const loading = ref(false)
const saving = ref(false)
const modalOpen = ref(false)
const rows = ref<DataRecord[]>([])
const editingId = ref('')
const formState = reactive<DataRecord>({})

const canAdd = computed(() => hasPermission('storageInstance:add'))
const columns = [
  { title: '实例ID', dataIndex: 'instanceId', ellipsis: true },
  { title: '实例名称', dataIndex: 'instanceName', ellipsis: true },
  { title: '存储类型', dataIndex: 'storeType', width: 130 },
  { title: 'Endpoint', dataIndex: 'endpoint', ellipsis: true },
  { title: 'Bucket', dataIndex: 'bucketName', ellipsis: true },
  { title: '启用状态', dataIndex: 'enabled', width: 110 },
  { title: '内置', dataIndex: 'builtIn', width: 90 },
  { title: '操作', dataIndex: 'actions', width: 160, fixed: 'right' as const },
]

async function loadData() {
  loading.value = true
  try {
    rows.value = await fetchStorageInstances()
  } catch (error: unknown) {
    message.error(error instanceof Error ? error.message : '加载存储实例失败')
  } finally {
    loading.value = false
  }
}

function resetFormState() {
  Object.keys(formState).forEach(key => delete formState[key])
}

function openCreate() {
  if (!canAdd.value) {
    return
  }
  editingId.value = ''
  resetFormState()
  modalOpen.value = true
}

function openEdit(record: DataRecord) {
  if (!hasPermission('storageInstance:update')) {
    return
  }
  editingId.value = String(record.instanceId ?? '')
  resetFormState()
  Object.assign(formState, record)
  modalOpen.value = true
}

function validateForm(): boolean {
  if (!String(formState.instanceName ?? '').trim()) {
    message.warning('请填写实例名称')
    return false
  }
  if (!String(formState.storeType ?? '').trim()) {
    message.warning('请填写存储类型')
    return false
  }
  return true
}

async function saveData() {
  if (!validateForm()) {
    return
  }
  saving.value = true
  try {
    await saveStorageInstance({ ...formState }, Boolean(editingId.value))
    message.success('保存成功')
    modalOpen.value = false
    await loadData()
  } catch (error: unknown) {
    message.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function removeRecord(record: DataRecord) {
  if (!hasPermission('storageInstance:delete')) {
    return
  }
  const instanceId = String(record.instanceId ?? '')
  if (!instanceId) {
    message.warning('缺少实例ID，无法删除')
    return
  }
  try {
    await deleteStorageInstance(instanceId)
    message.success('删除成功')
    await loadData()
  } catch (error: unknown) {
    message.error(error instanceof Error ? error.message : '删除失败')
  }
}

function enabledText(value: unknown): string {
  return Number(value) === 1 ? '启用' : '停用'
}

function booleanText(value: unknown): string {
  return Number(value) === 1 ? '是' : '否'
}

onMounted(loadData)
</script>

<template>
  <AdminLayout>
    <a-card class="admin-page-card">
      <template #title>
        <div class="page-heading">
          <div>
            <h2>存储实例</h2>
            <p>维护对象存储实例配置，支持多 Provider 与启停管理。</p>
          </div>
          <a-space>
            <a-button @click="loadData">刷新</a-button>
            <a-button v-if="canAdd" type="primary" @click="openCreate">新增</a-button>
          </a-space>
        </div>
      </template>
      <a-table
        class="admin-data-table"
        :loading="loading"
        :data-source="rows"
        row-key="instanceId"
        :columns="columns"
        :scroll="{ x: 'max-content' }"
      >
        <template #emptyText>
          <a-empty description="暂无存储实例。可新增一个实例后，再到对象浏览页查看文件对象。" />
        </template>
        <template #bodyCell="{ column, record }">
          <a-tag v-if="column.dataIndex === 'enabled'" :color="Number(record.enabled) === 1 ? 'green' : 'default'">
            {{ enabledText(record.enabled) }}
          </a-tag>
          <a-tag v-else-if="column.dataIndex === 'builtIn'" :color="Number(record.builtIn) === 1 ? 'blue' : 'default'">
            {{ booleanText(record.builtIn) }}
          </a-tag>
          <a-space v-else-if="column.dataIndex === 'actions'">
            <a-button v-if="hasPermission('storageInstance:update')" type="link" @click="openEdit(record as DataRecord)">编辑</a-button>
            <a-popconfirm
              v-if="hasPermission('storageInstance:delete')"
              title="确认删除该存储实例？"
              @confirm="removeRecord(record as DataRecord)"
            >
              <a-button type="link" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="modalOpen" title="存储实例" :confirm-loading="saving" @ok="saveData">
      <a-form layout="vertical">
        <a-form-item label="实例ID">
          <a-input v-model:value="formState.instanceId as string | undefined" :disabled="Boolean(editingId)" />
        </a-form-item>
        <a-form-item label="实例名称" required>
          <a-input v-model:value="formState.instanceName as string | undefined" />
        </a-form-item>
        <a-form-item label="存储类型" required>
          <a-input v-model:value="formState.storeType as string | undefined" />
        </a-form-item>
        <a-form-item label="Endpoint">
          <a-input v-model:value="formState.endpoint as string | undefined" />
        </a-form-item>
        <a-form-item label="Region">
          <a-input v-model:value="formState.region as string | undefined" />
        </a-form-item>
        <a-form-item label="Bucket">
          <a-input v-model:value="formState.bucketName as string | undefined" />
        </a-form-item>
        <a-form-item label="前缀">
          <a-input v-model:value="formState.prefix as string | undefined" />
        </a-form-item>
        <a-form-item label="Root Path">
          <a-input v-model:value="formState.rootPath as string | undefined" />
        </a-form-item>
        <a-form-item label="访问域名">
          <a-input v-model:value="formState.domain as string | undefined" />
        </a-form-item>
        <a-form-item label="启用状态">
          <a-input-number v-model:value="formState.enabled as number | undefined" class="full-control" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="formState.remark as string | undefined" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </AdminLayout>
</template>
