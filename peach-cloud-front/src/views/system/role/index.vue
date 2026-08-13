<script setup lang="ts">
import CrudPage from '../../../components/admin/CrudPage.vue'
</script>

<template>
  <CrudPage
    title="角色管理"
    description="维护角色编码、名称、作用域及授权入口。"
    row-key="roleId"
    permission-prefix="role"
    :endpoint="{ pageList: '/auth/role/pageList', add: '/auth/role/add', update: '/auth/role/update', delById: '/auth/role/delById', idField: 'roleId' }"
    :columns="[
      { title: '角色ID', dataIndex: 'roleId', width: 180 },
      { title: '角色编码', dataIndex: 'roleCode' },
      { title: '角色名称', dataIndex: 'roleName' },
      { title: '租户', dataIndex: 'tenantId' },
      { title: '机构', dataIndex: 'orgId' },
    ]"
    :fields="[
      { label: '角色ID', name: 'roleId' },
      { label: '角色编码', name: 'roleCode', required: true },
      { label: '角色名称', name: 'roleName', required: true },
      { label: '租户ID', name: 'tenantId' },
      { label: '机构ID', name: 'orgId' },
    ]"
  />
</template>
