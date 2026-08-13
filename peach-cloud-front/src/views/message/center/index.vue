<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'

import { fetchMessagePage, fetchUnreadCount, readAllMessages, readMessage } from '../../../api/message'
import type { DataRecord } from '../../../api/admin'
import AdminLayout from '../../../layouts/admin/index.vue'

type MessageCategory = 'message' | 'announcement' | 'todo'

const activeCategory = ref<MessageCategory>('message')
const loading = ref(false)
const unreadCount = ref(0)
const rows = ref<DataRecord[]>([])
const total = ref(0)
const errorMessage = ref('')
const query = reactive({ pageNum: 1, pageSize: 10, readFlag: undefined as number | undefined })

const categoryTitle = computed(() => {
  const titleMap: Record<MessageCategory, string> = {
    message: '消息',
    announcement: '公告',
    todo: '待办',
  }
  return titleMap[activeCategory.value]
})
const emptyDescription = computed(() => `暂无${categoryTitle.value}数据。系统通知会通过 WebSocket 实时更新，也可以点击刷新重新获取。`)
const columns = [
  { title: '标题', dataIndex: 'title', ellipsis: true },
  { title: '类型', dataIndex: 'messageType', width: 130 },
  { title: '来源', dataIndex: 'sourceType', width: 130 },
  { title: '状态', dataIndex: 'readFlag', width: 110 },
  { title: '创建时间', dataIndex: 'createdTime', width: 190 },
  { title: '操作', dataIndex: 'actions', width: 120, fixed: 'right' as const },
]

async function loadUnreadCount() {
  unreadCount.value = await fetchUnreadCount()
}

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    const page = await fetchMessagePage(activeCategory.value, query)
    rows.value = page.list ?? []
    total.value = page.total ?? rows.value.length
    await loadUnreadCount()
  } catch (error: unknown) {
    rows.value = []
    total.value = 0
    errorMessage.value = error instanceof Error ? error.message : '加载消息失败'
    message.error(errorMessage.value)
  } finally {
    loading.value = false
  }
}

async function markRead(record: DataRecord) {
  const messageId = String(record.id ?? record.messageId ?? '')
  if (!messageId) {
    message.warning('缺少消息ID')
    return
  }
  await readMessage(messageId)
  message.success('已标记为已读')
  await loadData()
}

async function markAllRead() {
  await readAllMessages(activeCategory.value)
  message.success('已全部标记为已读')
  await loadData()
}

function changeCategory(category: MessageCategory) {
  activeCategory.value = category
  query.pageNum = 1
  void loadData()
}

function handleCategoryChange(key: string | number) {
  changeCategory(String(key) as MessageCategory)
}

function handleTableChange(pagination: { current?: number; pageSize?: number }) {
  query.pageNum = pagination.current ?? 1
  query.pageSize = pagination.pageSize ?? 10
  void loadData()
}

function readFlagText(value: unknown): string {
  return Number(value) === 1 ? '已读' : '未读'
}

onMounted(loadData)
</script>

<template>
  <AdminLayout>
    <a-card class="admin-page-card">
      <template #title>
        <div class="page-heading">
          <div>
            <h2>消息中心</h2>
            <p>集中查看消息、公告和待办；WebSocket 通知会实时更新未读数量。</p>
          </div>
          <a-space>
            <a-badge :count="unreadCount">
              <span class="ws-status ws-status--connected">未读</span>
            </a-badge>
            <a-button @click="loadData">刷新</a-button>
            <a-button type="primary" @click="markAllRead">全部已读</a-button>
          </a-space>
        </div>
      </template>
      <a-tabs :active-key="activeCategory" @change="handleCategoryChange">
        <a-tab-pane key="message" tab="消息" />
        <a-tab-pane key="announcement" tab="公告" />
        <a-tab-pane key="todo" tab="待办" />
      </a-tabs>
      <a-table
        class="admin-data-table"
        :loading="loading"
        :data-source="rows"
        row-key="id"
        :pagination="{ current: query.pageNum, pageSize: query.pageSize, total, showSizeChanger: true }"
        :columns="columns"
        :scroll="{ x: 'max-content' }"
        @change="handleTableChange"
      >
        <template #emptyText>
          <a-result
            v-if="errorMessage"
            status="warning"
            title="消息加载失败"
            :sub-title="errorMessage"
          >
            <template #extra>
              <a-button type="primary" @click="loadData">重新加载</a-button>
            </template>
          </a-result>
          <a-empty v-else :description="emptyDescription" />
        </template>
        <template #bodyCell="{ column, record }">
          <a-tag v-if="column.dataIndex === 'readFlag'" :color="Number(record.readFlag) === 1 ? 'green' : 'blue'">
            {{ readFlagText(record.readFlag) }}
          </a-tag>
          <a-button
            v-else-if="column.dataIndex === 'actions'"
            type="link"
            :disabled="Number(record.readFlag) === 1"
            @click="markRead(record as DataRecord)"
          >
            标记已读
          </a-button>
        </template>
      </a-table>
    </a-card>
  </AdminLayout>
</template>
