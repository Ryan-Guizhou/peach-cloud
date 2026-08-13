<script setup lang="ts">
import CrudPage from '../../../components/admin/CrudPage.vue'
</script>

<template>
  <CrudPage
    title="操作日志"
    description="查看 UserOperLog AOP 记录的接口操作、执行耗时和结果。"
    row-key="id"
    permission-prefix="operationLog"
    :endpoint="{ pageList: '/auth/operLog/pageList' }"
    :columns="[
      { title: '日志ID', dataIndex: 'id', width: 180 },
      { title: '模块', dataIndex: 'moduleCode' },
      { title: '操作类型', dataIndex: 'optTypeCode' },
      { title: '操作人', dataIndex: 'creatorName' },
      { title: '请求路径', dataIndex: 'requestUri' },
      { title: '是否成功', dataIndex: 'isSuccess' },
      { title: '耗时(ms)', dataIndex: 'executionTime' },
      { title: '创建时间', dataIndex: 'createTime' },
    ]"
    :fields="[]"
  />
</template>
