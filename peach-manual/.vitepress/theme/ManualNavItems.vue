<script setup lang="ts">
import { useRoute, withBase } from 'vitepress'

interface SidebarItem {
  text?: string
  link?: string
  items?: SidebarItem[]
}

defineProps<{
  items: SidebarItem[]
}>()

const route = useRoute()

function linkIsActive(link: string): boolean {
  const target = withBase(link)
  const current = route.path
  if (current === target) {
    return true
  }
  const normalizedTarget = target.endsWith('/') ? target : `${target}/`
  return current.startsWith(normalizedTarget)
}
</script>

<template>
  <template v-for="(item, index) in items" :key="`${item.text ?? 'item'}-${index}`">
    <a
      v-if="item.link"
      class="manual-nav__item"
      :class="{ 'is-active': linkIsActive(item.link) }"
      :href="withBase(item.link)"
    >
      {{ item.text }}
    </a>
    <ManualNavItems v-else-if="item.items" :items="item.items" />
  </template>
</template>
