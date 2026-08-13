<script setup lang="ts">
import CrudPage from '../../../components/admin/CrudPage.vue'
</script>

<template>
  <CrudPage
    title="路由管理"
    description="维护后端登录返回的路由列表，filePath 对应前端 views 路径。"
    row-key="routerId"
    permission-prefix="router"
    :endpoint="{ pageList: '/auth/router/pageList', add: '/auth/router/add', update: '/auth/router/update', delById: '/auth/router/delById', idField: 'routerId' }"
    :columns="[
      { title: '路由ID', dataIndex: 'routerId', width: 180 },
      { title: '路由编码', dataIndex: 'routerCode' },
      { title: '路由名称', dataIndex: 'routerName' },
      { title: '路由地址', dataIndex: 'routerUrl' },
      { title: '文件路径', dataIndex: 'filePath' },
    ]"
    :fields="[
      { label: '路由ID', name: 'routerId' },
      { label: '路由编码', name: 'routerCode', required: true },
      { label: '路由名称', name: 'routerName', required: true },
      { label: '路由地址', name: 'routerUrl', required: true },
      { label: '文件路径', name: 'filePath', required: true },
      { label: '应用ID', name: 'appId' },
    ]"
  />
</template>
