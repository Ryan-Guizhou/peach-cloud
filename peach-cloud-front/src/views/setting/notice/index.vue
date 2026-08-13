<script setup lang="ts">
import CrudPage from '../../../components/admin/CrudPage.vue'
</script>

<template>
  <CrudPage
    title="公告管理"
    description="维护系统公告、发布状态、生效时间和站内消息触达配置。"
    row-key="id"
    permission-prefix="notice"
    :endpoint="{
      pageList: '/setting/notice/pageList',
      add: '/setting/notice/save',
      update: '/setting/notice/update',
      delById: '/setting/notice/delete',
      idField: 'id',
      deleteMode: 'body-list',
    }"
    :columns="[
      { title: 'ID', dataIndex: 'id', width: 180 },
      { title: '公告编码', dataIndex: 'noticeCode' },
      { title: '标题消息 Key', dataIndex: 'titleMessageKey' },
      { title: '公告类型', dataIndex: 'noticeType' },
      { title: '发布状态', dataIndex: 'publishStatus' },
      { title: '优先级', dataIndex: 'priority' },
      { title: '阅读数', dataIndex: 'readCount' },
    ]"
    :fields="[
      { label: '公告编码', name: 'noticeCode', required: true },
      { label: '标题消息 Key', name: 'titleMessageKey', required: true },
      { label: '内容消息 Key', name: 'contentMessageKey', required: true },
      { label: '公告类型', name: 'noticeType' },
      { label: '优先级', name: 'priority', type: 'number' },
      { label: '发布状态', name: 'publishStatus' },
      { label: '生效开始时间', name: 'effectiveFrom' },
      { label: '生效结束时间', name: 'effectiveTo' },
      { label: '启用收件箱', name: 'inboxEnabled', type: 'number' },
      { label: '状态', name: 'status', type: 'number' },
    ]"
  />
</template>
