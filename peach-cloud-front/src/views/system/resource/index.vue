<script setup lang="ts">
import CrudPage from '../../../components/admin/CrudPage.vue'
</script>

<template>
  <CrudPage
    title="资源管理"
    description="维护按钮、API 等可授权资源，供角色授权、前端按钮权限和网关接口权限使用。"
    row-key="resourceId"
    permission-prefix="resource"
    :endpoint="{ pageList: '/auth/resource/pageList', add: '/auth/resource/add', update: '/auth/resource/update', delById: '/auth/resource/delById', idField: 'resourceId' }"
    :columns="[
      { title: '资源ID', dataIndex: 'resourceId', width: 180 },
      { title: '功能编码', dataIndex: 'funcCode' },
      { title: '资源类型', dataIndex: 'resourceType' },
      { title: '资源编码', dataIndex: 'resourceCode' },
      { title: '资源名称', dataIndex: 'resourceName' },
      { title: '应用ID', dataIndex: 'appId' },
      { title: '租户ID', dataIndex: 'tenantId' },
    ]"
    :fields="[
      { label: '资源ID', name: 'resourceId' },
      { label: '功能编码', name: 'funcCode', required: true },
      { label: '资源类型', name: 'resourceType', required: true },
      { label: '资源编码', name: 'resourceCode', required: true },
      { label: '资源名称', name: 'resourceName', required: true },
      { label: '应用ID', name: 'appId' },
    ]"
  />
</template>
