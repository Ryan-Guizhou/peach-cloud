<script setup lang="ts">
import CrudPage from '../../../components/admin/CrudPage.vue'
</script>

<template>
  <CrudPage
    title="字典管理"
    description="维护字典类型主数据，支撑业务枚举、页面筛选和状态展示复用。"
    row-key="id"
    permission-prefix="dict"
    :endpoint="{
      pageList: '/setting/dict/type/pageList',
      add: '/setting/dict/type/save',
      update: '/setting/dict/type/update',
      delById: '/setting/dict/type/delete',
      idField: 'id',
      deleteMode: 'body-list',
    }"
    :columns="[
      { title: 'ID', dataIndex: 'id', width: 180 },
      { title: '字典编码', dataIndex: 'dictCode' },
      { title: '字典名称', dataIndex: 'dictName' },
      { title: '模块编码', dataIndex: 'moduleCode' },
      { title: '排序', dataIndex: 'sortOrder' },
      { title: '状态', dataIndex: 'status' },
    ]"
    :fields="[
      { label: '字典编码', name: 'dictCode', required: true },
      { label: '字典名称', name: 'dictName', required: true },
      { label: '模块编码', name: 'moduleCode' },
      { label: '排序', name: 'sortOrder', type: 'number' },
      { label: '状态', name: 'status', type: 'number' },
    ]"
  />
</template>
