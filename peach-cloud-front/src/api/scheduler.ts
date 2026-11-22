import http, { unwrapApiResponse } from './http'
import type { ApiResponse } from '../types/auth'
import type { JobAction, SchedulerExecution, SchedulerHandler, SchedulerJob, SchedulerJobSave, SchedulerQuery } from '../types/scheduler'

const base = '/scheduler'

async function get<T>(path: string, params?: object): Promise<T> {
  const { data } = await http.get<ApiResponse<T>>(`${base}${path}`, { params })
  return unwrapApiResponse(data, '加载调度数据失败', { requireData: true })
}

export const fetchJobs = (query: SchedulerQuery) => get<SchedulerJob[]>('/jobs', query)
export const fetchJob = (id: string) => get<SchedulerJob>(`/jobs/${encodeURIComponent(id)}`)
export const fetchExecutions = (query: SchedulerQuery) => get<SchedulerExecution[]>('/executions', query)
export const fetchExecution = (id: string) => get<SchedulerExecution>(`/executions/${encodeURIComponent(id)}`)
export const fetchHandlers = (query: { keyword?: string; applicationName?: string; status?: string } = {}) => get<SchedulerHandler[]>('/handlers', query)
export const previewCron = (expression: string, timeZone: string) => get<string[]>('/jobs/cron/preview', { expression, timeZone, count: 5 })

export async function saveJob(payload: SchedulerJobSave, id?: string): Promise<SchedulerJob> {
  const response = id
    ? await http.put<ApiResponse<SchedulerJob>>(`${base}/jobs/${encodeURIComponent(id)}`, payload)
    : await http.post<ApiResponse<SchedulerJob>>(`${base}/jobs`, payload)
  return unwrapApiResponse(response.data, '保存调度任务失败', { requireData: true })
}

export async function actOnJob(id: string, action: JobAction): Promise<unknown> {
  const path = `${base}/jobs/${encodeURIComponent(id)}`
  const response = action === 'delete' ? await http.delete<ApiResponse<unknown>>(path)
    : await http.post<ApiResponse<unknown>>(`${path}/${action}`)
  return unwrapApiResponse(response.data, '任务操作失败')
}

export async function actOnExecution(id: string, action: 'retry' | 'cancel', reason: string): Promise<unknown> {
  const { data } = await http.post<ApiResponse<unknown>>(`${base}/executions/${encodeURIComponent(id)}/${action}`, undefined, { params: { reason } })
  return unwrapApiResponse(data, '执行操作失败')
}

