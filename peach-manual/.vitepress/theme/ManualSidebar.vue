<script setup lang="ts">
import { withBase } from 'vitepress'
import { useSidebar } from 'vitepress/dist/client/theme-default/composables/sidebar'
import ManualNavItems from './ManualNavItems.vue'

defineProps<{
  open: boolean
}>()

const { sidebarGroups } = useSidebar()
</script>

<template>
  <aside class="manual-sidebar" :class="{ 'is-open': open }">
    <div class="manual-sidebar__head">
      <a class="manual-brand" :href="withBase('/')">
        <span class="manual-brand__mark">P</span>
        <div class="manual-brand__meta">
          <strong>Peach Cloud</strong>
          <small>USER MANUAL</small>
        </div>
      </a>
    </div>

    <nav class="manual-sidebar__nav" aria-label="手册目录">
      <section
        v-for="(group, index) in sidebarGroups"
        :key="`${group.text ?? 'group'}-${index}`"
        class="manual-nav__section"
      >
        <div v-if="group.text" class="manual-nav__title">{{ group.text }}</div>
        <ManualNavItems :items="group.items" />
      </section>
    </nav>

    <div class="manual-sidebar__foot">
      <div class="manual-status">
        <div class="manual-status__line">
          <span class="manual-status__dot" />
          手册与控制台同步
        </div>
        <div class="manual-status__sub">默认浅色 · 控制台布局</div>
      </div>
    </div>
  </aside>
</template>
