<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'

import {
  fetchAuthFunctions,
  fetchAuthResources,
  saveRoleFunctions,
  saveRoleResources,
  type DataRecord,
  type RoleResourceItem,
} from '../../../api/admin'
import AdminLayout from '../../../layouts/admin/index.vue'
import { useAuthStore } from '../../../stores/auth'

const authStore = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const functionCodes = ref<string[]>([])
const resources = ref<RoleResourceItem[]>([])
const formState = reactive({
  partyCode: '',
  fiscal: Number(authStore.session?.fiscal ?? new Date().getFullYear()),
  funcCodeInput: '',
  resourceFuncCode: '',
  resourceOpType: 'BUTTON',
  resourceCode: '',
  resourceName: '',
})

const tenantId = computed(() => authStore.session?.tenantId ?? '')
const orgId = computed(() => authStore.session?.orgId ?? '')
const resourceColumns = [
  { title: '功能编码', dataIndex: 'funcCode', ellipsis: true },
  { title: '类型', dataIndex: 'opType', width: 110 },
  { title: '资源编码', dataIndex: 'resourceCode', ellipsis: true },
  { title: '资源名称', dataIndex: 'resourceName', ellipsis: true },
  { title: '操作', dataIndex: 'actions', width: 90, fixed: 'right' as const },
]

async function loadAuth() {
  if (!formState.partyCode.trim()) {
    message.warning('请输入角色编码')
    return
  }
  loading.value = true
  try {
    const query: DataRecord = {
      tenantId: tenantId.value,
      orgId: orgId.value,
      partyCode: formState.partyCode.trim(),
      fiscal: formState.fiscal,
    }
    const [functions, resourceList] = await Promise.all([
      fetchAuthFunctions(query),
      fetchAuthResources(query),
    ])
    functionCodes.value = functions
      .map(item => String(item.funcCode ?? ''))
      .filter(item => item.length > 0)
    resources.value = resourceList
      .map((item): RoleResourceItem => ({
        funcCode: String(item.funcCode ?? ''),
        opType: String(item.opType ?? 'BUTTON'),
        resourceCode: String(item.resourceCode ?? ''),
        resourceName: typeof item.resourceName === 'string' ? item.resourceName : undefined,
      }))
      .filter(item => item.funcCode.length > 0 && item.resourceCode.length > 0)
  } catch (error: unknown) {
    message.error(error instanceof Error ? error.message : '加载授权失败')
  } finally {
    loading.value = false
  }
}

function addFunctionCode() {
  const code = formState.funcCodeInput.trim()
  if (!code) {
    message.warning('请输入功能编码')
    return
  }
  if (functionCodes.value.includes(code)) {
    message.warning('该功能编码已存在')
    return
  }
  functionCodes.value = [...functionCodes.value, code]
  formState.funcCodeInput = ''
}

function removeFunctionCode(code: string) {
  functionCodes.value = functionCodes.value.filter(item => item !== code)
}

function addResource() {
  const resource: RoleResourceItem = {
    funcCode: formState.resourceFuncCode.trim(),
    opType: formState.resourceOpType,
    resourceCode: formState.resourceCode.trim(),
    resourceName: formState.resourceName.trim() || undefined,
  }
  if (!resource.funcCode || !resource.resourceCode) {
    message.warning('资源授权需要功能编码和资源编码')
    return
  }
  const duplicated = resources.value.some(item => (
    item.funcCode === resource.funcCode
    && item.opType === resource.opType
    && item.resourceCode === resource.resourceCode
  ))
  if (duplicated) {
    message.warning('该资源授权已存在')
    return
  }
  resources.value = [...resources.value, resource]
  formState.resourceFuncCode = ''
  formState.resourceCode = ''
  formState.resourceName = ''
}

function removeResource(index: number) {
  resources.value = resources.value.filter((_, currentIndex) => currentIndex !== index)
}

async function saveAuth() {
  if (!formState.partyCode.trim()) {
    message.warning('请输入角色编码')
    return
  }
  saving.value = true
  try {
    await saveRoleFunctions({
      tenantId: tenantId.value,
      orgId: orgId.value,
      partyCode: formState.partyCode.trim(),
      fiscal: formState.fiscal,
      funcCodeList: functionCodes.value,
    })
    await saveRoleResources({
      tenantId: tenantId.value,
      orgId: orgId.value,
      partyCode: formState.partyCode.trim(),
      fiscal: formState.fiscal,
      resourceList: resources.value,
    })
    message.success('授权保存成功')
  } catch (error: unknown) {
    message.error(error instanceof Error ? error.message : '授权保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <AdminLayout>
    <a-card class="admin-page-card">
      <template #title>
        <div class="page-heading">
          <div>
            <h2>角色授权</h2>
            <p>按当前租户、机构和年度维护角色的菜单/路由功能授权，以及按钮/API 资源授权。</p>
          </div>
          <a-space>
            <a-button :loading="loading" @click="loadAuth">读取授权</a-button>
            <a-button type="primary" :loading="saving" @click="saveAuth">保存授权</a-button>
          </a-space>
        </div>
      </template>

      <a-form layout="vertical" class="admin-inline-form">
        <a-row :gutter="16">
          <a-col :xs="24" :md="8">
            <a-form-item label="角色编码" required>
              <a-input v-model:value="formState.partyCode" placeholder="例如 ROLE_ADMIN" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="8">
            <a-form-item label="年度" required>
              <a-input-number v-model:value="formState.fiscal" class="full-control" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="8">
            <a-form-item label="当前上下文">
              <a-input :value="`${tenantId || '-'} / ${orgId || '-'}`" disabled />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <a-row :gutter="[16, 16]" class="admin-section">
      <a-col :xs="24" :xl="9">
        <a-card class="admin-section-card" title="功能授权">
          <a-input-group compact>
            <a-input
              v-model:value="formState.funcCodeInput"
              class="auth-code-input"
              placeholder="输入菜单/路由 funcCode"
              @press-enter="addFunctionCode"
            />
            <a-button type="primary" @click="addFunctionCode">添加</a-button>
          </a-input-group>
          <div v-if="functionCodes.length > 0" class="tag-list">
            <a-tag v-for="code in functionCodes" :key="code" closable @close.prevent="removeFunctionCode(code)">
              {{ code }}
            </a-tag>
          </div>
          <div v-else class="admin-empty-guide">
            <a-empty description="暂无功能授权。可先输入角色编码读取授权，或手动添加菜单/路由功能编码。" />
          </div>
        </a-card>
      </a-col>

      <a-col :xs="24" :xl="15">
        <a-card class="admin-section-card" title="资源授权">
          <div class="auth-resource-form">
            <a-input v-model:value="formState.resourceFuncCode" placeholder="功能编码" />
            <a-select v-model:value="formState.resourceOpType" class="full-control">
              <a-select-option value="BUTTON">BUTTON</a-select-option>
              <a-select-option value="API">API</a-select-option>
            </a-select>
            <a-input v-model:value="formState.resourceCode" placeholder="资源编码，例如 user:add" />
            <a-input v-model:value="formState.resourceName" placeholder="资源名称" @press-enter="addResource" />
            <a-button type="primary" @click="addResource">添加</a-button>
          </div>
          <a-table
            class="inner-table admin-data-table"
            size="small"
            :pagination="false"
            :data-source="resources"
            :columns="resourceColumns"
            :scroll="{ x: 'max-content' }"
          >
            <template #emptyText>
              <a-empty description="暂无按钮/API 资源授权。按钮用于前端展示控制，API 用于后端接口访问控制。" />
            </template>
            <template #bodyCell="{ column, index }">
              <a-button v-if="column.dataIndex === 'actions'" type="link" danger @click="removeResource(Number(index))">
                移除
              </a-button>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>
  </AdminLayout>
</template>
