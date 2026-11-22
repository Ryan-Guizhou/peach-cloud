import http, { unwrapApiResponse } from './http'
import type { ApiResponse, MenuInfo } from '../types/auth'
import type { PageResult, DataRecord } from './admin'
import type { FunctionDefinition, FunctionResource } from './function'
export interface RoleRecord extends DataRecord { roleId: string; roleCode: string; roleName: string; roleDesc?: string; roleScope?: string; roleType?: string; skipUrl?: string }
export interface ResourceGrant { funcCode: string; opType: string; resourceCode: string; resourceName?: string }
export interface RoleWorkspace { role: RoleRecord; functions: FunctionDefinition[]; resources: FunctionResource[]; funcCodes: string[]; resourceGrants: ResourceGrant[]; menus: MenuInfo[]; revision: string }
export interface RoleMember { userCode: string; userName?: string }
export interface RoleMembers { users: RoleMember[]; userCodes: string[]; revision: string }
export interface Calibration { removedFuncCodes: string[]; removedResourceKeys: string[] }
export interface GrantCommand { roleId: string; revision: string; funcCodes: string[]; resourceKeys: string[] }
export async function fetchRoles(query: { keyword: string; pageNum: number; pageSize: number }, signal?: AbortSignal): Promise<PageResult<RoleRecord>> {
  const { data } = await http.post<ApiResponse<PageResult<RoleRecord>>>('/auth/role/pageList', query, { signal })
  return unwrapApiResponse(data, '加载角色失败', { requireData: true })
}
export async function fetchWorkspace(roleId: string, signal?: AbortSignal): Promise<RoleWorkspace> {
  const { data } = await http.get<ApiResponse<RoleWorkspace>>('/auth/role/workspace', { params: { roleId }, signal })
  return unwrapApiResponse(data, '加载角色授权失败', { requireData: true })
}
export async function fetchMembers(roleId: string, signal?: AbortSignal): Promise<RoleMembers> {
  const { data } = await http.get<ApiResponse<RoleMembers>>('/auth/role/members', { params: { roleId }, signal })
  return unwrapApiResponse(data, '加载角色成员失败', { requireData: true })
}
export async function saveRole(record: Partial<RoleRecord>): Promise<void> {
  const { data } = await http.post<ApiResponse<void>>(`/auth/role/${record.roleId ? 'update' : 'add'}`, record)
  unwrapApiResponse(data)
}
export async function postWorkspace(action: 'saveGrants' | 'saveMembers' | 'saveDefault', body: object): Promise<void> {
  const { data } = await http.post<ApiResponse<void>>(`/auth/role/${action}`, body)
  unwrapApiResponse(data)
}
export async function previewCalibration(command: GrantCommand): Promise<Calibration> {
  const { data } = await http.post<ApiResponse<Calibration>>('/auth/role/calibrate', command)
  return unwrapApiResponse(data, '校准失败', { requireData: true })
}
