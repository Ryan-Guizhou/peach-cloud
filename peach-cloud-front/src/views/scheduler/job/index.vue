<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import AdminLayout from '../components/SchedulerLayout.vue'
import { actOnJob, fetchJob, fetchJobs } from '../../../api/scheduler'
import type { JobAction, SchedulerJob } from '../../../types/scheduler'
import { actionLabels, canAct, displayTime, errorText, jobStates, options, scheduleLabels } from '../shared'
import JobEditor from '../components/JobEditor.vue'
import SchedulerDetail from '../components/SchedulerDetail.vue'
import SchedulerPager from '../components/SchedulerPager.vue'

const router = useRouter()
const query = reactive({ pageNum: 1, pageSize: 20, keyword: '', applicationName: '', state: undefined as string | undefined })
const rows = ref<SchedulerJob[]>([])
const loading = ref(false)
const failure = ref('')
const busy = ref(false)
const editorOpen = ref(false)
const editing = ref<SchedulerJob>()
const detail = ref<SchedulerJob>()
const detailOpen = ref(false)
const detailLoading = ref(false)
let sequence = 0
let detailSequence = 0
onBeforeUnmount(() => { sequence++; detailSequence++ })
const columns = [
  { title: '任务编码 / 名称', key: 'job', width: 220 },
  { title: '应用 / Handler', key: 'handler', width: 220 },
  { title: '调度方式', key: 'schedule', width: 220 },
  { title: '状态', key: 'state', width: 110 },
  { title: '引擎同步', dataIndex: 'syncStatus', width: 120 },
  { title: '下次触发', key: 'nextFireTime', width: 190 },
  { title: '操作', key: 'actions', width: 350, fixed: 'right' as const },
]
const fields = computed(() => detail.value ? [
  { label: '任务 ID', value: detail.value.id }, { label: '任务编码', value: detail.value.jobCode },
  { label: '任务名称', value: detail.value.jobName }, { label: '目标应用', value: detail.value.applicationName },
  { label: 'Handler', value: detail.value.handlerName }, { label: '状态', value: jobStates[detail.value.state] },
  { label: '调度类型', value: scheduleLabels[detail.value.scheduleType] }, { label: 'Cron', value: detail.value.cronExpression },
  { label: '周期（秒）', value: detail.value.intervalSeconds }, { label: '起始时间', value: displayTime(detail.value.startAt) },
  { label: '时区', value: detail.value.timeZone }, { label: '错过触发策略', value: detail.value.misfirePolicy },
  { label: '并发策略', value: detail.value.concurrencyPolicy }, { label: '超时（毫秒）', value: detail.value.timeoutMs },
  { label: '最大执行次数', value: detail.value.maxAttempts }, { label: '重试间隔（秒）', value: detail.value.retryIntervalSeconds },
  { label: '参数', value: detail.value.parametersJson }, { label: '说明', value: detail.value.description },
  { label: '同步状态', value: detail.value.syncStatus }, { label: '同步错误', value: detail.value.lastSyncError },
  { label: '调度版本', value: detail.value.scheduleVersion }, { label: '最近触发', value: displayTime(detail.value.lastFireTime) },
  { label: '下次触发', value: displayTime(detail.value.nextFireTime) }, { label: '创建时间', value: displayTime(detail.value.createdTime) },
] : [])

async function load(page = query.pageNum) {
  query.pageNum = page
  const current = ++sequence
  loading.value = true
  failure.value = ''
  try { const result = await fetchJobs({ ...query }); if (current === sequence) rows.value = result }
  catch (error: unknown) { if (current === sequence) { rows.value = []; failure.value = errorText(error) } }
  finally { if (current === sequence) loading.value = false }
}
async function openJob(row: SchedulerJob, edit: boolean) {
  if (busy.value) return
  busy.value = true
  const current = ++detailSequence
  if (!edit) { detail.value = undefined; detailOpen.value = true; detailLoading.value = true }
  try {
    const job = await fetchJob(row.id)
    if (current !== detailSequence) return
    if (edit) { editing.value = job; editorOpen.value = true } else detail.value = job
  } catch (error: unknown) { message.error(errorText(error)); detailOpen.value = false }
  finally { busy.value = false; detailLoading.value = false }
}
function create() { editing.value = undefined; editorOpen.value = true }
async function saved() { editorOpen.value = false; await load(1) }
function confirmAction(row: SchedulerJob, action: JobAction) {
  if (busy.value || !canAct(row.state, action)) return
  Modal.confirm({
    title: `${actionLabels[action]}「${row.jobName}」？`,
    content: action === 'run' ? '将创建一次人工执行实例，不修改原调度计划。' : action === 'delete' ? '任务将被逻辑删除。' : '操作后将刷新任务状态；引擎同步结果请查看同步状态。',
    okText: actionLabels[action], cancelText: '取消', okType: action === 'delete' ? 'danger' : 'primary',
    async onOk() {
      if (busy.value) return
      busy.value = true
      try {
        const result = await actOnJob(row.id, action)
        message.success(action === 'run' ? `已创建执行实例：${String(result ?? '')}` : '操作成功')
        await load()
      } catch (error: unknown) { message.error(errorText(error)); throw error }
      finally { busy.value = false }
    },
  })
}
onMounted(() => load())
</script>

<template>
  <AdminLayout>
    <a-card class="admin-page-card">
      <template #title><div class="page-heading"><div><h2>任务管理</h2><p>配置调度计划，管理任务生命周期与人工触发。</p></div></div></template>
      <template #extra><a-button type="primary" @click="create">新增任务</a-button></template>
      <a-space wrap class="scheduler-toolbar">
        <a-input v-model:value="query.keyword" aria-label="任务编码或名称" placeholder="任务编码或名称" allow-clear @press-enter="load(1)" />
        <a-input v-model:value="query.applicationName" aria-label="目标应用" placeholder="目标应用" allow-clear @press-enter="load(1)" />
        <a-select v-model:value="query.state" aria-label="任务状态" placeholder="全部状态" allow-clear :options="options(Object.fromEntries(Object.entries(jobStates).filter(([state]) => state !== 'DELETED')))" style="width: 150px" />
        <a-button type="primary" :loading="loading" @click="load(1)">查询</a-button>
        <a-button :disabled="loading" @click="query.keyword = ''; query.applicationName = ''; query.state = undefined; load(1)">重置</a-button>
      </a-space>
      <a-alert v-if="failure" type="error" show-icon :message="failure"><template #action><a-button size="small" @click="load()">重试</a-button></template></a-alert>
      <a-table row-key="id" :columns="columns" :data-source="rows" :loading="loading" :pagination="false" :scroll="{ x: 1430 }" :locale="{ emptyText: failure ? '加载失败，请重试' : '暂无调度任务' }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'job'"><strong>{{ record.jobName }}</strong><div>{{ record.jobCode }}</div></template>
          <template v-else-if="column.key === 'handler'">{{ record.applicationName }}<div>{{ record.handlerName }}</div></template>
          <template v-else-if="column.key === 'schedule'">{{ scheduleLabels[record.scheduleType as keyof typeof scheduleLabels] }}<div>{{ record.scheduleType === 'CRON' ? record.cronExpression : record.scheduleType === 'FIXED_INTERVAL' ? `${record.intervalSeconds} 秒` : displayTime(record.startAt) }}</div><small>{{ record.timeZone }}</small></template>
          <template v-else-if="column.key === 'state'"><a-tag :color="record.state === 'ENABLED' ? 'success' : 'default'">{{ jobStates[record.state as keyof typeof jobStates] }}</a-tag></template>
          <template v-else-if="column.key === 'nextFireTime'">{{ displayTime(record.nextFireTime) }}</template>
          <template v-else-if="column.key === 'actions'">
            <a-space wrap size="small">
              <a-button size="small" type="link" :disabled="busy" @click="openJob(record, false)">详情</a-button>
              <a-button size="small" type="link" :disabled="busy || record.state === 'DELETED'" @click="openJob(record, true)">编辑</a-button>
              <a-button size="small" type="link" @click="router.push({ path: '/scheduler/executions', query: { jobCode: record.jobCode } })">执行记录</a-button>
              <a-tooltip v-for="(label, action) in actionLabels" :key="action" :title="canAct(record.state, action) ? undefined : '当前状态不支持此操作'">
                <span><a-button size="small" type="link" :danger="action === 'delete'" :disabled="busy || !canAct(record.state, action)" @click="confirmAction(record, action)">{{ label }}</a-button></span>
              </a-tooltip>
            </a-space>
          </template>
        </template>
      </a-table>
      <SchedulerPager :page="query.pageNum" :count="rows.length" :page-size="query.pageSize" :loading="loading" @change="load" />
    </a-card>
    <JobEditor :open="editorOpen" :job="editing" @close="editorOpen = false" @saved="saved" />
    <SchedulerDetail :open="detailOpen" title="任务详情" :loading="detailLoading" :fields="fields" @close="detailOpen = false" />
  </AdminLayout>
</template>

<style scoped>
.scheduler-toolbar { margin-bottom: var(--space-4); }
</style>
