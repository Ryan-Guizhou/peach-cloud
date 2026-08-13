<script setup lang="ts">
import CrudPage from '../../../components/admin/CrudPage.vue'
</script>

<template>
  <CrudPage
    title="IP 白名单"
    description="维护网关 IP 白名单，支撑缓存预热和访问控制。"
    row-key="id"
    permission-prefix="ipWhitelist"
    :endpoint="{
      pageList: '/setting/ipWhitelist/pageList',
      add: '/setting/ipWhitelist/save',
      update: '/setting/ipWhitelist/update',
      delById: '/setting/ipWhitelist/delete',
      idField: 'id',
      deleteMode: 'body-list',
    }"
    :columns="[
      { title: 'ID', dataIndex: 'id', width: 180 },
      { title: 'IP/CIDR', dataIndex: 'ipAddress' },
      { title: '说明', dataIndex: 'ipDesc' },
      { title: '状态', dataIndex: 'status' },
      { title: '租户ID', dataIndex: 'tenantId' },
      { title: '机构ID', dataIndex: 'orgId' },
    ]"
    :fields="[
      { label: 'IP/CIDR', name: 'ipAddress', required: true },
      { label: '说明', name: 'ipDesc', type: 'textarea' },
      { label: '状态', name: 'status', type: 'number' },
    ]"
  />
</template>
