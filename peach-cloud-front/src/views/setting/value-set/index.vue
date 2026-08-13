<script setup lang="ts">
import CrudPage from '../../../components/admin/CrudPage.vue'
</script>

<template>
  <CrudPage
    title="值集管理"
    description="维护值集主数据，供页面下拉、业务选项和规则字段复用。"
    row-key="id"
    permission-prefix="valueSet"
    :endpoint="{
      pageList: '/setting/valueSet/pageList',
      add: '/setting/valueSet/save',
      update: '/setting/valueSet/update',
      delById: '/setting/valueSet/delete',
      idField: 'id',
      deleteMode: 'body-list',
    }"
    :columns="[
      { title: 'ID', dataIndex: 'id', width: 180 },
      { title: '值集编码', dataIndex: 'valueSetCode' },
      { title: '值集名称', dataIndex: 'valueSetName' },
      { title: '模块编码', dataIndex: 'moduleCode' },
      { title: '来源类型', dataIndex: 'sourceType' },
      { title: '状态', dataIndex: 'status' },
    ]"
    :fields="[
      { label: '值集编码', name: 'valueSetCode', required: true },
      { label: '值集名称', name: 'valueSetName', required: true },
      { label: '模块编码', name: 'moduleCode' },
      { label: '来源类型', name: 'sourceType' },
      { label: '描述', name: 'description', type: 'textarea' },
      { label: '状态', name: 'status', type: 'number' },
    ]"
  />
</template>
