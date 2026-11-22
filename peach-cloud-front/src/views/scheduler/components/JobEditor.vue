<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { message, type FormInstance } from 'ant-design-vue'
import { fetchHandlers, previewCron, saveJob } from '../../../api/scheduler'
import type { SchedulerHandler, SchedulerJob, SchedulerJobSave } from '../../../types/scheduler'
import { defaultJob, errorText, options, scheduleLabels } from '../shared'

const props = defineProps<{ open: boolean; job?: SchedulerJob }>()
const emit = defineEmits<{ close: []; saved: [] }>()
const form = reactive<SchedulerJobSave>(defaultJob())
const formRef = ref<FormInstance>()
const handlers = ref<SchedulerHandler[]>([])
const handlerLoading = ref(false)
const saving = ref(false)
const previewing = ref(false)
const preview = ref<string[]>([])
const failure = ref('')
const applications = computed(() => [...new Set(handlers.value.map(item => item.applicationName))].map(value => ({ value, label: value })))
const handlerOptions = computed(() => handlers.value.filter(item => item.applicationName === form.applicationName).map(item => ({ value: item.handlerName, label: item.handlerName })))
let generation = 0
watch(() => props.open, async (open) => {
  const current = ++generation
  if (!open) return
  Object.keys(form).forEach(key => Reflect.deleteProperty(form, key))
  Object.assign(form, defaultJob())
  if (props.job) {
    // 只复制保存契约字段，避免提交状态、审计人等运行时属性。
    for (const key of [...Object.keys(defaultJob()), 'description', 'intervalSeconds', 'startAt']) {
      if (key in props.job) Reflect.set(form, key, Reflect.get(props.job, key))
    }
  }
  failure.value = ''
  preview.value = []
  formRef.value?.clearValidate()
  handlerLoading.value = true
  try {
    const result = await fetchHandlers()
    if (current === generation) handlers.value = result.filter(item => item.status === 'ONLINE')
  } catch (error: unknown) {
    if (current === generation) { handlers.value = []; failure.value = errorText(error) }
  } finally {
    if (current === generation) handlerLoading.value = false
  }
})
watch(() => [form.cronExpression, form.timeZone, form.scheduleType], () => { preview.value = [] })

const required = [{ required: true, message: '请填写或选择此项', whitespace: true }]
const rules = {
  jobCode: required, jobName: required, applicationName: required, handlerName: required,
  timeZone: [{ required: true, validator: async () => {
    try { new Intl.DateTimeFormat('zh-CN', { timeZone: form.timeZone }) } catch { throw new Error('请输入有效 IANA 时区，如 Asia/Shanghai') }
  } }],
  cronExpression: [{ validator: async () => { if (form.scheduleType === 'CRON' && !form.cronExpression?.trim()) throw new Error('请填写 Quartz Cron 表达式') } }],
  intervalSeconds: [{ validator: async () => { if (form.scheduleType === 'FIXED_INTERVAL' && (!Number.isSafeInteger(form.intervalSeconds) || Number(form.intervalSeconds) < 1)) throw new Error('请输入正整数秒数') } }],
  startAt: [{ validator: async () => { if (form.scheduleType === 'ONE_TIME' && !form.startAt) throw new Error('请选择执行时间') } }],
  parametersJson: [{ validator: async () => { try { JSON.parse(form.parametersJson) } catch { throw new Error('请输入合法 JSON') } } }],
}

async function showPreview() {
  if (previewing.value) return
  previewing.value = true
  const expression = form.cronExpression ?? ''
  const timeZone = form.timeZone
  try {
    const result = await previewCron(expression, timeZone)
    if (expression === form.cronExpression && timeZone === form.timeZone) preview.value = result
  } catch (error: unknown) { message.error(errorText(error)) }
  finally { previewing.value = false }
}

async function submit() {
  if (saving.value) return
  try { await formRef.value?.validate() } catch { return }
  if (!handlers.value.some(item => item.applicationName === form.applicationName && item.handlerName === form.handlerName)) {
    failure.value = '请选择已注册且在线的应用和 Handler'
    return
  }
  saving.value = true
  failure.value = ''
  try {
    const payload = { ...form }
    if (payload.scheduleType !== 'CRON') delete payload.cronExpression
    if (payload.scheduleType !== 'FIXED_INTERVAL') delete payload.intervalSeconds
    await saveJob(payload, props.job?.id)
    message.success('任务已保存')
    emit('saved')
  } catch (error: unknown) { failure.value = errorText(error) }
  finally { saving.value = false }
}
</script>

<template>
  <a-modal :open="open" :title="job ? '编辑调度任务' : '新增调度任务'" :width="760" :confirm-loading="saving" :closable="!saving" :mask-closable="false" :keyboard="!saving" :cancel-button-props="{ disabled: saving }" ok-text="保存任务" cancel-text="取消" @ok="submit" @cancel="emit('close')">
    <a-alert v-if="failure" type="error" show-icon :message="failure" class="form-alert" />
    <a-alert v-if="!handlerLoading && !handlers.length" type="warning" show-icon message="暂无在线 Handler，请先启动业务执行服务并完成注册，再创建或保存任务。" class="form-alert" />
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" :disabled="saving" novalidate>
      <div class="form-grid">
        <a-form-item label="任务编码" name="jobCode"><a-input v-model:value="form.jobCode" :maxlength="64" :disabled="Boolean(job)" /></a-form-item>
        <a-form-item label="任务名称" name="jobName"><a-input v-model:value="form.jobName" :maxlength="128" /></a-form-item>
        <a-form-item label="目标应用" name="applicationName"><a-select v-model:value="form.applicationName" :loading="handlerLoading" :options="applications" show-search @change="form.handlerName = ''" /></a-form-item>
        <a-form-item label="Handler" name="handlerName"><a-select v-model:value="form.handlerName" :options="handlerOptions" :disabled="!form.applicationName" show-search /></a-form-item>
        <a-form-item label="调度类型" name="scheduleType"><a-select v-model:value="form.scheduleType" :options="options(scheduleLabels)" /></a-form-item>
        <a-form-item label="调度时区" name="timeZone"><a-input v-model:value="form.timeZone" :maxlength="64" placeholder="Asia/Shanghai" /></a-form-item>
      </div>
      <a-form-item v-if="form.scheduleType === 'CRON'" label="Quartz Cron 表达式" name="cronExpression" extra="使用 Quartz 六或七段表达式，例如 0 0/5 * * * ?">
        <a-input v-model:value="form.cronExpression" :maxlength="128" />
        <a-button :loading="previewing" class="preview-button" @click="showPreview">预览未来 5 次触发时间</a-button>
        <ol v-if="preview.length"><li v-for="time in preview" :key="time">{{ time }}</li></ol>
      </a-form-item>
      <a-form-item v-if="form.scheduleType === 'FIXED_INTERVAL'" label="固定周期（秒）" name="intervalSeconds"><a-input-number v-model:value="form.intervalSeconds" :min="1" :precision="0" /></a-form-item>
      <a-form-item label="起始时间" name="startAt" :extra="form.scheduleType === 'ONE_TIME' ? '一次性任务必填，按调度时区解释' : '可选，按调度时区解释'">
        <a-date-picker v-model:value="form.startAt" show-time value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择日期与时间" />
      </a-form-item>
      <div class="form-grid">
        <a-form-item label="错过触发策略"><a-select v-model:value="form.misfirePolicy" :options="options({ FIRE_ONCE_NOW: '立即补触发一次', SKIP: '跳过错过的触发' })" /></a-form-item>
        <a-form-item label="并发策略"><a-select v-model:value="form.concurrencyPolicy" :options="options({ DISALLOW: '串行等待', ALLOW: '允许并发', SKIP_IF_RUNNING: '运行中则跳过' })" /></a-form-item>
        <a-form-item label="执行超时（毫秒）" name="timeoutMs" :rules="[{ required: true, type: 'integer', min: 1000, max: 86400000, message: '请输入 1000～86400000 的整数' }]"><a-input-number v-model:value="form.timeoutMs" :min="1000" :max="86400000" :precision="0" /></a-form-item>
        <a-form-item label="最大执行次数（包含首次）" name="maxAttempts" :rules="[{ required: true, type: 'integer', min: 1, max: 20, message: '请输入 1～20 的整数' }]"><a-input-number v-model:value="form.maxAttempts" :min="1" :max="20" :precision="0" /></a-form-item>
        <a-form-item label="重试间隔（秒）" name="retryIntervalSeconds" :rules="[{ required: true, type: 'integer', min: 1, max: 86400, message: '请输入 1～86400 的整数' }]"><a-input-number v-model:value="form.retryIntervalSeconds" :min="1" :max="86400" :precision="0" /></a-form-item>
      </div>
      <a-form-item label="任务参数 JSON" name="parametersJson" extra="不得包含密码、令牌等敏感凭据"><a-textarea v-model:value="form.parametersJson" :maxlength="16384" :auto-size="{ minRows: 4, maxRows: 12 }" /></a-form-item>
      <a-form-item label="任务说明"><a-textarea v-model:value="form.description" :maxlength="500" :auto-size="{ minRows: 2, maxRows: 5 }" /></a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 var(--space-4); }
.form-alert { margin-bottom: var(--space-4); }
.preview-button { margin-top: var(--space-2); }
:deep(textarea) { resize: none; }
@media (max-width: 600px) { .form-grid { grid-template-columns: 1fr; } }
</style>
