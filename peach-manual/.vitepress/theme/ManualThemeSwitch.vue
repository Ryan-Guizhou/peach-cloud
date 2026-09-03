<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'

import {
  applyManualTheme,
  manualThemeOptions,
  readStoredManualTheme,
  storeManualTheme,
  type ManualThemeMode,
} from './manual-theme'

const mode = ref<ManualThemeMode>('light')
let mediaQuery: MediaQueryList | null = null

function handleMediaChange(): void {
  if (mode.value === 'auto') {
    applyManualTheme('auto')
  }
}

function selectMode(next: ManualThemeMode): void {
  mode.value = next
  storeManualTheme(next)
  applyManualTheme(next)
}

onMounted(() => {
  mode.value = readStoredManualTheme()
  applyManualTheme(mode.value)
  mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
  mediaQuery.addEventListener('change', handleMediaChange)
})

onUnmounted(() => {
  mediaQuery?.removeEventListener('change', handleMediaChange)
})
</script>

<template>
  <div class="manual-theme-segment" role="group" aria-label="主题切换">
    <button
      v-for="option in manualThemeOptions"
      :key="option.value"
      type="button"
      class="manual-theme-segment__item"
      :class="{ 'is-active': mode === option.value }"
      @click="selectMode(option.value)"
    >
      {{ option.label }}
    </button>
  </div>
</template>
