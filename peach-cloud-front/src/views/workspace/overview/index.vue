<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import AdminLayout from '../../../layouts/admin/index.vue'
import { buildMenuGroups } from '../../../router/dynamic'
import { useAuthStore } from '../../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const session = computed(() => authStore.session)
const groups = computed(() => buildMenuGroups(authStore.loginInfo)
  .map(group => ({ ...group, items: group.items.filter(item => item.path !== '/workspace') }))
  .filter(group => group.items.length > 0))
const roles = computed(() => authStore.loginInfo?.roleList ?? [])
</script>

<template>
  <AdminLayout responsive>
    <a-card class="admin-page-card workspace-welcome">
      <h1>你好，{{ session?.userName || '欢迎回来' }}</h1>
      <p>从工作台进入你的业务空间。</p>
      <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small">
        <a-descriptions-item label="当前租户">{{ session?.tenantName || '—' }}</a-descriptions-item>
        <a-descriptions-item label="当前机构">{{ session?.orgName || '—' }}</a-descriptions-item>
        <a-descriptions-item label="年度">{{ session?.fiscal || '—' }}</a-descriptions-item>
        <a-descriptions-item label="当前角色" :span="3">
          <a-space wrap><a-tag v-for="role in roles" :key="role.roleCode">{{ role.roleName || role.roleCode }}</a-tag><span v-if="!roles.length">暂无角色</span></a-space>
        </a-descriptions-item>
      </a-descriptions>
    </a-card>
    <section class="workspace-shortcuts" aria-label="可用功能">
      <a-card v-for="group in groups" :key="group.title" :title="group.title" class="admin-section-card">
        <div class="workspace-links">
          <button v-for="item in group.items" :key="item.path" type="button" @click="router.push(item.path)">{{ item.meta.title }}<span aria-hidden="true">&rarr;</span></button>
        </div>
      </a-card>
      <a-empty v-if="!groups.length" description="当前角色暂无可用功能，请联系管理员授权。" />
    </section>
  </AdminLayout>
</template>

<style scoped>
.workspace-welcome h1 { margin: 0 0 8px; font-size: 24px; color: var(--color-text-primary); }
.workspace-welcome p { margin: 0 0 24px; color: var(--color-text-secondary); }
.workspace-shortcuts { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 300px), 1fr)); align-items: start; gap: 20px; margin-top: 24px; }
.workspace-links { display: grid; gap: 8px; }
.workspace-links button { display: flex; justify-content: space-between; gap: 16px; padding: 12px 14px; border: 1px solid var(--color-border); border-radius: 8px; color: var(--color-text-primary); background: var(--color-bg-surface); text-align: left; }
.workspace-links button:hover { color: var(--color-accent); border-color: var(--color-border-strong); background: var(--color-bg-soft); }
.workspace-links button span { color: var(--color-text-muted); }
</style>
