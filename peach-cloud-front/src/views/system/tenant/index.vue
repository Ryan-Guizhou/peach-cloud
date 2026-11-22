<script setup lang="ts">
import { onMounted, onBeforeUnmount, reactive, ref } from 'vue'
import { message, type FormInstance } from 'ant-design-vue'
import AdminLayout from '../../../layouts/admin/index.vue'
import { usePermission } from '../../../composables/usePermission'
import { fetchTenants, fetchTenant, saveTenant, changeTenantStatus, deleteTenant, type Tenant, type TenantQuery, type TenantSave, type TenantStatus } from '../../../api/tenant'

const { hasPermission } = usePermission()
const query = reactive<TenantQuery>({ pageNum: 1, pageSize: 10 })
const rows = ref<Tenant[]>([])
const total = ref(0)
const loading = ref(false)
const errorMessage = ref('')
const modalOpen = ref(false)
const saving = ref(false)
const detailLoading = ref(false)
const mutating = ref(false)
const editing = ref<Tenant>()
const formRef = ref<FormInstance>()
const form = reactive<TenantSave>(emptyForm())
const statusLabels: Record<TenantStatus, string> = { PENDING: '待激活', ACTIVE: '已激活', SUSPENDED: '已暂停', CLOSED: '已关闭' }
const statusOptions = Object.entries(statusLabels).map(([value, label]) => ({ value, label }))
const typeOptions = [{ value: 'ENTERPRISE', label: '企业' }, { value: 'PERSONAL', label: '个人' }]
const transitions: Record<TenantStatus, TenantStatus[]> = { PENDING: ['ACTIVE', 'CLOSED'], ACTIVE: ['SUSPENDED', 'CLOSED'], SUSPENDED: ['ACTIVE', 'CLOSED'], CLOSED: [] }
const actionLabels: Record<TenantStatus, string> = { PENDING: '待激活', ACTIVE: '激活', SUSPENDED: '暂停', CLOSED: '关闭' }
const columns = [
  { title: '租户编码', dataIndex: 'tenantCode', width: 180, ellipsis: true },
  { title: '租户名称', dataIndex: 'tenantName', width: 200, ellipsis: true },
  { title: '类型', dataIndex: 'tenantType', width: 110 },
  { title: '状态', dataIndex: 'status', width: 110 },
  { title: '联系人', dataIndex: 'contactName', width: 140, ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', width: 190 },
  { title: '操作', dataIndex: 'actions', width: 300, fixed: 'right' as const },
]
let controller: AbortController | undefined
let detailGeneration = 0
let disposed = false

function emptyForm(): TenantSave {
  return { tenantCode: '', tenantName: '', tenantType: 'ENTERPRISE', description: '', contactName: '', contactPhone: '', contactEmail: '' }
}

async function loadData() {
  controller?.abort()
  const request = new AbortController()
  controller = request
  loading.value = true
  errorMessage.value = ''
  try {
    const page = await fetchTenants({ ...query }, request.signal)
    if (request.signal.aborted) return
    rows.value = page.list
    total.value = page.total
    if (rows.value.length === 0 && total.value > 0 && query.pageNum > 1) {
      query.pageNum = Math.max(1, Math.ceil(total.value / query.pageSize))
      await loadData()
    }
  } catch (error: unknown) {
    if (request.signal.aborted) return
    rows.value = []
    total.value = 0
    errorMessage.value = error instanceof Error ? error.message : '加载租户失败'
  } finally {
    if (controller === request && !disposed) loading.value = false
  }
}

function search() {
  query.pageNum = 1
  void loadData()
}

function openCreate() {
  if (!hasPermission('tenant:add')) return
  detailGeneration++
  editing.value = undefined
  Object.assign(form, emptyForm())
  formRef.value?.clearValidate()
  modalOpen.value = true
}

async function openEdit(row: Tenant) {
  if (!hasPermission('tenant:update') || detailLoading.value) return
  const generation = ++detailGeneration
  detailLoading.value = true
  try {
    const detail = await fetchTenant(row.id)
    if (disposed || generation !== detailGeneration) return
    editing.value = detail
    Object.assign(form, { tenantCode: detail.tenantCode, tenantName: detail.tenantName, tenantType: detail.tenantType, description: detail.description ?? '', contactName: detail.contactName ?? '', contactPhone: detail.contactPhone ?? '', contactEmail: detail.contactEmail ?? '' })
    formRef.value?.clearValidate()
    modalOpen.value = true
  } catch (error: unknown) {
    if (!disposed) message.error(error instanceof Error ? error.message : '加载租户详情失败')
  } finally {
    if (!disposed) detailLoading.value = false
  }
}

async function save() {
  if (saving.value || !hasPermission(editing.value ? 'tenant:update' : 'tenant:add')) return
  try { await formRef.value?.validate() } catch { return }
  saving.value = true
  try {
    await saveTenant({ ...form }, editing.value ? { id: editing.value.id, version: editing.value.version } : undefined)
    if (disposed) return
    message.success('保存成功')
    modalOpen.value = false
    await loadData()
  } catch (error: unknown) {
    if (!disposed) message.error(error instanceof Error ? error.message : '保存失败，请重试；数据已变更时请关闭后重新编辑')
  } finally {
    if (!disposed) saving.value = false
  }
}

async function mutate(row: Tenant, status?: TenantStatus) {
  if (mutating.value || !hasPermission(status ? 'tenant:status' : 'tenant:delete')) return
  mutating.value = true
  try {
    if (status) await changeTenantStatus(row, status)
    else await deleteTenant(row)
    if (disposed) return
    message.success(status ? '状态已更新' : '租户已删除')
    await loadData()
  } catch (error: unknown) {
    if (!disposed) message.error(error instanceof Error ? error.message : '操作失败，请刷新后重试')
  } finally {
    if (!disposed) mutating.value = false
  }
}

function paginate(page: { current?: number; pageSize?: number }) {
  query.pageNum = page.current ?? 1
  query.pageSize = page.pageSize ?? 10
  void loadData()
}

onMounted(() => { void loadData() })
onBeforeUnmount(() => { disposed = true; detailGeneration++; controller?.abort() })
</script>

<template>
  <AdminLayout>
    <a-card class="admin-page-card">
      <template #title>
        <div class="page-heading">
          <div><h2>租户管理</h2><p>维护租户资料，管理激活、暂停与关闭状态。</p></div>
          <a-space wrap>
            <a-button :loading="loading" @click="loadData">刷新</a-button>
            <a-button v-if="hasPermission('tenant:add')" type="primary" :disabled="detailLoading || saving" @click="openCreate">新增租户</a-button>
          </a-space>
        </div>
      </template>
      <a-form layout="inline" class="tenant-filters" @finish="search">
        <a-form-item label="关键词"><a-input v-model:value="query.keyword" placeholder="租户编码或名称" allow-clear :maxlength="128" /></a-form-item>
        <a-form-item label="状态"><a-select v-model:value="query.status" :options="statusOptions" placeholder="全部状态" allow-clear class="tenant-select" /></a-form-item>
        <a-form-item label="类型"><a-select v-model:value="query.tenantType" :options="typeOptions" placeholder="全部类型" allow-clear class="tenant-select" /></a-form-item>
        <a-form-item><a-button html-type="submit" type="primary">查询</a-button></a-form-item>
      </a-form>
      <a-table class="admin-data-table" :columns="columns" :data-source="rows" row-key="id" :loading="loading" :scroll="{ x: 'max-content' }" :pagination="{ current: query.pageNum, pageSize: query.pageSize, total, showSizeChanger: true }" @change="paginate">
        <template #emptyText>
          <a-result v-if="errorMessage" status="warning" title="租户加载失败" :sub-title="errorMessage"><template #extra><a-button @click="loadData">重新加载</a-button></template></a-result>
          <a-empty v-else description="暂无租户，请调整查询条件或新增租户。" />
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'status'"><a-tag>{{ statusLabels[(record as Tenant).status] }}</a-tag></template>
          <template v-else-if="column.dataIndex === 'tenantType'">{{ typeOptions.find(option => option.value === record.tenantType)?.label ?? record.tenantType }}</template>
          <template v-else-if="column.dataIndex === 'actions'">
            <a-space wrap>
              <a-button v-if="hasPermission('tenant:update')" type="link" :disabled="detailLoading || mutating" @click="openEdit(record as Tenant)">编辑</a-button>
              <template v-if="hasPermission('tenant:status')">
                <a-popconfirm v-for="status in transitions[(record as Tenant).status]" :key="status" :title="`确认${actionLabels[status]}租户「${record.tenantName}」？`" :description="status === 'CLOSED' ? '关闭后无法重新激活。' : undefined" :ok-text="actionLabels[status]" cancel-text="取消" :disabled="mutating" @confirm="mutate(record as Tenant, status)">
                  <a-button type="link" :danger="status === 'CLOSED'" :disabled="mutating">{{ actionLabels[status] }}</a-button>
                </a-popconfirm>
              </template>
              <a-popconfirm v-if="hasPermission('tenant:delete') && ['PENDING', 'CLOSED'].includes(record.status)" :title="`确认删除租户「${record.tenantName}」？`" description="删除后将从租户列表移除。" ok-text="删除" cancel-text="取消" :disabled="mutating" @confirm="mutate(record as Tenant)">
                <a-button type="link" danger :disabled="mutating">删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
    <a-modal v-model:open="modalOpen" :title="editing ? '编辑租户' : '新增租户'" ok-text="保存" cancel-text="取消" :confirm-loading="saving" :closable="!saving" :mask-closable="!saving" :keyboard="!saving" :cancel-button-props="{ disabled: saving }" @ok="save">
      <a-alert v-if="!editing" message="新租户保存后为待激活状态，请在列表中单独激活。" type="info" show-icon class="tenant-notice" />
      <a-form ref="formRef" :model="form" layout="vertical" :disabled="saving">
        <a-form-item label="租户编码" name="tenantCode" :rules="[{ required: true, whitespace: true, message: '请输入租户编码' }, { max: 128 }]"><a-input v-model:value="form.tenantCode" :maxlength="128" /></a-form-item>
        <a-form-item label="租户名称" name="tenantName" :rules="[{ required: true, whitespace: true, message: '请输入租户名称' }, { max: 128 }]"><a-input v-model:value="form.tenantName" :maxlength="128" /></a-form-item>
        <a-form-item label="租户类型" name="tenantType" :rules="[{ required: true, message: '请选择租户类型' }, { max: 32 }]"><a-select v-model:value="form.tenantType" :options="typeOptions" /></a-form-item>
        <a-form-item label="联系人" name="contactName" :rules="[{ max: 64 }]"><a-input v-model:value="form.contactName" :maxlength="64" /></a-form-item>
        <a-form-item label="联系电话" name="contactPhone" :rules="[{ max: 32 }]"><a-input v-model:value="form.contactPhone" :maxlength="32" /></a-form-item>
        <a-form-item label="联系邮箱" name="contactEmail" :rules="[{ type: 'email', message: '请输入有效邮箱地址' }, { max: 128 }]"><a-input v-model:value="form.contactEmail" :maxlength="128" /></a-form-item>
        <a-form-item label="描述" name="description" :rules="[{ max: 500 }]"><a-textarea v-model:value="form.description" :maxlength="500" :rows="3" show-count /></a-form-item>
      </a-form>
    </a-modal>
  </AdminLayout>
</template>

<style scoped>
.tenant-filters { gap: 12px 0; margin-bottom: 20px; }
.tenant-select { min-width: 130px; }
.tenant-notice { margin-bottom: 16px; }
</style>
