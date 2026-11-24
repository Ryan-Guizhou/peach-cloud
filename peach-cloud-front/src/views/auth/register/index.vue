<script setup lang="ts">
import { RouterLink } from 'vue-router'
import AuthShell from '../../../components/auth/AuthShell.vue'
</script>

<template>
  <AuthShell>
    <div class="segment">
      <RouterLink to="/login" class="segment__item">登录</RouterLink>
      <RouterLink to="/register" class="segment__item is-active" aria-current="page">注册</RouterLink>
    </div>
    <header class="form-header">
      <h2>创建账户</h2>
      <p>当前暂未开放自助注册，请联系管理员创建账户并分配租户、机构和角色。</p>
    </header>
    <a-alert type="info" show-icon message="自助注册暂未开放" description="如果已有账户，可以返回登录页面继续。" />
    <p class="return-line"><RouterLink to="/login" class="forgot-link">返回登录</RouterLink></p>
  </AuthShell>
</template>
