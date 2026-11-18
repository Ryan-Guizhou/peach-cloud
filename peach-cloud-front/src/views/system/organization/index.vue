<script setup lang="ts">
import CrudPage from '../../../components/admin/CrudPage.vue'
</script>

<template>
  <CrudPage
    title="机构管理"
    description="仅管理当前已认证租户的机构；机构编码创建后不可修改。"
    row-key="id"
    permission-prefix="organization"
    :endpoint="{ pageList: '/auth/organization/pageList', add: '/auth/organization/add', update: '/auth/organization/update', delById: '/auth/organization/delById', idField: 'id' }"
    :columns="[
      { title: '机构ID', dataIndex: 'orgId', width: 180 },
      { title: '机构编码', dataIndex: 'orgCode' },
      { title: '机构名称', dataIndex: 'orgName' },
      { title: '状态', dataIndex: 'status' },
      { title: '排序', dataIndex: 'sortNum' },
    ]"
    :fields="[

      { label: '机构编码', name: 'orgCode', required: true, immutableOnEdit: true },
      { label: '机构名称', name: 'orgName', required: true },
    ]"
  />
</template>
