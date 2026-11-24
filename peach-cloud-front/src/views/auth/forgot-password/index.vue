<script setup lang="ts">
import { RouterLink } from 'vue-router'
import AuthShell from '../../../components/auth/AuthShell.vue'
</script>

<template>
  <AuthShell>
    <div class="segment">
      <RouterLink to="/login" class="segment__item">登录</RouterLink>
      <RouterLink to="/register" class="segment__item">注册</RouterLink>
    </div>
    <header class="form-header">
      <h2>找回密码</h2>
      <p>当前暂未开放自助找回密码，请联系管理员协助恢复账户访问。</p>
    </header>
    <a-alert type="info" show-icon message="自助找回密码暂未开放" description="此页面不会发送重置邮件。" />
    <p class="return-line"><RouterLink to="/login" class="forgot-link">返回登录</RouterLink></p>
  </AuthShell>
</template>
