<script setup lang="ts">
import CrudPage from '../../../components/admin/CrudPage.vue'
</script>

<template>
  <CrudPage
    title="授权日志"
    description="查看角色功能授权、角色资源授权与用户角色绑定变更记录。"
    row-key="logId"
    permission-prefix="authLog"
    :endpoint="{ pageList: '/auth/authLog/pageList' }"
    :columns="[
      { title: '操作时间', dataIndex: 'operatTime', width: 170 },
      { title: '操作人账号', dataIndex: 'operatorCode', width: 140 },
      { title: '操作人名称', dataIndex: 'operatorName', width: 140 },
      { title: '被授权对象', dataIndex: 'userCode', width: 140 },
      { title: '对象名称', dataIndex: 'userName', width: 140 },
      { title: '授权描述', dataIndex: 'authDescribe', ellipsis: true },
      { title: '租户', dataIndex: 'tenantId', width: 120 },
      { title: '机构', dataIndex: 'orgId', width: 120 },
    ]"
    :fields="[]"
  />
</template>
