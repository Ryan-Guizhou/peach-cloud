<script setup lang="ts">
import CrudPage from '../../../components/admin/CrudPage.vue'
</script>

<template>
  <CrudPage
    title="多语言管理"
    description="维护语言基础配置，支撑后端多语言消息解析和前端展示。"
    row-key="id"
    permission-prefix="multiMessage"
    :endpoint="{
      pageList: '/setting/multiMessage/language/pageList',
      add: '/setting/multiMessage/language/save',
      update: '/setting/multiMessage/language/update',
      delById: '/setting/multiMessage/language/delete',
      idField: 'id',
      deleteMode: 'body-list',
    }"
    :columns="[
      { title: 'ID', dataIndex: 'id', width: 180 },
      { title: '语言编码', dataIndex: 'languageCode' },
      { title: '语言名称', dataIndex: 'languageName' },
      { title: '本地名称', dataIndex: 'nativeName' },
      { title: '默认', dataIndex: 'defaultFlag' },
      { title: '状态', dataIndex: 'status' },
      { title: '排序', dataIndex: 'sortOrder' },
    ]"
    :fields="[
      { label: '语言编码', name: 'languageCode', required: true },
      { label: '语言名称', name: 'languageName', required: true },
      { label: '本地名称', name: 'nativeName' },
      { label: '图标', name: 'icon' },
      { label: '默认标识', name: 'defaultFlag', type: 'number' },
      { label: '状态', name: 'status', type: 'number' },
      { label: '排序', name: 'sortOrder', type: 'number' },
    ]"
  />
</template>
