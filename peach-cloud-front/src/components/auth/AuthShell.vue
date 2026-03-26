<script setup lang="ts">
import AuthIllustration from './AuthIllustration.vue'
import PeachCloudLogo from './PeachCloudLogo.vue'

defineProps<{
  isTyping: boolean
  showPassword: boolean
  passwordLength: number
}>()
</script>

<template>
  <div class="auth-shell">
    <aside class="hero-panel">
      <div class="hero-panel__brand">
        <PeachCloudLogo />
      </div>

      <div class="hero-copy">
        <span class="hero-copy__eyebrow">Data Management Platform</span>
        <h1>Unified data, permissions and workflows.</h1>
        <p>A clear and coordinated visual gateway for team data operations.</p>
      </div>

      <div class="hero-panel__art">
        <AuthIllustration
          :is-typing="isTyping"
          :show-password="showPassword"
          :password-length="passwordLength"
        />
      </div>

      <div class="hero-panel__footer">
        <div>
          <strong>PeachCloud DataOS</strong>
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
