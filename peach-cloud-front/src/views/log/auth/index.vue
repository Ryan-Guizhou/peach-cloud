<script setup lang="ts">
import CrudPage from '../../../components/admin/CrudPage.vue'
</script>

<template>
  <CrudPage
    title="授权日志"
    description="查看角色功能授权和资源授权变更记录。"
    row-key="logId"
    permission-prefix="authLog"
    :endpoint="{ pageList: '/auth/authLog/pageList' }"
    :columns="[
      { title: '日志ID', dataIndex: 'logId', width: 180 },
      { title: '租户', dataIndex: 'tenantId' },
      { title: '机构', dataIndex: 'orgId' },
      { title: '操作人', dataIndex: 'operatorUserId' },
      { title: '授权描述', dataIndex: 'authDescribe' },
      { title: '操作时间', dataIndex: 'operatTime' },
    ]"
    :fields="[]"
  />
</template>
