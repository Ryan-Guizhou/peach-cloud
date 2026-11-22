<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import AdminLayout from '../../../layouts/admin/index.vue'
import { usePermission } from '../../../composables/usePermission'
import { fetchLoginLogs, fetchLoginLog, type LoginLog, type LoginLogQuery } from '../../../api/login-log'

const { hasPermission } = usePermission()
const canPlatform = computed(() => hasPermission('loginLog:platform'))
const query = reactive<LoginLogQuery>({ pageNum: 1, pageSize: 20, scope: 'TENANT', account: '', ipAddress: '' })
const startInput = ref('')
const endInput = ref('')
const rows = ref<LoginLog[]>([])
const total = ref(0)
const loading = ref(false)
const errorMessage = ref('')
const detail = ref<LoginLog>()
const detailOpen = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detailId = ref('')
const events = [{ value: 'LOGIN', label: '登录' }, { value: 'LOGOUT', label: '主动登出' }, { value: 'CAPTCHA', label: '验证码校验' }]
const results = [{ value: 'SUCCESS', label: '成功' }, { value: 'FAILED', label: '失败' }]
const reasons: Record<string, string> = {
  OK: '成功', REQUEST_REJECTED: '请求校验未通过', CAPTCHA_FAILED: '验证码校验失败',
  ACCOUNT_LOCKED: '账户已锁定', CREDENTIAL_DECRYPT_FAILED: '登录凭据解密失败', BAD_CREDENTIALS: '账户或密码错误',
  NO_MEMBERSHIP: '未绑定有效租户或机构', CONTEXT_UNAVAILABLE: '租户或机构不匹配',
  TENANT_UNAVAILABLE: '租户不可用或绑定已变更', INTERNAL_ERROR: '认证处理异常',
}
const columns = [
  { title: '时间', dataIndex: 'occurredAt', width: 205 },
  { title: '事件', dataIndex: 'eventType', width: 120 },
  { title: '账户', dataIndex: 'account', width: 125 },
  { title: '结果', dataIndex: 'result', width: 90 },
  { title: '原因', dataIndex: 'reasonCode', width: 220, ellipsis: true },
  { title: '来源 IP', dataIndex: 'ipAddress', width: 160 },
  { title: '浏览器 / 系统', dataIndex: 'browser', width: 180 },
  { title: '耗时', dataIndex: 'durationMs', width: 100 },
  { title: '操作', dataIndex: 'actions', width: 90, fixed: 'right' as const },
]
let request: AbortController | undefined
let detailRequest: AbortController | undefined
let disposed = false
const errorText = (error: unknown) => error instanceof Error ? error.message : '加载失败，请重试'
const eventLabel = (value: string) => events.find(event => event.value === value)?.label ?? value
const reasonLabel = (value: string) => reasons[value] ?? value

async function load() {
  request?.abort()
  const current = new AbortController()
  request = current
  loading.value = true
  errorMessage.value = ''
  rows.value = []
  total.value = 0
  try {
    const page = await fetchLoginLogs({ ...query }, current.signal)
    if (disposed || current.signal.aborted) return
    rows.value = page.list ?? page.result ?? []
    total.value = page.total ?? page.topTotal ?? 0
  } catch (error: unknown) {
    if (!disposed && !current.signal.aborted) errorMessage.value = errorText(error)
  } finally { if (!disposed && request === current) loading.value = false }
}
function search() {
  if (startInput.value && endInput.value && startInput.value >= endInput.value) {
    message.warning('开始时间必须早于结束时间')
    return
  }
  query.startTime = startInput.value ? startInput.value.replace('T', ' ') + ':00' : undefined
  query.endTime = endInput.value ? endInput.value.replace('T', ' ') + ':00' : undefined
  query.pageNum = 1
  void load()
}
function reset() {
  Object.assign(query, { pageNum: 1, pageSize: 20, scope: 'TENANT', account: '', ipAddress: '', result: undefined, eventType: undefined, startTime: undefined, endTime: undefined })
  startInput.value = ''; endInput.value = ''
  void load()
}
function pageChange(page: { current?: number; pageSize?: number }) {
  query.pageNum = page.current ?? 1
  query.pageSize = page.pageSize ?? 20
  void load()
}
async function openDetail(id: string) {
  detailRequest?.abort()
  const current = new AbortController()
  detailRequest = current
  detailId.value = id
  detail.value = undefined
  detailError.value = ''
  detailOpen.value = true
  detailLoading.value = true
  try {
    const row = await fetchLoginLog(id, current.signal)
    if (!disposed && !current.signal.aborted) detail.value = row
  } catch (error: unknown) {
    if (!disposed && !current.signal.aborted) detailError.value = errorText(error)
  } finally { if (!disposed && detailRequest === current) detailLoading.value = false }
}
function closeDetail() { detailRequest?.abort(); detailOpen.value = false }
onMounted(load)
onBeforeUnmount(() => { disposed = true; request?.abort(); detailRequest?.abort() })
</script>

<template>
  <AdminLayout responsive>
    <a-card class="admin-page-card login-log-page">
      <template #title>
        <div class="page-heading"><div><h2>登录日志</h2><p>查询登录、失败尝试和主动登出记录，账户信息已脱敏。</p></div><a-button :loading="loading" @click="load">刷新</a-button></div>
      </template>
      <a-alert class="audit-note" type="info" show-icon message="默认查询最近 7 天，日志默认保留 90 天；支持按完整账户精确查询。" />
      <a-form :model="query" layout="vertical" class="audit-filters" @finish="search">
        <a-form-item label="查询范围" name="scope"><a-select v-model:value="query.scope" :options="[{ value: 'TENANT', label: '当前租户' }, ...(canPlatform ? [{ value: 'UNASSIGNED', label: '未归属事件（平台审计）' }] : [])]" /></a-form-item>
        <a-form-item label="账户" name="account"><a-input v-model:value="query.account" placeholder="输入完整账户" :maxlength="128" allow-clear /></a-form-item>
        <a-form-item label="结果" name="result"><a-select v-model:value="query.result" :options="results" placeholder="全部结果" allow-clear /></a-form-item>
        <a-form-item label="事件" name="eventType"><a-select v-model:value="query.eventType" :options="events" placeholder="全部事件" allow-clear /></a-form-item>
        <a-form-item label="来源 IP" name="ipAddress" :rules="[{ pattern: /^[0-9a-fA-F:.]{1,45}$/, message: '请输入有效的 IP 地址' }]"><a-input v-model:value="query.ipAddress" placeholder="精确 IP 地址" :maxlength="45" allow-clear /></a-form-item>
        <a-form-item label="开始时间"><a-input id="audit-start" v-model:value="startInput" type="datetime-local" aria-label="开始时间" /></a-form-item>
        <a-form-item label="结束时间"><a-input id="audit-end" v-model:value="endInput" type="datetime-local" aria-label="结束时间" /></a-form-item>
        <a-form-item label="筛选"><a-space><a-button html-type="submit" type="primary" :loading="loading">查询</a-button><a-button @click="reset">重置</a-button></a-space></a-form-item>
      </a-form>
      <a-alert v-if="errorMessage" type="error" show-icon :message="errorMessage" class="audit-note"><template #action><a-button size="small" @click="load">重试</a-button></template></a-alert>
      <a-table :columns="columns" :data-source="rows" row-key="logId" :loading="loading" :scroll="{ x: 1290 }" :pagination="{ current: query.pageNum, pageSize: query.pageSize, total, showSizeChanger: true, pageSizeOptions: ['10', '20', '50', '100'] }" @change="pageChange">
        <template #emptyText><a-empty :description="errorMessage ? '日志加载失败，请重试' : '当前范围内暂无日志，可调整时间、账户或查询范围'" /></template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'eventType'">{{ eventLabel(record.eventType) }}</template>
          <template v-else-if="column.dataIndex === 'result'"><a-tag :color="record.result === 'SUCCESS' ? 'success' : 'error'">{{ record.result === 'SUCCESS' ? '成功' : '失败' }}</a-tag></template>
          <template v-else-if="column.dataIndex === 'reasonCode'">{{ reasonLabel(record.reasonCode) }}</template>
          <template v-else-if="column.dataIndex === 'account'">{{ record.account || '未提供' }}</template>
          <template v-else-if="column.dataIndex === 'durationMs'">{{ record.durationMs }} ms</template>
          <a-button v-else-if="column.dataIndex === 'actions'" type="link" @click="openDetail(record.logId)">详情</a-button>
        </template>
      </a-table>
    </a-card>
    <a-drawer :open="detailOpen" title="登录日志详情" width="min(560px, 100vw)" @close="closeDetail">
      <a-spin :spinning="detailLoading">
        <a-result v-if="detailError" status="warning" title="详情加载失败" :sub-title="detailError"><template #extra><a-button @click="openDetail(detailId)">重试</a-button></template></a-result>
        <a-descriptions v-else-if="detail" :column="1" bordered size="small">
          <a-descriptions-item label="事件时间">{{ detail.occurredAt }}</a-descriptions-item>
          <a-descriptions-item label="事件">{{ eventLabel(detail.eventType) }}</a-descriptions-item>
          <a-descriptions-item label="结果">{{ detail.result === 'SUCCESS' ? '成功' : '失败' }}</a-descriptions-item>
          <a-descriptions-item label="原因">{{ reasonLabel(detail.reasonCode) }}</a-descriptions-item>
          <a-descriptions-item label="账户">{{ detail.account || '未提供' }}</a-descriptions-item>
          <a-descriptions-item label="用户ID">{{ detail.userId || '未确认' }}</a-descriptions-item>
          <a-descriptions-item label="租户ID">{{ detail.tenantId || '未归属' }}</a-descriptions-item>
          <a-descriptions-item label="机构ID">{{ detail.orgId || '未归属' }}</a-descriptions-item>
          <a-descriptions-item label="来源 IP">{{ detail.ipAddress }}</a-descriptions-item>
          <a-descriptions-item label="浏览器 / 系统">{{ detail.browser }}</a-descriptions-item>
          <a-descriptions-item label="耗时">{{ detail.durationMs }} ms</a-descriptions-item>
          <a-descriptions-item label="追踪ID"><span class="trace-value">{{ detail.traceId }}</span></a-descriptions-item>
        </a-descriptions>
      </a-spin>
    </a-drawer>
  </AdminLayout>
</template>

<style scoped>
.audit-note { margin-bottom: 20px; }
.audit-filters { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 0 16px; }
.audit-filters :deep(.ant-form-item) { min-width: 0; }
.trace-value { overflow-wrap: anywhere; }
@media (max-width: 1100px) { .audit-filters { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 560px) { .audit-filters { grid-template-columns: minmax(0, 1fr); } }
</style>
