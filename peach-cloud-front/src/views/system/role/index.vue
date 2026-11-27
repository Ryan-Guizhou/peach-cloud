<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { message, Modal, type FormInstance } from 'ant-design-vue'
import AdminLayout from '../../../layouts/admin/index.vue'
import { usePermission } from '../../../composables/usePermission'
import { deleteById } from '../../../api/admin'
import { fetchRoles, fetchWorkspace, fetchMembers, saveRole, postWorkspace, previewCalibration, type RoleRecord, type RoleWorkspace, type RoleMembers, type Calibration, type GrantCommand } from '../../../api/role-workspace'
import { buildFunctionTree, type FunctionNode } from '../function/tree'
import { resourceKey, grantKey, mergeVisibleChecks } from './state'

type PageChange = { current?: number; pageSize?: number }
type MemberChange = { target: { checked: boolean } }
const { hasPermission } = usePermission()
const canAuthorize = computed(() => hasPermission('authorization:update'))
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const roles = ref<RoleRecord[]>([])
const total = ref(0)
const selectedRoleId = ref('')
const workspace = ref<RoleWorkspace>()
const listLoading = ref(false)
const workspaceLoading = ref(false)
const listError = ref('')
const workspaceError = ref('')
const saving = ref(false)
const funcCodes = ref<string[]>([])
const resourceKeys = ref<string[]>([])
const functionSearch = ref('')
const selectedFunction = ref('')
const expanded = ref<string[]>([])
const resourceType = ref<string | undefined>('BUTTON')
const resourceSearch = ref('')
const resourcePage = ref(1)
const editingOpen = ref(false)
const roleForm = reactive<Partial<RoleRecord>>({})
const roleFormRef = ref<FormInstance>()
const membersOpen = ref(false)
const membersLoading = ref(false)
const membersError = ref('')
const members = ref<RoleMembers>()
const memberCodes = ref<string[]>([])
const memberSearch = ref('')
const defaultOpen = ref(false)
const defaultUrl = ref('')
const calibration = ref<Calibration>()
const calibrationOpen = ref(false)
const calibrating = ref(false)
let listRequest: AbortController | undefined
let workspaceRequest: AbortController | undefined
let memberRequest: AbortController | undefined
let disposed = false
const errorText = (error: unknown) => error instanceof Error ? error.message : '请求失败，请重试'
const same = (a: string[], b: string[]) => JSON.stringify([...a].sort()) === JSON.stringify([...b].sort())
const dirty = computed(() => !!workspace.value && (!same(funcCodes.value, workspace.value.funcCodes) || !same(resourceKeys.value, workspace.value.resourceGrants.map(grantKey))))
const roleColumns = [{ title: '角色名称', dataIndex: 'roleName', ellipsis: true }, { title: '操作', dataIndex: 'actions', width: 190 }]
const resourceColumns = [{ title: '编码', dataIndex: 'resourceCode', ellipsis: true }, { title: '名称', dataIndex: 'resourceName', ellipsis: true }, { title: '授权', dataIndex: 'grant', width: 70 }]
const resourceRows = computed(() => {
  const definitions = (workspace.value?.resources ?? []).map(row => ({ ...row, key: resourceKey(row.funcCode, row.resourceType, row.resourceCode), invalid: false }))
  const known = new Set(definitions.map(row => row.key))
  const orphaned = (workspace.value?.resourceGrants ?? []).filter(row => !known.has(grantKey(row))).map(row => ({ resourceId: grantKey(row), funcCode: row.funcCode, resourceType: row.opType, resourceCode: row.resourceCode, resourceName: `${row.resourceName || row.resourceCode}（已失效）`, key: grantKey(row), invalid: true }))
  return [...definitions, ...orphaned]
})
const filteredResources = computed(() => resourceRows.value.filter(row => row.funcCode === selectedFunction.value && (!resourceType.value || row.resourceType === resourceType.value) && `${row.resourceCode} ${row.resourceName}`.toLowerCase().includes(resourceSearch.value.trim().toLowerCase())))
const rawTree = computed(() => {
  const all = [...(workspace.value?.functions ?? [])]
  const present = new Set(all.map(row => row.funcCode))
  for (const code of funcCodes.value) if (!present.has(code)) all.push({ funcId: `missing:${code}`, funcCode: code, funcName: '已失效功能' })
  return buildFunctionTree(all, functionSearch.value)
})
function treeNodes(nodes: FunctionNode[]): { key: string; title: string; children: ReturnType<typeof treeNodes>; selectable: boolean }[] {
  return nodes.map(node => ({ key: node.record.funcCode, title: `${node.title}${node.anomaly ? '（层级异常）' : ''}`, children: treeNodes(node.children), selectable: true }))
}
const tree = computed(() => treeNodes(rawTree.value))
const visibleFunctions = computed(() => { const all: string[] = []; const visit = (nodes: ReturnType<typeof treeNodes>) => nodes.forEach(node => { all.push(node.key); visit(node.children) }); visit(tree.value); return all })
const userOptions = computed(() => {
  const all = [...(members.value?.users ?? [])]
  const known = new Set(all.map(row => row.userCode))
  for (const code of members.value?.userCodes ?? []) if (!known.has(code)) all.push({ userCode: code, userName: '不可用用户（可移除）' })
  return all.filter(row => `${row.userCode} ${row.userName ?? ''}`.toLowerCase().includes(memberSearch.value.toLowerCase()))
})
const defaultOptions = computed(() => [{ value: '', label: '工作台（默认）' }, ...(workspace.value?.menus ?? []).filter(row => row.menuUrl).map(row => ({ value: row.menuUrl ?? '', label: `${row.menuName} · ${row.menuUrl}` }))])
function confirmDiscard(): Promise<boolean> {
  if (!dirty.value) return Promise.resolve(true)
  return new Promise(resolve => Modal.confirm({ title: '授权尚未保存', content: '继续将放弃当前角色的授权草稿。', okText: '放弃草稿并继续', cancelText: '留在当前角色', onOk: () => resolve(true), onCancel: () => resolve(false) }))
}
async function loadRoles() {
  listRequest?.abort(); const current = new AbortController(); listRequest = current
  listLoading.value = true; listError.value = ''
  try {
    const page = await fetchRoles({ ...query }, current.signal)
    if (disposed || current.signal.aborted) return
    roles.value = page.list ?? page.result ?? []; total.value = page.total ?? page.topTotal ?? 0
    if (!selectedRoleId.value && roles.value[0]) await selectRole(roles.value[0].roleId)
  } catch (error: unknown) { if (!disposed && !current.signal.aborted) { listError.value = errorText(error); roles.value = []; total.value = 0 } }
  finally { if (!disposed && listRequest === current) listLoading.value = false }
}
async function selectRole(id: string, force = false) {
  if (!force && (id === selectedRoleId.value || !(await confirmDiscard()))) return
  workspaceRequest?.abort(); const current = new AbortController(); workspaceRequest = current
  selectedRoleId.value = id; workspace.value = undefined; workspaceLoading.value = true; workspaceError.value = ''
  funcCodes.value = []; resourceKeys.value = []; functionSearch.value = ''; resourceSearch.value = ''
  try {
    const result = await fetchWorkspace(id, current.signal)
    if (disposed || current.signal.aborted) return
    workspace.value = result; funcCodes.value = [...result.funcCodes]; resourceKeys.value = result.resourceGrants.map(grantKey)
    expanded.value = result.functions.map(row => row.funcCode)
    selectedFunction.value = result.resourceGrants[0]?.funcCode ?? result.functions.find(row => row.funcType !== 'CATALOG')?.funcCode ?? result.functions[0]?.funcCode ?? ''
  } catch (error: unknown) { if (!disposed && !current.signal.aborted) workspaceError.value = errorText(error) }
  finally { if (!disposed && workspaceRequest === current) workspaceLoading.value = false }
}
function checkFunctions(keys: (string | number)[] | { checked: (string | number)[] }) {
  if (!canAuthorize.value || saving.value) return
  const checked = (Array.isArray(keys) ? keys : keys.checked).map(String)
  const previous = new Set(funcCodes.value)
  funcCodes.value = mergeVisibleChecks(funcCodes.value, visibleFunctions.value, checked)
  const removed = new Set([...previous].filter(code => !funcCodes.value.includes(code)))
  const removedResources = new Set(resourceRows.value.filter(row => removed.has(row.funcCode)).map(row => row.key))
  resourceKeys.value = resourceKeys.value.filter(key => !removedResources.has(key))
}
function toggleResource(key: string, funcCode: string, checked: boolean) {
  if (!canAuthorize.value || saving.value) return
  if (checked && !funcCodes.value.includes(funcCode)) { message.warning('请先勾选该资源所属功能'); return }
  resourceKeys.value = checked ? [...new Set([...resourceKeys.value, key])] : resourceKeys.value.filter(value => value !== key)
}
function command(): GrantCommand {
  if (!workspace.value) throw new Error('请先选择角色')
  return { roleId: selectedRoleId.value, revision: workspace.value.revision, funcCodes: [...funcCodes.value], resourceKeys: [...resourceKeys.value] }
}
async function saveGrants() {
  if (saving.value || !workspace.value || !canAuthorize.value) return
  saving.value = true
  try { await postWorkspace('saveGrants', command()); if (!disposed) { message.success('角色授权已保存'); await selectRole(selectedRoleId.value, true) } }
  catch (error: unknown) { if (!disposed) message.error(errorText(error)) }
  finally { saving.value = false }
}
async function calibrate() {
  if (!workspace.value || !canAuthorize.value) return
  calibrating.value = true
  try { const result = await previewCalibration(command()); if (!disposed) { calibration.value = result; calibrationOpen.value = true } }
  catch (error: unknown) { if (!disposed) message.error(errorText(error)) }
  finally { calibrating.value = false }
}
function applyCalibration() {
  const result = calibration.value
  if (!result) return
  funcCodes.value = funcCodes.value.filter(code => !result.removedFuncCodes.includes(code))
  resourceKeys.value = resourceKeys.value.filter(key => !result.removedResourceKeys.includes(key))
  calibrationOpen.value = false
  message.success('校准结果已应用到草稿，请点击授权保存')
}
async function editRole(row?: RoleRecord) {
  if (!hasPermission(row ? 'role:update' : 'role:add')) return
  Object.keys(roleForm).forEach(key => delete roleForm[key])
  Object.assign(roleForm, row ?? { roleCode: '', roleName: '', roleType: 'CUSTOM', roleScope: 'ORG', roleDesc: '' })
  editingOpen.value = true; roleFormRef.value?.clearValidate()
}
async function saveRoleForm() {
  if (saving.value) return
  try { await roleFormRef.value?.validate() } catch { return }
  saving.value = true
  try {
    await saveRole({ roleId: roleForm.roleId, roleName: roleForm.roleName, roleDesc: roleForm.roleDesc, roleType: roleForm.roleType, roleScope: roleForm.roleScope })
    if (!disposed) { message.success('角色保存成功'); editingOpen.value = false; if (workspace.value && workspace.value.role.roleId === roleForm.roleId) Object.assign(workspace.value.role, roleForm); await loadRoles() }
  } catch (error: unknown) { if (!disposed) message.error(errorText(error)) }
  finally { saving.value = false }
}
async function removeRole(row: RoleRecord) {
  if (saving.value || !hasPermission('role:delete')) return
  saving.value = true
  try {
    await deleteById('/auth/role/delById', 'roleId', row.roleId)
    if (!disposed) { message.success('角色已删除'); if (row.roleId === selectedRoleId.value) { selectedRoleId.value = ''; workspace.value = undefined }; await loadRoles() }
  } catch (error: unknown) { if (!disposed) message.error(errorText(error)) }
  finally { saving.value = false }
}
async function openMembers(row: RoleRecord) {
  if (row.roleId !== selectedRoleId.value) await selectRole(row.roleId)
  if (row.roleId !== selectedRoleId.value) return
  membersOpen.value = true; membersLoading.value = true; membersError.value = ''; members.value = undefined; memberSearch.value = ''
  memberRequest?.abort(); const current = new AbortController(); memberRequest = current
  try { const result = await fetchMembers(row.roleId, current.signal); if (!disposed && !current.signal.aborted) { members.value = result; memberCodes.value = [...result.userCodes] } }
  catch (error: unknown) { if (!disposed && !current.signal.aborted) membersError.value = errorText(error) }
  finally { if (!disposed && memberRequest === current) membersLoading.value = false }
}
function toggleMember(code: string, checked: boolean) { memberCodes.value = checked ? [...new Set([...memberCodes.value, code])] : memberCodes.value.filter(value => value !== code) }
async function saveMembers() {
  if (!members.value || saving.value || !canAuthorize.value) return
  saving.value = true
  try { await postWorkspace('saveMembers', { roleId: selectedRoleId.value, revision: members.value.revision, userCodes: [...memberCodes.value] }); if (!disposed) { message.success('角色成员已保存'); membersOpen.value = false } }
  catch (error: unknown) { if (!disposed) message.error(errorText(error)) }
  finally { saving.value = false }
}
async function openDefault(row: RoleRecord) {
  if (!(await confirmDiscard())) return
  await selectRole(row.roleId, true)
  if (!workspace.value) return
  defaultUrl.value = workspace.value.role.skipUrl ?? ''; defaultOpen.value = true
}
async function saveDefault() {
  if (!workspace.value || saving.value || !hasPermission('role:update')) return
  saving.value = true
  try { await postWorkspace('saveDefault', { roleId: selectedRoleId.value, revision: workspace.value.revision, skipUrl: defaultUrl.value }); if (!disposed) { message.success('默认菜单已保存'); defaultOpen.value = false; await selectRole(selectedRoleId.value, true) } }
  catch (error: unknown) { if (!disposed) message.error(errorText(error)) }
  finally { saving.value = false }
}
watch([selectedFunction, resourceType, resourceSearch], () => { resourcePage.value = 1 })
watch(functionSearch, () => { expanded.value = workspace.value?.functions.map(row => row.funcCode) ?? [] })
onMounted(loadRoles)
onBeforeRouteLeave(confirmDiscard)
onBeforeUnmount(() => { disposed = true; listRequest?.abort(); workspaceRequest?.abort(); memberRequest?.abort() })
</script>

<template>
  <AdminLayout responsive>
    <div class="role-heading"><h2>角色管理</h2><p>选择角色，维护成员、功能和资源授权。</p></div>
    <a-alert v-if="workspaceError" type="error" :message="workspaceError" show-icon><template #action><a-button @click="selectRole(selectedRoleId, true)">重试</a-button></template></a-alert>
    <div class="role-workspace">
      <a-card class="role-list-panel" title="角色列表" size="small">
        <template #extra><a-button v-if="hasPermission('role:add')" type="primary" size="small" :disabled="saving" @click="editRole()">＋ 新增</a-button></template>
        <a-input-search v-model:value="query.keyword" aria-label="角色关键词" placeholder="输入角色编码或名称" allow-clear @search="query.pageNum = 1; loadRoles()" />
        <a-alert v-if="listError" :message="listError" type="error"><template #action><a-button @click="loadRoles">重试</a-button></template></a-alert>
        <a-table class="role-table" :columns="roleColumns" :data-source="roles" row-key="roleId" :loading="listLoading" size="small" bordered :row-selection="{ type: 'radio', selectedRowKeys: selectedRoleId ? [selectedRoleId] : [], onChange: (keys: (string | number)[]) => { if (!saving) selectRole(String(keys[0])) }, getCheckboxProps: () => ({ disabled: saving }) }" :pagination="{ current: query.pageNum, pageSize: query.pageSize, total, size: 'small', showSizeChanger: false }" @change="(page: PageChange) => { query.pageNum = page.current ?? 1; loadRoles() }">
          <template #emptyText><a-empty :description="listError ? '角色加载失败，请重试' : '没有匹配的角色'" /></template>
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'roleName'"><span :title="record.roleCode">{{ record.roleName }}</span><small class="role-code">{{ record.roleCode }}</small></template>
            <a-space v-else-if="column.dataIndex === 'actions'" :size="0" wrap>
              <a-button v-if="hasPermission('role:update')" type="link" size="small" :disabled="saving" @click="editRole(record)">编辑</a-button>
              <a-button v-if="canAuthorize" type="link" size="small" :disabled="saving" @click="openMembers(record)">用户</a-button>
              <a-button v-if="hasPermission('role:update')" type="link" size="small" :disabled="saving" @click="openDefault(record)">默认菜单</a-button>
              <a-popconfirm v-if="hasPermission('role:delete')" title="确认删除此角色？" description="存在成员或授权时无法删除。" @confirm="removeRole(record)"><a-button type="link" danger size="small" :disabled="saving">删除</a-button></a-popconfirm>
            </a-space>
          </template>
        </a-table>
      </a-card>
      <a-card class="function-grant-panel" :title="workspace ? `功能 · ${workspace.role.roleName}` : '功能'" size="small">
        <template #extra><a-button v-if="canAuthorize" type="primary" size="small" :loading="saving" :disabled="!workspace || !dirty || workspaceLoading" @click="saveGrants">授权</a-button></template>
        <a-input-search v-model:value="functionSearch" aria-label="功能关键词" placeholder="输入功能编码或名称" allow-clear />
        <p class="grant-note">按节点独立授权；选择目录不会自动授权全部子功能。<span v-if="dirty">有未保存的授权</span></p>
        <a-spin :spinning="workspaceLoading"><a-tree v-if="workspace && tree.length" v-model:expanded-keys="expanded" :tree-data="tree" :checked-keys="{ checked: funcCodes, halfChecked: [] }" :selected-keys="selectedFunction ? [selectedFunction] : []" :checkable="canAuthorize" check-strictly block-node :disabled="saving" @check="checkFunctions" @select="(keys: (string | number)[]) => selectedFunction = String(keys[0] ?? '')" /><a-empty v-else :description="workspace ? '没有匹配的功能' : '请先选择角色'" /></a-spin>
      </a-card>
      <a-card class="resource-grant-panel" title="资源列表" size="small">
        <template #extra><a-button v-if="canAuthorize" size="small" :loading="calibrating" :disabled="!workspace || saving || workspaceLoading" @click="calibrate">自动校准</a-button></template>
        <div class="resource-toolbar"><a-select v-model:value="resourceType" aria-label="资源类型" :options="[{ value: 'BUTTON', label: '按钮' }, { value: 'API', label: 'API 接口' }]" placeholder="全部类型" allow-clear /><a-input v-model:value="resourceSearch" aria-label="资源关键词" placeholder="资源编码或名称" allow-clear /></div>
        <p class="grant-note">{{ selectedFunction || '请在功能树中选择节点' }}</p>
        <a-table :columns="resourceColumns" :data-source="filteredResources" row-key="key" :loading="workspaceLoading" size="small" bordered :pagination="{ current: resourcePage, pageSize: 10, size: 'small', showSizeChanger: false }" @change="(page: PageChange) => resourcePage = page.current ?? 1">
          <template #emptyText><a-empty description="此功能下没有匹配的资源" /></template>
          <template #bodyCell="{ column, record }"><a-switch v-if="column.dataIndex === 'grant'" :checked="resourceKeys.includes(record.key)" :disabled="!canAuthorize || saving || (record.invalid && !resourceKeys.includes(record.key))" :aria-label="`授权${record.resourceName}`" @change="(value: boolean | string | number) => toggleResource(record.key, record.funcCode, Boolean(value))" /></template>
        </a-table>
      </a-card>
    </div>
    <a-modal v-model:open="editingOpen" :title="roleForm.roleId ? '编辑角色' : '新增角色'" :confirm-loading="saving" :mask-closable="false" :closable="!saving" :cancel-button-props="{ disabled: saving }" @ok="saveRoleForm">
      <a-form ref="roleFormRef" :model="roleForm" layout="vertical">
        <a-form-item label="角色编码"><a-input :value="roleForm.roleCode" disabled placeholder="保存后自动生成" /></a-form-item>
        <a-form-item label="角色名称" name="roleName" :rules="[{ required: true, whitespace: true, message: '请输入角色名称' }]"><a-input v-model:value="roleForm.roleName" :maxlength="64" /></a-form-item>
        <a-form-item label="角色范围" name="roleScope"><a-input v-model:value="roleForm.roleScope" :maxlength="20" /></a-form-item>
        <a-form-item label="角色类型" name="roleType"><a-input v-model:value="roleForm.roleType" :maxlength="20" /></a-form-item>
        <a-form-item label="描述" name="roleDesc"><a-textarea v-model:value="roleForm.roleDesc" :maxlength="64" /></a-form-item>
      </a-form>
    </a-modal>
    <a-modal v-model:open="membersOpen" title="角色用户" :confirm-loading="saving" :ok-button-props="{ disabled: !members || membersLoading || !canAuthorize }" :mask-closable="false" :closable="!saving" :cancel-button-props="{ disabled: saving }" @ok="saveMembers">
      <a-input-search v-model:value="memberSearch" aria-label="筛选角色用户" placeholder="输入用户编码或名称" allow-clear />
      <a-alert v-if="membersError" :message="membersError" type="error" />
      <a-spin :spinning="membersLoading"><div class="member-list"><a-checkbox v-for="user in userOptions" :key="user.userCode" :checked="memberCodes.includes(user.userCode)" :disabled="saving" @change="(event: MemberChange) => toggleMember(user.userCode, event.target.checked)">{{ user.userName || user.userCode }} · {{ user.userCode }}</a-checkbox><a-empty v-if="!membersLoading && !userOptions.length" description="没有匹配的用户" /></div></a-spin>
    </a-modal>
    <a-modal v-model:open="defaultOpen" title="默认菜单" :confirm-loading="saving" :closable="!saving" :mask-closable="false" :cancel-button-props="{ disabled: saving }" @ok="saveDefault">
      <p>仅单角色登录时生效；多角色用户仍进入工作台。</p><a-select v-model:value="defaultUrl" aria-label="角色默认菜单" :options="defaultOptions" class="default-menu-select" show-search option-filter-prop="label" /><p class="grant-note">可选项来自该角色已保存授权中的有效菜单。</p>
    </a-modal>
    <a-modal v-model:open="calibrationOpen" title="授权校准预览" ok-text="应用到草稿" @ok="applyCalibration">
      <p>只移除失效功能或不再属于已选功能的资源授权，不新增任何权限。</p>
      <p>待移除功能：{{ calibration?.removedFuncCodes.length ?? 0 }}；待移除资源：{{ calibration?.removedResourceKeys.length ?? 0 }}。</p>
      <ul v-if="calibration?.removedFuncCodes.length"><li v-for="code in calibration.removedFuncCodes" :key="code">{{ code }}</li></ul>
      <ul v-if="calibration?.removedResourceKeys.length"><li v-for="key in calibration.removedResourceKeys" :key="key">{{ resourceRows.find(row => row.key === key)?.resourceName || '已失效资源' }}</li></ul>
      <a-alert type="info" message="应用后仍需点击“授权”保存，当前不会修改数据库。" />
    </a-modal>
  </AdminLayout>
</template>
<style scoped>
.role-heading { margin-bottom: 20px; }
.role-heading h2 { margin: 0 0 6px; font-size: 22px; }
.role-heading p { margin: 0; color: var(--color-text-muted); }
.role-workspace { display: grid; grid-template-columns: minmax(340px, 1.1fr) minmax(280px, 1fr) minmax(290px, 1fr); gap: 16px; }
.role-workspace :deep(.ant-card) { min-width: 0; min-height: 560px; }
.role-workspace :deep(.ant-tree) { max-height: 65vh; overflow: auto; }
.role-table { margin-top: 12px; }
.role-table :deep(.ant-btn-link) { padding-inline: 3px; font-size: 12px; }
.role-code { display: block; color: var(--color-text-muted); font-size: 11px; overflow-wrap: anywhere; }
.grant-note { margin: 12px 0; font-size: 12px; color: var(--color-text-muted); overflow-wrap: anywhere; }
.grant-note span { display: block; color: var(--color-accent-strong); }
.resource-toolbar { display: flex; gap: 8px; flex-wrap: wrap; }
.resource-toolbar :deep(.ant-select) { width: 115px; }
.resource-toolbar :deep(.ant-input-affix-wrapper) { flex: 1; min-width: 130px; }
.member-list { display: flex; flex-direction: column; gap: 12px; min-height: 80px; max-height: 420px; overflow: auto; padding: 16px 0; }
.default-menu-select { width: 100%; }
@media (max-width: 1300px) { .role-workspace { grid-template-columns: minmax(0, 1fr); } .role-workspace :deep(.ant-card) { min-height: 0; } }
</style>
