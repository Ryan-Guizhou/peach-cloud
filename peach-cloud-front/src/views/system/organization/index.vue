<script setup lang="ts">
import CrudPage from '../../../components/admin/CrudPage.vue'
</script>

<template>
  <CrudPage
    title="机构管理"
    description="维护租户下机构数据，支撑多机构上下文切换。"
    row-key="orgId"
    permission-prefix="organization"
    :endpoint="{ pageList: '/auth/organization/pageList', add: '/auth/organization/add', update: '/auth/organization/update', delById: '/auth/organization/delById', idField: 'orgId' }"
    :columns="[
      { title: '机构ID', dataIndex: 'orgId', width: 180 },
      { title: '租户ID', dataIndex: 'tenantId' },
      { title: '机构编码', dataIndex: 'orgCode' },
      { title: '机构名称', dataIndex: 'orgName' },
      { title: '状态', dataIndex: 'status' },
      { title: '排序', dataIndex: 'sortNum' },
    ]"
    :fields="[
      { label: '机构ID', name: 'orgId' },
      { label: '租户ID', name: 'tenantId', required: true },
      { label: '机构编码', name: 'orgCode', required: true },
      { label: '机构名称', name: 'orgName', required: true },
      { label: '状态', name: 'status' },
      { label: '排序', name: 'sortNum', type: 'number' },
    ]"
  />
</template>
