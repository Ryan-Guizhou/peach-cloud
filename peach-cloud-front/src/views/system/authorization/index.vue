<script setup lang="ts">
import AdminLayout from '../../../layouts/admin/index.vue'
import { useAuthStore } from '../../../stores/auth'

const authStore = useAuthStore()
</script>

<template>
  <AdminLayout>
    <a-card class="admin-page-card">
      <template #title>
        <div class="page-heading">
          <div>
            <h2>SaaS 角色授权</h2>
            <p>按当前成员的物理租户、应用与自然年度隔离权限；服务端负责全部身份和数据范围验证。</p>
          </div>
        </div>
      </template>
      <a-descriptions title="当前有效身份" :column="1" bordered size="small">
        <a-descriptions-item label="租户">
          {{ authStore.session?.tenantName || authStore.session?.tenantId || '未选择' }}
        </a-descriptions-item>
        <a-descriptions-item label="年度">{{ authStore.session?.year ?? '-' }}</a-descriptions-item>
        <a-descriptions-item label="成员物理 ID">
          {{ authStore.session?.memberRefId || '缺少有效身份' }}
        </a-descriptions-item>
      </a-descriptions>
      <a-result
        status="info"
        title="新授权目录接入中"
        sub-title="旧版授权界面按用户业务码、机构号与 FISCAL 组合生成授权，不能用于新的多租户年度权限体系。"
      >
        <template #extra>
          <p>后端已具备受保护的角色权限替换、独立成员范围授权与撤销、数据行和字段策略写入接口；新页面将使用来自受信后端目录的物理主键选择器，不允许浏览器手动构造外键或修改租户年度身份。</p>
        </template>
      </a-result>
    </a-card>
  </AdminLayout>
</template>
