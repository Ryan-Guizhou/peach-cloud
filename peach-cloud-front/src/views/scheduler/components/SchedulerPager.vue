<script setup lang="ts">
defineProps<{ page: number; count: number; pageSize: number; loading: boolean }>()
defineEmits<{ change: [page: number] }>()
</script>

<template>
  <div class="scheduler-pager">
    <span>第 {{ page }} 页 · 本页 {{ count }} 条</span>
    <a-space>
      <a-button :disabled="loading || page <= 1" @click="$emit('change', page - 1)">上一页</a-button>
      <a-button :disabled="loading || count < pageSize" @click="$emit('change', page + 1)">下一页</a-button>
    </a-space>
  </div>
</template>

<style scoped>
.scheduler-pager { display: flex; justify-content: space-between; gap: var(--space-4); flex-wrap: wrap; align-items: center; margin-top: var(--space-4); color: var(--color-text-secondary); }
</style>
