<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="not-found-page">
    <a-result status="404" title="页面不存在" sub-title="当前路由未在前端本地视图白名单中找到。">
      <template #extra>
        <a-button type="primary" @click="router.replace('/workspace')">返回工作台</a-button>
      </template>
    </a-result>
  </div>
</template>
