<script setup lang="ts">
import PeachCloudLogo from './PeachCloudLogo.vue'

defineProps<{
  systemName?: string
  systemDescription?: string
  isTyping?: boolean
  showPassword?: boolean
  passwordLength?: number
}>()
</script>

<template>
  <div class="auth-shell">
    <aside class="hero-panel">
      <div class="hero-panel__brand">
        <PeachCloudLogo />
      </div>

      <div class="hero-copy">
        <span class="hero-copy__eyebrow">PEACH CLOUD · CONTROL PLANE</span>
        <h1>一个工作空间，连接团队与业务。</h1>
        <p>{{ systemDescription || '统一数据、权限与业务上下文，在一个可信的数据中枢内协同运行。' }}</p>
      </div>

      <ul class="hero-features" aria-label="平台能力">
        <li><strong>统一身份</strong><span>管理账户、租户和机构，协同处理日常工作。</span></li>
        <li><strong>权限管理</strong><span>通过角色与功能授权，访问属于你的业务空间。</span></li>
        <li><strong>运行与审计</strong><span>查看任务执行与操作记录，让工作有迹可循。</span></li>
      </ul>

      <div class="hero-panel__footer">
        <div>
          <strong>{{ systemName || 'Peach Cloud' }}</strong>
          <span>Copyright 2026 PeachCloud Systems. All rights reserved.</span>
        </div>
        <div>
          <span>账户与业务管理平台</span>
          <span>统一身份 · 协同管理</span>
        </div>
      </div>

      <div class="hero-grid" />
      <div class="hero-glow hero-glow--top" />
      <div class="hero-glow hero-glow--bottom" />
    </aside>

    <main class="form-panel">
      <div class="form-wrap form-wrap--wide">
        <div class="mobile-brand">
          <PeachCloudLogo :compact="true" />
        </div>
        <slot />
      </div>
    </main>
  </div>
</template>
