<script setup lang="ts">
defineProps<{ open: boolean; title: string; loading: boolean; fields: Array<{ label: string; value: unknown }> }>()
defineEmits<{ close: [] }>()
</script>

<template>
  <a-drawer :open="open" :title="title" width="min(640px, 100vw)" @close="$emit('close')">
    <a-spin :spinning="loading">
      <a-descriptions bordered :column="1" size="small">
        <a-descriptions-item v-for="field in fields" :key="field.label" :label="field.label"><span class="detail-value">{{ field.value ?? '—' }}</span></a-descriptions-item>
      </a-descriptions>
    </a-spin>
  </a-drawer>
</template>

<style scoped>
.detail-value { white-space: pre-wrap; overflow-wrap: anywhere; }
</style>
