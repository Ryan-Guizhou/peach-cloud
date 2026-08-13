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
        <span class="hero-copy__eyebrow">Data Intelligence Infrastructure</span>
        <h1>让数据成为组织运行的实时语言。</h1>
        <p>{{ systemDescription || '统一数据、权限与业务上下文，在一个可信的数据中枢内协同运行。' }}</p>
      </div>

      <div class="hero-panel__art">
        <div class="data-orbit" aria-hidden="true">
          <div class="data-orbit__ring data-orbit__ring--outer" />
          <div class="data-orbit__ring data-orbit__ring--inner" />
          <div class="data-orbit__core">
            <span>DATA</span>
            <strong>100%</strong>
            <small>可信上下文</small>
          </div>
          <span class="data-node data-node--one">租户</span>
          <span class="data-node data-node--two">机构</span>
          <span class="data-node data-node--three">权限</span>
          <span class="data-node data-node--four">资产</span>
        </div>
        <div class="data-pulse-card">
          <div><span>实时数据流</span><strong>12.8K/s</strong></div>
          <svg viewBox="0 0 280 72" role="img" aria-label="实时数据流趋势">
            <path d="M2 58 C30 55 36 24 62 34 S98 62 124 39 S163 18 184 35 S220 58 278 12" />
          </svg>
        </div>
      </div>

      <div class="hero-panel__footer">
        <div>
          <strong>{{ systemName || 'PeachCloud DataOS' }}</strong>
          <span>Copyright 2026 PeachCloud Systems. All rights reserved.</span>
        </div>
        <div>
          <span>Data Security Compliance Center</span>
          <span>Terms of Service | Privacy Policy</span>
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
