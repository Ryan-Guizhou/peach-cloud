<script setup lang="ts">
import CrudPage from '../../../components/admin/CrudPage.vue'
</script>

<template>
  <CrudPage
    title="用户管理"
    description="维护用户基础资料；更新和删除必须提交当前记录版本，避免并发覆盖。"
    row-key="id"
    permission-prefix="user"
    :endpoint="{ pageList: '/auth/user/pageList', add: '/auth/user/add', update: '/auth/user/update', delById: '/auth/user/delById', idField: 'id', versionField: 'version' }"
    :columns="[
      { title: '用户ID', dataIndex: 'userId', width: 180 },
      { title: '用户名称', dataIndex: 'userName' },
      { title: '手机号', dataIndex: 'mobilePhone' },
      { title: '邮箱', dataIndex: 'email' },
      { title: '默认机构', dataIndex: 'defaultOrgId' },
    ]"
    :fields="[

      { label: '用户名称', name: 'userName', required: true },
      { label: '手机号', name: 'mobilePhone' },
      { label: '邮箱', name: 'email' },
      { label: '默认机构ID', name: 'defaultOrgId' },
    ]"
  />
</template>
