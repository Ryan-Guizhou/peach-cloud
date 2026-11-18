<script setup lang="ts">
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'

import { createTenantRole } from '../../../api/saas-iam'
import AdminLayout from '../../../layouts/admin/index.vue'
import { useAuthStore } from '../../../stores/auth'

const authStore = useAuthStore()
const roleCode = ref('')
const roleName = ref('')
const isSaving = ref(false)
const createdRefId = ref('')

const canCreate = computed(() => authStore.isAuthenticated
  && authStore.permissions.has('TENANT.ROLE.MANAGE'))
const activeContext = computed(() => authStore.session)
const normalizedRoleCode = computed(() => roleCode.value.trim().toUpperCase())
const validForm = computed(() => /^ROLE_[A-Z0-9_]{1,15}$/.test(normalizedRoleCode.value)
  && roleName.value.trim().length > 0 && roleName.value.trim().length <= 64)

async function handleCreate() {
  if (!canCreate.value) {
    message.warning('当前年度没有创建租户角色的权限')
    return
  }
  if (!validForm.value || isSaving.value) {
    message.warning('请填写有效的角色代码和名称')
    return
  }
  isSaving.value = true
  createdRefId.value = ''
  try {
    createdRefId.value = await createTenantRole({
      roleCode: normalizedRoleCode.value,
      roleName: roleName.value.trim(),
    })
    message.success('角色已创建')
    roleCode.value = ''
    roleName.value = ''
  } catch (error: unknown) {
    message.error(error instanceof Error ? error.message : '创建角色失败')
  } finally {
    isSaving.value = false
  }
}
</script>

<template>
  <AdminLayout>
    <a-card class="admin-page-card">
      <template #title>
        <div class="page-heading">
          <div>
            <h2>租户年度角色</h2>
            <p>角色创建只使用当前已验证的租户、应用和自然年度，身份不由表单指定。</p>
          </div>
        </div>
      </template>

      <a-descriptions title="当前授权上下文" :column="1" bordered size="small">
        <a-descriptions-item label="租户">
          {{ activeContext?.tenantName || activeContext?.tenantId || '未选择' }}
        </a-descriptions-item>
        <a-descriptions-item label="应用">{{ activeContext?.appRefId || '未选择' }}</a-descriptions-item>
        <a-descriptions-item label="自然年度">{{ activeContext?.year ?? '-' }}</a-descriptions-item>
      </a-descriptions>

      <a-alert
        v-if="!canCreate"
        type="warning"
        show-icon
        style="margin-top: 20px"
        message="当前身份未获得租户角色管理权限"
        description="请使用拥有 TENANT.ROLE.MANAGE 权限的有效年度成员身份；页面显示不代表后端授权。"
      />

      <a-form layout="vertical" style="margin-top: 24px">
        <a-form-item label="角色代码" required extra="格式：ROLE_ 前缀，后接 1～15 位大写英文字母、数字或下划线">
          <a-input
            v-model:value="roleCode"
            :maxlength="20"
            placeholder="例如 ROLE_DATA_OPERATOR"
            :disabled="isSaving || !canCreate"
            autocomplete="off"
          />
        </a-form-item>
        <a-form-item label="角色名称" required>
          <a-input v-model:value="roleName" :maxlength="64" :disabled="isSaving || !canCreate" />
        </a-form-item>
        <a-button
          type="primary"
          :loading="isSaving"
          :disabled="!canCreate || !validForm"
          @click="handleCreate"
        >
          创建当前年度角色
        </a-button>
      </a-form>

      <a-result
        v-if="createdRefId"
        status="success"
        title="角色创建成功"
        sub-title="物理 ID 已由服务端生成，无需输入或自行修改"
      >
        <template #extra>
          <a-typography-text copyable>{{ createdRefId }}</a-typography-text>
        </template>
      </a-result>

      <a-alert
        type="info"
        show-icon
        style="margin-top: 20px"
        message="角色目录和授权列表待接入"
        description="当前已支持受控创建；查看角色、选择成员和权限、设置行级/字段权限必须使用后端分页目录和物理主键选择器。旧版按 tenantId/orgId/fiscal 拼接的管理操作已停止使用。"
      />
    </a-card>
  </AdminLayout>
</template>
