<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, withBase } from 'vitepress'
import { useData } from 'vitepress'
import VPNavBarSearch from 'vitepress/dist/client/theme-default/components/VPNavBarSearch.vue'

import ManualThemeSwitch from './ManualThemeSwitch.vue'

const props = defineProps<{
  isHome: boolean
  pageTitle: string
}>()

defineEmits<{
  'toggle-menu': []
}>()

const { theme } = useData()
const route = useRoute()

const navItems = computed(() => theme.value.nav ?? [])

function navIsActive(activeMatch: string | undefined, link: string): boolean {
  if (activeMatch) {
    return new RegExp(activeMatch).test(route.path)
  }
  return route.path === withBase(link) || route.path.startsWith(`${withBase(link)}`)
}
</script>

<template>
  <header class="manual-topbar">
    <div class="manual-topbar__left">
      <button
        v-if="!isHome"
        type="button"
        class="manual-menu-btn"
        aria-label="打开目录"
        @click="$emit('toggle-menu')"
      >
        ☰
      </button>

      <a v-if="isHome" class="manual-brand" :href="withBase('/')">
        <span class="manual-brand__mark">P</span>
        <div class="manual-brand__meta">
          <strong>Peach Cloud</strong>
          <small>USER MANUAL</small>
        </div>
      </a>

      <div v-else class="manual-crumbs">
        <span>Peach Cloud</span>
        <span class="manual-crumbs__sep">/</span>
        <strong>{{ pageTitle }}</strong>
      </div>
    </div>

    <div class="manual-topbar__actions">
      <VPNavBarSearch class="manual-search" />

      <nav class="manual-topnav" aria-label="手册分区">
        <a
          v-for="item in navItems"
          :key="item.link"
          class="manual-topnav__item"
          :class="{ 'is-active': navIsActive(item.activeMatch, item.link) }"
          :href="withBase(item.link)"
        >
          {{ item.text }}
        </a>
      </nav>

      <ManualThemeSwitch />
    </div>
  </header>
</template>
