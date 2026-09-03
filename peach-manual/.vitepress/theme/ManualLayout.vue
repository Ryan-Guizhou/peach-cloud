<script setup lang="ts">
import { computed } from 'vue'
import { Content, useData } from 'vitepress'
import { useCloseSidebarOnEscape, useSidebar } from 'vitepress/dist/client/theme-default/composables/sidebar'
import VPDocAsideOutline from 'vitepress/dist/client/theme-default/components/VPDocAsideOutline.vue'
import VPDocFooter from 'vitepress/dist/client/theme-default/components/VPDocFooter.vue'
import NotFound from 'vitepress/dist/client/theme-default/NotFound.vue'

import ManualSidebar from './ManualSidebar.vue'
import ManualTopbar from './ManualTopbar.vue'

const { frontmatter, page } = useData()
const { hasSidebar, hasAside, isOpen, close, toggle } = useSidebar()

useCloseSidebarOnEscape(isOpen, close)

const isHome = computed(() => frontmatter.value.layout === 'home')
const isNotFound = computed(() => page.value.isNotFound)
const pageTitle = computed(() => {
  const title = frontmatter.value.title
  if (typeof title === 'string' && title.length > 0) {
    return title
  }
  return page.value.title || '用户手册'
})
const showOutline = computed(() => hasAside.value && !isHome.value && !isNotFound.value)
const showDocFooter = computed(() => !isHome.value && !isNotFound.value)
const contentClass = computed(() => ({
  'manual-doc': !isHome.value,
  'manual-home': isHome.value,
  'vp-doc': !isHome.value,
}))
</script>

<template>
  <div class="manual-shell" :class="{ 'manual-shell--home': isHome }">
    <ManualSidebar v-if="hasSidebar" :open="isOpen" />
    <div
      v-if="hasSidebar && isOpen"
      class="manual-backdrop"
      @click="close"
    />

    <ManualTopbar
      :is-home="isHome"
      :page-title="pageTitle"
      @toggle-menu="toggle"
    />

    <main class="manual-main">
      <div class="manual-main__inner">
        <div class="manual-content">
          <NotFound v-if="isNotFound" />
          <Content v-else :class="contentClass" />
          <div v-if="showDocFooter" class="manual-doc-footer">
            <VPDocFooter />
          </div>
        </div>

        <aside v-if="showOutline" class="manual-aside" aria-label="本页目录">
          <VPDocAsideOutline />
        </aside>
      </div>
    </main>
  </div>
</template>
