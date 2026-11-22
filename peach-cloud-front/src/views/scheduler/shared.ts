import type { JobAction, JobState, SchedulerJobSave } from '../../types/scheduler'

export const jobStates: Record<JobState, string> = { DRAFT: '草稿', ENABLED: '启用', PAUSED: '已暂停', DISABLED: '禁用', DELETED: '已删除' }
export const executionStates: Record<string, string> = { CREATED: '待分发', QUEUED: '已入队', RUNNING: '执行中', RETRY_WAIT: '等待重试', SUCCEEDED: '成功', FAILED: '失败', TIMED_OUT: '超时', CANCELLED: '已取消', SKIPPED: '已跳过', DEAD: '重试耗尽' }
export const actionLabels: Record<JobAction, string> = { enable: '启用', pause: '暂停', resume: '恢复', disable: '禁用', delete: '删除', run: '立即执行' }
export const scheduleLabels = { CRON: 'Cron 表达式', FIXED_INTERVAL: '固定周期', ONE_TIME: '一次性' }
export const options = (labels: Record<string, string>) => Object.entries(labels).map(([value, label]) => ({ value, label }))
export const displayTime = (value?: string) => value ? value.replace('T', ' ') : '—'
export const errorText = (error: unknown) => error instanceof Error ? error.message : '操作失败，请重试'
export function canAct(state: JobState, action: JobAction): boolean {
  const allowed: Record<JobAction, JobState[]> = {
    enable: ['DRAFT', 'DISABLED'], pause: ['ENABLED'], resume: ['PAUSED'],
    disable: ['ENABLED', 'PAUSED'], delete: ['DRAFT', 'DISABLED'], run: ['DRAFT', 'ENABLED', 'PAUSED', 'DISABLED'],
  }
  return allowed[action].includes(state)
}
export function defaultJob(): SchedulerJobSave {
  return { jobCode: '', jobName: '', applicationName: '', handlerName: '', scheduleType: 'CRON', cronExpression: '0 0/5 * * * ?', timeZone: 'Asia/Shanghai', misfirePolicy: 'FIRE_ONCE_NOW', concurrencyPolicy: 'DISALLOW', timeoutMs: 1800000, maxAttempts: 1, retryIntervalSeconds: 60, parametersJson: '{}' }
}
