<script setup lang="ts">
import CrudPage from '../../../components/admin/CrudPage.vue'
</script>

<template>
  <CrudPage
    title="功能定义字段"
    description="维护功能定义编码，用于菜单、路由和授权关系绑定。"
    row-key="funcId"
    permission-prefix="function"
    :endpoint="{ pageList: '/auth/function/pageList', add: '/auth/function/add', update: '/auth/function/update', delById: '/auth/function/delById', idField: 'funcId' }"
    :columns="[
      { title: '功能ID', dataIndex: 'funcId', width: 180 },
      { title: '功能编码', dataIndex: 'funcCode' },
      { title: '父功能编码', dataIndex: 'parentFuncCode' },
      { title: '功能名称', dataIndex: 'funcName' },
      { title: '功能URL', dataIndex: 'funcUrl' },
      { title: '功能类型', dataIndex: 'funcType' },
      { title: '应用ID', dataIndex: 'appId' },
    ]"
    :fields="[
      { label: '功能ID', name: 'funcId' },
      { label: '功能编码', name: 'funcCode', required: true },
      { label: '父功能编码', name: 'parentFuncCode' },
      { label: '功能名称', name: 'funcName', required: true },
      { label: '功能描述', name: 'funcDesc', type: 'textarea' },
      { label: '功能URL', name: 'funcUrl' },
      { label: '序列号', name: 'funcSeq' },
      { label: '功能类型', name: 'funcType' },
      { label: '是否菜单', name: 'isMenu', type: 'number' },
      { label: '是否授权', name: 'isAuthorize', type: 'number' },
      { label: '应用ID', name: 'appId' },
      { label: '是否禁用', name: 'isDisable', type: 'number' },
    ]"
  />
</template>
