<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { message, type FormInstance } from 'ant-design-vue'
import AdminLayout from '../components/SchedulerLayout.vue'
import { actOnExecution, fetchExecution, fetchExecutions } from '../../../api/scheduler'
import type { SchedulerExecution } from '../../../types/scheduler'
import { displayTime, errorText, executionStates, options } from '../shared'
import SchedulerPager from '../components/SchedulerPager.vue'
import SchedulerDetail from '../components/SchedulerDetail.vue'

const route = useRoute()
const query = reactive({ pageNum: 1, pageSize: 20, executionId: '', triggerType: undefined as string | undefined, executorInstance: '', scheduledFrom: undefined as string | undefined, scheduledTo: undefined as string | undefined, jobCode: '', state: undefined as string | undefined })
const rows = ref<SchedulerExecution[]>([])
const loading = ref(false)
const failure = ref('')
const detail = ref<SchedulerExecution>()
const detailOpen = ref(false)
const detailLoading = ref(false)
const operation = ref<{ row: SchedulerExecution; action: 'retry' | 'cancel' }>()
const reasonForm = reactive({ reason: '' })
const formRef = ref<FormInstance>()
const saving = ref(false)
const operationFailure = ref('')
let sequence = 0
let detailSequence = 0
onBeforeUnmount(() => { sequence++; detailSequence++ })
const fields = computed(() => detail.value ? [
  { label: '执行实例', value: detail.value.executionId }, { label: '任务 ID', value: detail.value.jobId },
  { label: '任务编码', value: detail.value.jobCode }, { label: '状态', value: executionStates[detail.value.state] },
  { label: '触发来源', value: detail.value.triggerType }, { label: '触发唯一键', value: detail.value.occurrenceKey },
  { label: '计划时间', value: displayTime(detail.value.scheduledTime) }, { label: '尝试次数', value: detail.value.attempt },
  { label: '执行器实例', value: detail.value.executorInstance }, { label: '租约到期', value: displayTime(detail.value.leaseUntil) },
  { label: '下次重试', value: displayTime(detail.value.nextRetryTime) }, { label: '开始时间', value: displayTime(detail.value.startTime) },
  { label: '结束时间', value: displayTime(detail.value.finishTime) }, { label: '耗时（毫秒）', value: detail.value.durationMs },
  { label: 'Trace ID', value: detail.value.traceId }, { label: '错误类型', value: detail.value.errorType },
  { label: '错误摘要', value: detail.value.errorMessage }, { label: '创建时间', value: displayTime(detail.value.createdTime) },
] : [])
const canCancel = (row: SchedulerExecution) => ['CREATED', 'QUEUED', 'RETRY_WAIT'].includes(row.state)
async function load(page = query.pageNum) {
  if (query.scheduledFrom && query.scheduledTo && query.scheduledFrom > query.scheduledTo) {
    message.warning('计划时间起点不能晚于终点')
    return
  }
  query.pageNum = page
  const current = ++sequence
  loading.value = true
  failure.value = ''
  try { const result = await fetchExecutions({ ...query }); if (current === sequence) rows.value = result }
  catch (error: unknown) { if (current === sequence) { rows.value = []; failure.value = errorText(error) } }
  finally { if (current === sequence) loading.value = false }
}
async function showDetail(row: SchedulerExecution) {
  const current = ++detailSequence
  detail.value = undefined
  detailOpen.value = true
  detailLoading.value = true
  try { const result = await fetchExecution(row.executionId); if (current === detailSequence) detail.value = result }
  catch (error: unknown) { if (current === detailSequence) { message.error(errorText(error)); detailOpen.value = false } }
  finally { if (current === detailSequence) detailLoading.value = false }
}
function openOperation(row: SchedulerExecution, action: 'retry' | 'cancel') {
  reasonForm.reason = ''
  operationFailure.value = ''
  operation.value = { row, action }
  formRef.value?.clearValidate()
}
async function submitOperation() {
  if (saving.value || !operation.value) return
  try { await formRef.value?.validate() } catch { return }
  saving.value = true
  operationFailure.value = ''
  try {
    await actOnExecution(operation.value.row.executionId, operation.value.action, reasonForm.reason.trim())
    message.success('操作已受理')
    operation.value = undefined
    await load()
  } catch (error: unknown) { operationFailure.value = errorText(error) }
  finally { saving.value = false }
}
watch(() => route.query.jobCode, (value) => { query.jobCode = typeof value === 'string' ? value : ''; void load(1) }, { immediate: true })
</script>

<template>
  <AdminLayout>
    <a-card class="admin-page-card">
      <template #title><div class="page-heading"><div><h2>执行记录</h2><p>查看任务执行过程和结果，按执行状态进行人工干预。</p></div></div></template>
      <a-space wrap class="scheduler-toolbar">
        <a-input v-model:value="query.jobCode" aria-label="任务编码" placeholder="任务编码" allow-clear @press-enter="load(1)" />
        <a-select v-model:value="query.state" aria-label="执行状态" placeholder="全部状态" allow-clear :options="options(executionStates)" style="width: 160px" />
        <a-input v-model:value="query.executionId" aria-label="执行实例 ID" placeholder="执行实例 ID" allow-clear @press-enter="load(1)" />
        <a-input v-model:value="query.executorInstance" aria-label="执行器实例" placeholder="执行器实例" allow-clear @press-enter="load(1)" />
        <a-select v-model:value="query.triggerType" aria-label="触发来源" placeholder="全部触发来源" allow-clear :options="options({ MANUAL: '人工触发', SCHEDULED: '计划触发', RETRY: '重试触发' })" style="width: 160px" />
        <a-date-picker v-model:value="query.scheduledFrom" aria-label="计划开始时间" placeholder="计划开始时间" show-time value-format="YYYY-MM-DDTHH:mm:ss" />
        <a-date-picker v-model:value="query.scheduledTo" aria-label="计划结束时间" placeholder="计划结束时间" show-time value-format="YYYY-MM-DDTHH:mm:ss" />
        <a-button type="primary" :loading="loading" @click="load(1)">查询</a-button>
        <a-button :disabled="loading" @click="query.jobCode = ''; query.state = undefined; query.executionId = ''; query.executorInstance = ''; query.triggerType = undefined; query.scheduledFrom = undefined; query.scheduledTo = undefined; load(1)">重置</a-button>
        <a-button :disabled="loading" @click="load()">刷新</a-button>
      </a-space>
      <a-alert v-if="failure" type="error" show-icon :message="failure"><template #action><a-button size="small" @click="load()">重试</a-button></template></a-alert>
      <a-spin :spinning="loading">
        <a-empty v-if="!rows.length && !loading" :description="failure ? '加载失败，请重试' : '暂无执行记录'" />
        <div class="execution-grid">
          <article v-for="record in rows" :key="record.executionId" class="execution-card">
            <header><strong>{{ record.jobCode }}</strong><a-tag :color="record.state === 'SUCCEEDED' ? 'success' : record.state === 'RUNNING' ? 'processing' : 'default'">{{ executionStates[record.state] }}</a-tag></header>
            <p class="execution-id">{{ record.executionId }}</p>
            <dl>
              <div><dt>触发来源</dt><dd>{{ record.triggerType === 'MANUAL' ? '人工触发' : record.triggerType }}</dd></div>
              <div><dt>计划时间</dt><dd>{{ displayTime(record.scheduledTime) }}</dd></div>
              <div><dt>开始时间</dt><dd>{{ displayTime(record.startTime) }}</dd></div>
              <div><dt>结束时间</dt><dd>{{ displayTime(record.finishTime) }}</dd></div>
              <div><dt>耗时</dt><dd>{{ record.durationMs == null ? '—' : `${record.durationMs} 毫秒` }}</dd></div>
              <div><dt>尝试次数</dt><dd>{{ record.attempt }}</dd></div>
              <div><dt>执行器</dt><dd>{{ record.executorInstance || '—' }}</dd></div>
            </dl>
            <a-alert v-if="record.errorMessage" type="error" :message="record.errorMessage" />
            <footer><a-space>
              <a-button size="small" @click="showDetail(record)">详情</a-button>
              <a-tooltip title="仅等待重试的实例可人工重试"><span><a-button size="small" :disabled="saving || record.state !== 'RETRY_WAIT'" @click="openOperation(record, 'retry')">重试</a-button></span></a-tooltip>
              <a-tooltip title="执行中的实例不能取消"><span><a-button size="small" danger :disabled="saving || !canCancel(record)" @click="openOperation(record, 'cancel')">取消</a-button></span></a-tooltip>
            </a-space></footer>
          </article>
        </div>
      </a-spin>
      <SchedulerPager :page="query.pageNum" :count="rows.length" :page-size="query.pageSize" :loading="loading" @change="load" />
    </a-card>
    <SchedulerDetail :open="detailOpen" title="执行详情" :loading="detailLoading" :fields="fields" @close="detailOpen = false; detailSequence++" />
    <a-modal :open="Boolean(operation)" :title="operation?.action === 'retry' ? '人工重试' : '取消执行'" :confirm-loading="saving" :mask-closable="false" :closable="!saving" :keyboard="!saving" :cancel-button-props="{ disabled: saving }" :ok-text="operation?.action === 'retry' ? '确认重试' : '确认取消执行'" cancel-text="返回" @ok="submitOperation" @cancel="operation = undefined">
      <p>执行实例：{{ operation?.row.executionId }}</p>
      <a-alert v-if="operationFailure" type="error" show-icon :message="operationFailure" />
      <a-form ref="formRef" :model="reasonForm" layout="vertical" novalidate>
        <a-form-item label="操作原因" name="reason" :rules="[{ required: true, whitespace: true, message: '请填写操作原因' }]"><a-textarea v-model:value="reasonForm.reason" :disabled="saving" :auto-size="{ minRows: 3, maxRows: 6 }" /></a-form-item>
      </a-form>
    </a-modal>
  </AdminLayout>
</template>

<style scoped>
.scheduler-toolbar { margin-bottom: var(--space-4); }
.execution-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 360px), 1fr)); gap: var(--space-4); }
.execution-card { min-width: 0; padding: var(--space-4); border: 1px solid #e6edf5; border-radius: var(--radius-sm); }
.execution-card header { display: flex; align-items: center; justify-content: space-between; gap: var(--space-2); }
.execution-card strong, .execution-card dd { overflow-wrap: anywhere; }
.execution-id { color: #697b92; overflow-wrap: anywhere; margin: var(--space-2) 0; }
.execution-card dl { margin: var(--space-4) 0; }
.execution-card dl > div { display: grid; grid-template-columns: 80px minmax(0, 1fr); gap: var(--space-2); margin-bottom: var(--space-2); }
.execution-card dt { color: #697b92; }
.execution-card dd { margin: 0; }
.execution-card footer { margin-top: var(--space-4); }
:deep(textarea) { resize: none; }
</style>
