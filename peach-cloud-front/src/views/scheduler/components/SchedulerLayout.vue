<script setup lang="ts">
import AdminLayout from '../../../layouts/admin/index.vue'
</script>

<template>
  <AdminLayout class="scheduler-layout"><slot /></AdminLayout>
</template>

<style scoped>
@media (max-width: 768px) {
  .scheduler-layout :deep(.console-sidebar) { position: static; width: 100%; }
  .scheduler-layout :deep(.console-sidebar__head),
  .scheduler-layout :deep(.console-sidebar__foot) { display: none; }
  .scheduler-layout :deep(.console-nav) { display: flex; overflow-x: auto; padding: var(--space-2); }
  .scheduler-layout :deep(.console-nav__section) { flex: 0 0 auto; }
  .scheduler-layout :deep(.console-topbar) { position: static; height: auto; min-height: 68px; padding: var(--space-3); flex-wrap: wrap; gap: var(--space-2); }
  .scheduler-layout :deep(.console-crumbs) { min-width: 0; }
  .scheduler-layout :deep(.console-topbar__actions) { flex-wrap: wrap; margin-left: 0; }
  .scheduler-layout :deep(.console-main) { margin-left: 0; padding-top: 0; }
  .scheduler-layout :deep(.console-page) { padding: var(--space-3); }
  .scheduler-layout :deep(.ant-card-head-wrapper) { flex-wrap: wrap; gap: var(--space-2); }
  .scheduler-layout :deep(.ant-card-head-title) { white-space: normal; }
}
</style>
