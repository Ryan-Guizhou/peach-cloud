<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'

import {
  fetchSaasCatalog,
  fetchRolePermissionRefs,
  fetchMemberBindings,
  grantMemberRole,
  replaceTenantRolePermissions,
  revokeMemberRole,
} from '../../../api/saas-iam'
import type { CatalogKind, SaasCatalogEntry, ScopeType } from '../../../api/saas-iam'
import AdminLayout from '../../../layouts/admin/index.vue'
import { useAuthStore } from '../../../stores/auth'

const authStore = useAuthStore()
const canManage = computed(() => authStore.isAuthenticated
  && authStore.permissions.has('TENANT.ROLE.MANAGE'))

const kinds: CatalogKind[] = ['roles', 'permissions', 'members', 'organizations']
const catalog = reactive<Record<CatalogKind, SaasCatalogEntry[]>>({
  roles: [], permissions: [], members: [], organizations: [],
})
const pages = reactive<Record<CatalogKind, number>>({
  roles: 0, permissions: 0, members: 0, organizations: 0,
})
const hasMore = reactive<Record<CatalogKind, boolean>>({
  roles: false, permissions: false, members: false, organizations: false,
})
const loadingCatalog = ref(false)
const saving = ref(false)
const selectedRoleId = ref('')
const selectedMemberId = ref('')
const selectedScope = ref<ScopeType>('TENANT')
const selectedOrgId = ref('')
const selectedPermissionRefs = ref<string[]>([])
const existingPermissionRefs = ref<string[]>([])
const bindings = ref<SaasCatalogEntry[]>([])
const selectedBindingId = ref('')
const loadingRole = ref(false)
const loadingMember = ref(false)
let roleVersion = 0
let memberVersion = 0

const missingPermissionRefs = computed(() => {
  const visible = new Set(catalog.permissions.map(item => item.id))
  return existingPermissionRefs.value.filter(id => !visible.has(id))
})
const canReplaceRolePermissions = computed(() => canManage.value
  && Boolean(selectedRoleId.value)
  && !loadingRole.value && !loadingCatalog.value
  && !hasMore.permissions && missingPermissionRefs.value.length === 0)

const roleOptions = computed(() => catalog.roles.map(item => ({
  label: item.name ? `${item.name} (${item.code ?? ''})` : item.code || item.id,
  value: item.id,
})))
const memberOptions = computed(() => catalog.members.map(item => ({
  label: item.code || item.id,
  value: item.id,
})))
const orgOptions = computed(() => catalog.organizations.map(item => ({
  label: item.name ? `${item.name} (${item.code ?? ''})` : item.code || item.id,
  value: item.id,
})))
const permissionOptions = computed(() => catalog.permissions.map(item => ({
  label: `${item.name || item.code || item.id} [${item.resourceType || '资源'}]`,
  value: item.id,
})))
const bindingOptions = computed(() => bindings.value.map(item => ({
  label: `${item.name || item.roleRefId || item.id} / ${item.scopeType || 'TENANT'}`,
  value: item.id,
})))

async function loadCatalog(kind: CatalogKind, append = false) {
  if (!canManage.value || (append && !hasMore[kind])) {
    return
  }
  const page = append ? pages[kind] + 1 : 1
  const result = await fetchSaasCatalog(kind, page, 50)
  if (!canManage.value) {
    return
  }
  const oldItems = append ? catalog[kind] : []
  const byId = new Map(oldItems.map(item => [item.id, item]))
  result.items.forEach(item => byId.set(item.id, item))
  catalog[kind] = Array.from(byId.values())
  pages[kind] = page
  hasMore[kind] = result.hasMore
}

async function loadDirectories() {
  if (!canManage.value || loadingCatalog.value) {
    return
  }
  loadingCatalog.value = true
  try {
    await Promise.all(kinds.map(kind => loadCatalog(kind)))
  } catch (error: unknown) {
    kinds.forEach(kind => {
      catalog[kind] = []
      pages[kind] = 0
      hasMore[kind] = false
    })
    message.error(error instanceof Error ? error.message : '管理目录加载失败')
  } finally {
    loadingCatalog.value = false
  }
}

async function loadMore(kind: CatalogKind) {
  loadingCatalog.value = true
  try {
    await loadCatalog(kind, true)
  } catch (error: unknown) {
    message.error(error instanceof Error ? error.message : '后续目录加载失败')
  } finally {
    loadingCatalog.value = false
  }
}

watch(selectedRoleId, async (id) => {
  const version = ++roleVersion
  selectedPermissionRefs.value = []
  existingPermissionRefs.value = []
  if (!id || !canManage.value) {
    return
  }
  loadingRole.value = true
  try {
    const current = await fetchRolePermissionRefs(id)
    if (version !== roleVersion) {
      return
    }
    existingPermissionRefs.value = current
    selectedPermissionRefs.value = [...current]
  } catch (error: unknown) {
    if (version === roleVersion) {
      message.error(error instanceof Error ? error.message : '角色权限加载失败')
    }
  } finally {
    if (version === roleVersion) {
      loadingRole.value = false
    }
  }
})

watch(selectedMemberId, async (id) => {
  const version = ++memberVersion
  bindings.value = []
  selectedBindingId.value = ''
  if (!id || !canManage.value) {
    return
  }
  loadingMember.value = true
  try {
    const current = await fetchMemberBindings(id)
    if (version === memberVersion) {
      bindings.value = current
    }
  } catch (error: unknown) {
    if (version === memberVersion) {
      message.error(error instanceof Error ? error.message : '成员授权记录加载失败')
    }
  } finally {
    if (version === memberVersion) {
      loadingMember.value = false
    }
  }
})

watch(canManage, (allowed) => {
  if (allowed) {
    void loadDirectories()
    return
  }
  selectedRoleId.value = ''
  selectedMemberId.value = ''
  selectedPermissionRefs.value = []
  bindings.value = []
  kinds.forEach(kind => {
    catalog[kind] = []
    pages[kind] = 0
    hasMore[kind] = false
  })
})

onMounted(() => {
  if (canManage.value) {
    void loadDirectories()
  }
})

async function saveRolePermissions() {
  if (!canReplaceRolePermissions.value || saving.value) {
    message.warning('请先完整加载可委派权限目录与当前角色权限')
    return
  }
  saving.value = true
  try {
    await replaceTenantRolePermissions(selectedRoleId.value, selectedPermissionRefs.value)
    existingPermissionRefs.value = await fetchRolePermissionRefs(selectedRoleId.value)
    selectedPermissionRefs.value = [...existingPermissionRefs.value]
    message.success('当前年度角色权限已同步')
  } catch (error: unknown) {
    message.error(error instanceof Error ? error.message : '角色权限同步失败')
  } finally {
    saving.value = false
  }
}

async function grant() {
  if (!canManage.value || !selectedRoleId.value || !selectedMemberId.value
    || (selectedScope.value !== 'TENANT' && !selectedOrgId.value) || saving.value) {
    message.warning('请选择成员、角色及有效机构范围')
    return
  }
  saving.value = true
  try {
    await grantMemberRole({
      targetMemberRefId: selectedMemberId.value,
      roleRefId: selectedRoleId.value,
      scopeType: selectedScope.value,
      scopeRefId: selectedScope.value === 'TENANT' ? undefined : selectedOrgId.value,
    })
    bindings.value = await fetchMemberBindings(selectedMemberId.value)
    message.success('当前年度成员角色授权成功')
  } catch (error: unknown) {
    message.error(error instanceof Error ? error.message : '角色授权失败')
  } finally {
    saving.value = false
  }
}

async function revoke() {
  if (!canManage.value || !selectedBindingId.value || !selectedMemberId.value || saving.value) {
    return
  }
  saving.value = true
  try {
    await revokeMemberRole(selectedBindingId.value)
    bindings.value = await fetchMemberBindings(selectedMemberId.value)
    selectedBindingId.value = ''
    message.success('指定角色绑定已撤销')
  } catch (error: unknown) {
    message.error(error instanceof Error ? error.message : '撤销角色绑定失败')
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
            <h2>SaaS 年度角色授权</h2>
            <p>角色、成员、机构和权限都来自当前租户、应用和自然年度的受信目录。</p>
          </div>
        </div>
      </template>

      <a-alert
        v-if="!canManage"
        type="warning"
        show-icon
        message="当前成员没有完整租户角色管理权限"
        description="页面展示不是服务端授权；需要有效的 TENANT.ROLE.MANAGE 才能读取目录及提交修改。"
      />
      <template v-else>
        <a-descriptions size="small" bordered :column="1" style="margin-bottom: 20px">
          <a-descriptions-item label="租户">
            {{ authStore.session?.tenantName || authStore.session?.tenantId || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="自然年度">{{ authStore.session?.year }}</a-descriptions-item>
          <a-descriptions-item label="当前应用">{{ authStore.session?.appRefId }}</a-descriptions-item>
        </a-descriptions>

        <a-button :loading="loadingCatalog" @click="loadDirectories">刷新受信目录</a-button>

        <a-divider orientation="left">角色权限（完整替换）</a-divider>
        <a-form layout="vertical">
          <a-form-item label="选择角色">
            <a-select
              v-model:value="selectedRoleId"
              :options="roleOptions"
              :loading="loadingCatalog"
              placeholder="请选择当前年度角色"
              show-search
              option-filter-prop="label"
            />
            <a-button
              type="link"
              :disabled="!hasMore.roles || loadingCatalog"
              @click="loadMore('roles')"
            >加载更多角色</a-button>
          </a-form-item>

          <a-form-item label="当前角色的可委派权限">
            <a-select
              v-model:value="selectedPermissionRefs"
              mode="multiple"
              :options="permissionOptions"
              :disabled="!canReplaceRolePermissions || saving"
              placeholder="先加载当前角色权限及全部可委派目录"
              show-search
              option-filter-prop="label"
            />
            <a-button
              type="link"
              :disabled="!hasMore.permissions || loadingCatalog"
              @click="loadMore('permissions')"
            >加载更多可委派权限</a-button>
          </a-form-item>
          <a-alert
            v-if="hasMore.permissions || missingPermissionRefs.length > 0"
            type="warning"
            show-icon
            message="当前权限目录未完整，禁止全量替换"
            description="必须加载全部可委派权限。若原角色包含当前管理员不可委派的权限，请由更高权限管理员处理，不能静默删除原有授权。"
            style="margin-bottom: 16px"
          />
          <a-button
            type="primary"
            :loading="saving || loadingRole"
            :disabled="!canReplaceRolePermissions || saving"
            @click="saveRolePermissions"
          >保存角色全部权限</a-button>
        </a-form>

        <a-divider orientation="left">成员角色范围授权</a-divider>
        <a-form layout="vertical">
          <a-form-item label="目标成员">
            <a-select
              v-model:value="selectedMemberId"
              :options="memberOptions"
              :loading="loadingCatalog"
              placeholder="选择受信成员目录中的物理ID"
              show-search
              option-filter-prop="label"
            />
            <a-button
              type="link"
              :disabled="!hasMore.members || loadingCatalog"
              @click="loadMore('members')"
            >加载更多成员</a-button>
          </a-form-item>
          <a-form-item label="授予的角色">
            <a-select
              v-model:value="selectedRoleId"
              :options="roleOptions"
              placeholder="使用上方选中的年度角色"
              show-search
              option-filter-prop="label"
            />
          </a-form-item>
          <a-form-item label="数据范围">
            <a-select
              v-model:value="selectedScope"
              :options="[
                { label: '整个租户', value: 'TENANT' },
                { label: '单机构', value: 'ORG' },
                { label: '机构及下级树', value: 'ORG_TREE' },
              ]"
            />
          </a-form-item>
          <a-form-item v-if="selectedScope !== 'TENANT'" label="范围机构">
            <a-select
              v-model:value="selectedOrgId"
              :options="orgOptions"
              placeholder="从受信机构目录选择"
              show-search
              option-filter-prop="label"
            />
            <a-button
              type="link"
              :disabled="!hasMore.organizations || loadingCatalog"
              @click="loadMore('organizations')"
            >加载更多机构</a-button>
          </a-form-item>
          <a-button
            type="primary"
            :loading="saving"
            :disabled="saving || !selectedRoleId || !selectedMemberId"
            @click="grant"
          >授予独立角色绑定</a-button>
        </a-form>

        <a-divider orientation="left">撤销当前成员的指定绑定</a-divider>
        <a-select
          v-model:value="selectedBindingId"
          :options="bindingOptions"
          :loading="loadingMember"
          placeholder="选择一条现有绑定，避免按业务角色码误撤销"
          style="width: 100%; margin-bottom: 16px"
        />
        <a-button
          danger
          :loading="saving"
          :disabled="!selectedBindingId || saving"
          @click="revoke"
        >撤销选中的角色绑定</a-button>
        <a-alert
          style="margin-top: 20px"
          type="info"
          show-icon
          message="行级与字段策略编辑需要字段定义目录"
          description="当前已实现可验证的年度角色权限与成员范围授权；复杂行级、字段级策略仍需后端受信字段目录和完整历史策略读取，不能直接输入物理字段ID绕过管理约束。"
        />
      </template>
    </a-card>
  </AdminLayout>
</template>
