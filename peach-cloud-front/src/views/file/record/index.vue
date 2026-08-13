<script setup lang="ts">
import CrudPage from '../../../components/admin/CrudPage.vue'
</script>

<template>
  <CrudPage
    title="文件记录"
    description="查询业务文件记录，查看文件状态、业务归属和存储对象关联。"
    row-key="fileId"
    permission-prefix="fileRecord"
    :endpoint="{ pageList: '/file/internal/pageList' }"
    :columns="[
      { title: '文件ID', dataIndex: 'fileId', width: 180 },
      { title: '对象ID', dataIndex: 'objectId', width: 180 },
      { title: '文件名', dataIndex: 'fileName' },
      { title: '展示名称', dataIndex: 'displayName' },
      { title: '业务类型', dataIndex: 'bizType' },
      { title: '业务ID', dataIndex: 'bizId' },
      { title: '文件状态', dataIndex: 'fileStatus' },
      { title: '创建时间', dataIndex: 'createdTime' },
    ]"
    :fields="[]"
  />
</template>
