export type JobState = 'DRAFT' | 'ENABLED' | 'PAUSED' | 'DISABLED' | 'DELETED'
export type ExecutionState = 'CREATED' | 'QUEUED' | 'RUNNING' | 'RETRY_WAIT' | 'SUCCEEDED' | 'FAILED' | 'TIMED_OUT' | 'CANCELLED' | 'SKIPPED' | 'DEAD'
export type JobAction = 'enable' | 'pause' | 'resume' | 'disable' | 'delete' | 'run'

export interface SchedulerJobSave {
  jobCode: string
  jobName: string
  applicationName: string
  handlerName: string
  description?: string
  scheduleType: 'CRON' | 'FIXED_INTERVAL' | 'ONE_TIME'
  cronExpression?: string
  intervalSeconds?: number
  startAt?: string
  timeZone: string
  misfirePolicy: string
  concurrencyPolicy: string
  timeoutMs: number
  maxAttempts: number
  retryIntervalSeconds: number
  parametersJson: string
}

export interface SchedulerJob extends SchedulerJobSave {
  id: string
  state: JobState
  syncStatus: string
  lastSyncError?: string
  nextFireTime?: string
  lastFireTime?: string
  scheduleVersion: number
  createdTime?: string
  modifyTime?: string
}

export interface SchedulerExecution {
  executionId: string
  jobId: string
  jobCode: string
  occurrenceKey?: string
  triggerType: string
  scheduledTime?: string
  state: ExecutionState
  attempt: number
  executorInstance?: string
  leaseUntil?: string
  nextRetryTime?: string
  startTime?: string
  finishTime?: string
  durationMs?: number
  traceId?: string
  errorType?: string
  errorMessage?: string
  createdTime?: string
  modifyTime?: string
}

export interface SchedulerHandler {
  id: string
  applicationName: string
  handlerName: string
  description?: string
  status: string
  instanceId?: string
  lastHeartbeatTime?: string
}

export interface SchedulerQuery {
  executionId?: string
  triggerType?: string
  executorInstance?: string
  scheduledFrom?: string
  scheduledTo?: string
  pageNum: number
  pageSize: number
  keyword?: string
  applicationName?: string
  jobCode?: string
  state?: string
}
