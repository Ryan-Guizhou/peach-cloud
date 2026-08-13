<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'

import { deleteById, fetchPage, postRecord, type CrudEndpoint, type DataRecord } from '../../api/admin'
import { usePermission } from '../../composables/usePermission'
import AdminLayout from '../../layouts/admin/index.vue'

export interface CrudColumn {
  title: string
  dataIndex: string
  width?: number
  ellipsis?: boolean
}

export interface CrudField {
  label: string
  name: string
  required?: boolean
  type?: 'text' | 'number' | 'textarea'
}

const props = defineProps<{
  title: string
  description: string
  endpoint: CrudEndpoint
  columns: CrudColumn[]
  fields: CrudField[]
  rowKey: string
  permissionPrefix: string
}>()

const { hasPermission } = usePermission()
const query = reactive<DataRecord>({ pageNum: 1, pageSize: 10 })
const rows = ref<DataRecord[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const modalOpen = ref(false)
const editingId = ref('')
const errorMessage = ref('')
const formState = reactive<DataRecord>({})

const hasRowActions = computed(() => Boolean(props.endpoint.update || props.endpoint.delById))
const canAdd = computed(() => Boolean(props.endpoint.add) && props.fields.length > 0 && hasPermission(`${props.permissionPrefix}:add`))
const canUpdate = computed(() => Boolean(props.endpoint.update) && props.fields.length > 0 && hasPermission(`${props.permissionPrefix}:update`))
const canDelete = computed(() => Boolean(props.endpoint.delById) && hasPermission(`${props.permissionPrefix}:delete`))
const emptyDescription = computed(() => {
  if (canAdd.value) {
    return `暂无${props.title}数据，可点击右上角新增创建第一条记录。`
  }
  return `暂无${props.title}数据，可点击刷新重新获取，或确认当前账号是否有对应数据权限。`
})
const tableColumns = computed(() => {
  const columns = props.columns.map(column => ({ ellipsis: true, ...column }))
  if (!hasRowActions.value) {
    return columns
  }
  return [
    ...columns,
    {
      title: '操作',
      dataIndex: 'actions',
      width: 180,
      fixed: 'right' as const,
    },
  ]
})

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    const page = await fetchPage<DataRecord>(props.endpoint.pageList, query)
    rows.value = page.list ?? []
    total.value = page.total ?? rows.value.length
  } catch (error: unknown) {
    rows.value = []
    total.value = 0
    errorMessage.value = error instanceof Error ? error.message : '加载数据失败'
    message.error(errorMessage.value)
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
  props.fields.forEach((field) => {
    formState[field.name] = undefined
  })
  modalOpen.value = true
}

function openEdit(record: DataRecord) {
  if (!canUpdate.value) {
    return
  }
  editingId.value = String(record[props.rowKey] ?? '')
  resetFormState()
  props.fields.forEach((field) => {
    formState[field.name] = record[field.name] as string | number | boolean | null | undefined
  })
  modalOpen.value = true
}

function validateRequiredFields(): boolean {
  const missingField = props.fields.find((field) => {
    const value = formState[field.name]
    return field.required && (value === undefined || value === null || String(value).trim().length === 0)
  })
  if (!missingField) {
    return true
  }
  message.warning(`请填写${missingField.label}`)
  return false
}

async function saveRecord() {
  if (!validateRequiredFields()) {
    return
  }
  const url = editingId.value ? props.endpoint.update : props.endpoint.add
  if (!url) {
    message.warning('当前接口未配置保存地址')
    return
  }
  saving.value = true
  try {
    const payload: DataRecord = { ...formState }
    if (editingId.value) {
      payload[props.rowKey] = editingId.value
    }
    await postRecord(url, payload)
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
  if (!canDelete.value || !props.endpoint.delById) {
    message.warning('当前接口未配置删除地址')
    return
  }
  const id = String(record[props.rowKey] ?? '')
  if (!id) {
    message.warning('缺少主键，无法删除')
    return
  }
  try {
    await deleteById(props.endpoint.delById, props.endpoint.idField ?? props.rowKey, id, props.endpoint.deleteMode)
    message.success('删除成功')
    await loadData()
  } catch (error: unknown) {
    message.error(error instanceof Error ? error.message : '删除失败')
  }
}

function handleTableChange(pagination: { current?: number; pageSize?: number }) {
  query.pageNum = pagination.current ?? 1
  query.pageSize = pagination.pageSize ?? 10
  void loadData()
}

onMounted(loadData)
</script>

<template>
  <AdminLayout>
    <a-card class="admin-page-card">
      <template #title>
        <div class="page-heading">
          <div>
            <h2>{{ title }}</h2>
            <p>{{ description }}</p>
          </div>
          <a-space>
            <a-button @click="loadData">刷新</a-button>
            <a-button v-if="canAdd" type="primary" @click="openCreate">新增</a-button>
          </a-space>
        </div>
      </template>

      <a-table
        class="admin-data-table"
        :columns="tableColumns"
        :data-source="rows"
        :row-key="rowKey"
        :loading="loading"
        :scroll="{ x: 'max-content' }"
        :pagination="{ current: Number(query.pageNum), pageSize: Number(query.pageSize), total, showSizeChanger: true }"
        @change="handleTableChange"
      >
        <template #emptyText>
          <a-result
            v-if="errorMessage"
            status="warning"
            title="数据加载失败"
            :sub-title="errorMessage"
          >
            <template #extra>
              <a-button type="primary" @click="loadData">重新加载</a-button>
            </template>
          </a-result>
          <a-empty v-else :description="emptyDescription" />
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'actions'">
            <a-space>
              <a-button v-if="canUpdate" type="link" @click="openEdit(record as DataRecord)">编辑</a-button>
              <a-popconfirm v-if="canDelete" title="确认删除该记录？" @confirm="removeRecord(record as DataRecord)">
                <a-button type="link" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalOpen"
      :title="editingId ? `编辑${title}` : `新增${title}`"
      :confirm-loading="saving"
      @ok="saveRecord"
    >
      <a-form layout="vertical">
        <a-form-item v-for="field in fields" :key="field.name" :label="field.label" :required="field.required">
          <a-input-number
            v-if="field.type === 'number'"
            v-model:value="formState[field.name] as number | undefined"
            class="full-control"
          />
          <a-textarea
            v-else-if="field.type === 'textarea'"
            v-model:value="formState[field.name] as string | undefined"
            :rows="4"
          />
          <a-input v-else v-model:value="formState[field.name] as string | undefined" />
        </a-form-item>
      </a-form>
    </a-modal>
  </AdminLayout>
</template>
