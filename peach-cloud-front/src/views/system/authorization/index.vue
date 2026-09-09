<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'

import {
  fetchAuthFunctions,
  fetchAuthResources,
  fetchPage,
  fetchUserRoles,
  saveRoleFunctions,
  saveRoleResources,
  saveUserRoles,
  type DataRecord,
  type RoleResourceItem,
} from '../../../api/admin'
import AdminLayout from '../../../layouts/admin/index.vue'
import { usePermission } from '../../../composables/usePermission'
import { useAuthStore } from '../../../stores/auth'

interface RoleOption extends DataRecord {
  roleCode: string
  roleName: string
}

interface UserOption extends DataRecord {
  userCode: string
  userName: string
}

interface FunctionOption extends DataRecord {
  funcCode: string
  funcName: string
  parentFuncCode?: string
}

interface ResourceOption extends DataRecord {
  funcCode: string
  resourceType: string
  resourceCode: string
  resourceName?: string
}

const authStore = useAuthStore()
const { hasPermission } = usePermission()

const activeTab = ref<'role' | 'user'>('role')
const loading = ref(false)
const saving = ref(false)
const roleOptions = ref<RoleOption[]>([])
const userOptions = ref<UserOption[]>([])
const functionOptions = ref<FunctionOption[]>([])
const resourceOptions = ref<ResourceOption[]>([])
const selectedFuncCodes = ref<string[]>([])
const selectedResourceKeys = ref<string[]>([])
const selectedUserRoleCodes = ref<string[]>([])

const formState = reactive({
  partyCode: '',
  fiscal: Number(authStore.session?.fiscal ?? new Date().getFullYear()),
  userCode: '',
})

const tenantId = computed(() => authStore.session?.tenantId ?? '')
const orgId = computed(() => authStore.session?.orgId ?? '')

const canSaveRoleAuth = computed(() => hasPermission('authorization:update'))
const canSaveUserRoles = computed(() => hasPermission('authorization:update'))

const functionTreeData = computed(() => {
  const nodes = new Map<string, { title: string; key: string; children: Array<{ title: string; key: string }> }>()
  for (const item of functionOptions.value) {
    if (!item.funcCode) {
      continue
    }
    const parentCode = item.parentFuncCode?.trim()
    if (!parentCode) {
      if (!nodes.has(item.funcCode)) {
        nodes.set(item.funcCode, {
          title: item.funcName || item.funcCode,
          key: item.funcCode,
          children: [],
        })
      }
      continue
    }
    if (!nodes.has(parentCode)) {
      nodes.set(parentCode, { title: parentCode, key: parentCode, children: [] })
    }
    nodes.get(parentCode)?.children.push({
      title: item.funcName ? `${item.funcName} (${item.funcCode})` : item.funcCode,
      key: item.funcCode,
    })
  }
  return Array.from(nodes.values())
})

const resourceTableData = computed(() => resourceOptions.value.map(item => ({
  key: `${item.funcCode}::${item.resourceType}::${item.resourceCode}`,
  funcCode: item.funcCode,
  opType: item.resourceType,
  resourceCode: item.resourceCode,
  resourceName: item.resourceName ?? '',
})))

const resourceColumns = [
  { title: '功能编码', dataIndex: 'funcCode', ellipsis: true },
  { title: '类型', dataIndex: 'opType', width: 110 },
  { title: '资源编码', dataIndex: 'resourceCode', ellipsis: true },
  { title: '资源名称', dataIndex: 'resourceName', ellipsis: true },
]

function buildScopeQuery(extra: DataRecord = {}): DataRecord {
  return {
    tenantId: tenantId.value,
    orgId: orgId.value,
    fiscal: formState.fiscal,
    pageNum: 1,
    pageSize: 500,
    ...extra,
  }
}

async function loadBaseOptions() {
  const [roles, users, functions, resources] = await Promise.all([
    fetchPage<RoleOption>('/auth/role/pageList', buildScopeQuery()),
    fetchPage<UserOption>('/auth/user/pageList', buildScopeQuery()),
    fetchPage<FunctionOption>('/auth/function/pageList', buildScopeQuery()),
    fetchPage<ResourceOption>('/auth/resource/pageList', buildScopeQuery()),
  ])
  roleOptions.value = roles.list ?? []
  userOptions.value = users.list ?? []
  functionOptions.value = functions.list ?? []
  resourceOptions.value = resources.list ?? []
}

async function loadRoleAuth() {
  if (!formState.partyCode) {
    message.warning('请选择角色')
    return
  }
  loading.value = true
  try {
    const query: DataRecord = {
      tenantId: tenantId.value,
      orgId: orgId.value,
      partyCode: formState.partyCode,
      fiscal: formState.fiscal,
    }
    const [functions, resourceList] = await Promise.all([
      fetchAuthFunctions(query),
      fetchAuthResources(query),
    ])
    selectedFuncCodes.value = functions
      .map(item => String(item.funcCode ?? ''))
      .filter(item => item.length > 0)
    selectedResourceKeys.value = resourceList
      .map(item => `${String(item.funcCode ?? '')}::${String(item.opType ?? 'BUTTON')}::${String(item.resourceCode ?? '')}`)
      .filter(item => !item.startsWith('::'))
  } catch (error: unknown) {
    message.error(error instanceof Error ? error.message : '加载角色授权失败')
  } finally {
    loading.value = false
  }
}

async function loadUserRoles() {
  if (!formState.userCode) {
    message.warning('请选择用户')
    return
  }
  loading.value = true
  try {
    const records = await fetchUserRoles({
      tenantId: tenantId.value,
      orgId: orgId.value,
      partyCode: formState.userCode,
      partyType: 'USER',
      fiscal: formState.fiscal,
    })
    selectedUserRoleCodes.value = records
      .map(item => String(item.roleCode ?? ''))
      .filter(item => item.length > 0)
  } catch (error: unknown) {
    message.error(error instanceof Error ? error.message : '加载用户角色失败')
  } finally {
    loading.value = false
  }
}

async function saveRoleAuth() {
  if (!canSaveRoleAuth.value) {
    message.warning('缺少 authorization:update 权限')
    return
  }
  if (!formState.partyCode) {
    message.warning('请选择角色')
    return
  }
  saving.value = true
  try {
    const selectedResources = resourceTableData.value.filter(item => selectedResourceKeys.value.includes(item.key))
    await saveRoleFunctions({
      tenantId: tenantId.value,
      orgId: orgId.value,
      partyCode: formState.partyCode,
      fiscal: formState.fiscal,
      funcCodeList: selectedFuncCodes.value,
    })
    await saveRoleResources({
      tenantId: tenantId.value,
      orgId: orgId.value,
      partyCode: formState.partyCode,
      fiscal: formState.fiscal,
      resourceList: selectedResources.map((item): RoleResourceItem => ({
        funcCode: item.funcCode,
        opType: item.opType,
        resourceCode: item.resourceCode,
        resourceName: item.resourceName || undefined,
      })),
    })
    message.success('角色授权保存成功')
  } catch (error: unknown) {
    message.error(error instanceof Error ? error.message : '角色授权保存失败')
  } finally {
    saving.value = false
  }
}

async function saveUserRoleBinding() {
  if (!canSaveUserRoles.value) {
    message.warning('缺少 authorization:update 权限')
    return
  }
  if (!formState.userCode) {
    message.warning('请选择用户')
    return
  }
  saving.value = true
  try {
    await saveUserRoles({
      tenantId: tenantId.value,
      orgId: orgId.value,
      userCode: formState.userCode,
      fiscal: formState.fiscal,
      roleCodeList: selectedUserRoleCodes.value,
    })
    message.success('用户角色绑定保存成功')
  } catch (error: unknown) {
    message.error(error instanceof Error ? error.message : '用户角色绑定保存失败')
  } finally {
    saving.value = false
  }
}

watch(() => formState.partyCode, () => {
  selectedFuncCodes.value = []
  selectedResourceKeys.value = []
})

watch(() => formState.userCode, () => {
  selectedUserRoleCodes.value = []
})

onMounted(async () => {
  loading.value = true
  try {
    await loadBaseOptions()
  } catch (error: unknown) {
    message.error(error instanceof Error ? error.message : '加载基础数据失败')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <AdminLayout>
    <a-card class="admin-page-card">
      <template #title>
        <div class="page-heading">
          <div>
            <h2>权限授权</h2>
            <p>维护角色功能/资源授权与用户角色绑定，变更会写入授权日志并刷新在线用户权限缓存。</p>
          </div>
        </div>
      </template>

      <a-tabs v-model:active-key="activeTab">
        <a-tab-pane key="role" tab="角色授权">
          <a-form layout="vertical" class="admin-inline-form">
            <a-row :gutter="16">
              <a-col :xs="24" :md="8">
                <a-form-item label="角色" required>
                  <a-select
                    v-model:value="formState.partyCode"
                    show-search
                    placeholder="选择角色"
                    :options="roleOptions.map(item => ({ label: `${item.roleName} (${item.roleCode})`, value: item.roleCode }))"
                  />
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
            <a-space>
              <a-button :loading="loading" @click="loadRoleAuth">读取授权</a-button>
              <a-button type="primary" :loading="saving" :disabled="!canSaveRoleAuth" @click="saveRoleAuth">
                保存授权
              </a-button>
            </a-space>
          </a-form>

          <a-row :gutter="[16, 16]" class="admin-section">
            <a-col :xs="24" :xl="9">
              <a-card class="admin-section-card" title="功能授权">
                <a-tree
                  v-if="functionTreeData.length > 0"
                  v-model:checked-keys="selectedFuncCodes"
                  checkable
                  default-expand-all
                  :tree-data="functionTreeData"
                />
                <a-empty v-else description="暂无可授权功能，请先在功能管理中维护。" />
              </a-card>
            </a-col>
            <a-col :xs="24" :xl="15">
              <a-card class="admin-section-card" title="资源授权">
                <a-table
                  class="inner-table admin-data-table"
                  size="small"
                  row-key="key"
                  :pagination="{ pageSize: 12 }"
                  :row-selection="{ selectedRowKeys: selectedResourceKeys, onChange: (keys: string[]) => { selectedResourceKeys = keys } }"
                  :data-source="resourceTableData"
                  :columns="resourceColumns"
                  :scroll="{ x: 'max-content' }"
                >
                  <template #emptyText>
                    <a-empty description="暂无可授权资源，请先在资源管理中维护。" />
                  </template>
                </a-table>
              </a-card>
            </a-col>
          </a-row>
        </a-tab-pane>

        <a-tab-pane key="user" tab="用户角色">
          <a-form layout="vertical" class="admin-inline-form">
            <a-row :gutter="16">
              <a-col :xs="24" :md="8">
                <a-form-item label="用户" required>
                  <a-select
                    v-model:value="formState.userCode"
                    show-search
                    placeholder="选择用户"
                    :options="userOptions.map(item => ({ label: `${item.userName} (${item.userCode})`, value: item.userCode }))"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="8">
                <a-form-item label="年度" required>
                  <a-input-number v-model:value="formState.fiscal" class="full-control" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="8">
                <a-form-item label="绑定角色">
                  <a-select
                    v-model:value="selectedUserRoleCodes"
                    mode="multiple"
                    placeholder="选择要绑定的角色"
                    :options="roleOptions.map(item => ({ label: `${item.roleName} (${item.roleCode})`, value: item.roleCode }))"
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <a-space>
              <a-button :loading="loading" @click="loadUserRoles">读取绑定</a-button>
              <a-button type="primary" :loading="saving" :disabled="!canSaveUserRoles" @click="saveUserRoleBinding">
                保存绑定
              </a-button>
            </a-space>
          </a-form>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </AdminLayout>
</template>
