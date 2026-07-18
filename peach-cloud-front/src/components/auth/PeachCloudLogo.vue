<script setup lang="ts">
defineProps<{ compact?: boolean }>()
</script>

<template>
  <div :class="`brand${compact ? ' brand--compact' : ''}`">
    <svg
      class="brand__icon"
      viewBox="0 0 48 48"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <rect width="48" height="48" rx="15" fill="#101828" />
      <path d="M14 15.5H34" stroke="#6DD3FF" stroke-width="2.8" stroke-linecap="round" opacity="0.9" />
      <path d="M14 24H34" stroke="#FFFFFF" stroke-width="2.8" stroke-linecap="round" opacity="0.75" />
      <path d="M14 32.5H34" stroke="#FFB14A" stroke-width="2.8" stroke-linecap="round" opacity="0.9" />
      <circle cx="14" cy="15.5" r="4.2" fill="#FF8A4C" />
      <circle cx="24" cy="24" r="4.2" fill="#6DD3FF" />
      <circle cx="34" cy="32.5" r="4.2" fill="#FFD84D" />
    </svg>
    <div class="brand__text">
      <strong>PeachCloud</strong>
      <span>Data Management Platform</span>
    </div>
  </div>
</template>
