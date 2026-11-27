<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { message, type FormInstance } from 'ant-design-vue'
import AdminLayout from '../../../layouts/admin/index.vue'
import { deleteById, postRecord, type DataRecord } from '../../../api/admin'
import { fetchFunctions, fetchResources, type FunctionDefinition, type FunctionResource } from '../../../api/function'
import { usePermission } from '../../../composables/usePermission'
import { buildFunctionTree } from './tree'

const { hasPermission } = usePermission()
const functions = ref<FunctionDefinition[]>([])
const resources = ref<FunctionResource[]>([])
const selectedId = ref('')
const selected = computed(() => functions.value.find(row => row.funcId === selectedId.value))
const treeKeyword = ref('')
const tree = computed(() => buildFunctionTree(functions.value, treeKeyword.value))
const expandedKeys = ref<string[]>([])
const treeLoading = ref(false)
const resourceLoading = ref(false)
const treeError = ref('')
const resourceError = ref('')
const query = reactive({ resourceType: undefined as string | undefined, keyword: '', pageNum: 1, pageSize: 10 })
const total = ref(0)
const modalOpen = ref(false)
const editorKind = ref<'function' | 'resource'>('function')
const editingId = ref('')
const saving = ref(false)
const deletingId = ref('')
const formRef = ref<FormInstance>()
const form = reactive<DataRecord>({})
const resourceTypes = [{ value: 'BUTTON', label: '按钮' }, { value: 'API', label: 'API 接口' }]
const columns = [
  { title: '编码', dataIndex: 'resourceCode', ellipsis: true },
  { title: '名称', dataIndex: 'resourceName', ellipsis: true },
  { title: '类型', dataIndex: 'resourceType', width: 120 },
  { title: '操作', dataIndex: 'actions', width: 150 },
]
const functionFields = [
  { name: 'funcCode', label: '功能编码', required: false, max: 32 },
  { name: 'funcName', label: '功能名称', required: true, max: 32 },
  { name: 'parentFuncCode', label: '上级功能', max: 32 },
  { name: 'funcSeq', label: '排序号', max: 100 },
  { name: 'funcType', label: '功能类型', max: 20 },
  { name: 'funcUrl', label: '功能URL', max: 128 },
  { name: 'funcDesc', label: '功能描述', max: 32 },
]
const resourceFields = [
  { name: 'funcCode', label: '所属功能', required: true, max: 32 },
  { name: 'resourceCode', label: '资源编码', required: true, max: 64 },
  { name: 'resourceName', label: '资源名称', required: true, max: 32 },
  { name: 'resourceType', label: '资源类型', required: true, max: 20 },
]
const fields = computed(() => editorKind.value === 'function' ? functionFields : resourceFields)
const modalTitle = computed(() => `${editingId.value ? '修改' : '新增'}${editorKind.value === 'function' ? '功能' : '资源'}`)
const rules = computed(() => Object.fromEntries(fields.value.filter(field => field.required).map(field => [field.name, [{ required: true, whitespace: true, message: `请填写${field.label}` }]])))
const parentOptions = computed(() => functions.value
  .filter(row => row.funcId !== editingId.value)
  .map(row => ({ value: row.funcCode, label: `${row.funcCode} ${row.funcName}` })))
let treeRequest: AbortController | undefined
let resourceRequest: AbortController | undefined
let disposed = false
const errorText = (error: unknown) => error instanceof Error ? error.message : '请求失败，请重试'
const can = (kind: 'function' | 'resource', action: string) => hasPermission(`${kind}:${action}`)

async function loadTree() {
  treeRequest?.abort()
  const request = new AbortController()
  treeRequest = request
  treeLoading.value = true
  treeError.value = ''
  try {
    const rows = await fetchFunctions(request.signal)
    if (disposed || request.signal.aborted) return
    functions.value = rows
    expandedKeys.value = rows.map(row => row.funcId)
    if (!rows.some(row => row.funcId === selectedId.value)) selectedId.value = rows[0]?.funcId ?? ''
  } catch (error: unknown) {
    if (!disposed && !request.signal.aborted) {
      functions.value = []
      selectedId.value = ''
      treeError.value = errorText(error)
    }
  } finally {
    if (!disposed && treeRequest === request) treeLoading.value = false
  }
}
async function loadResources() {
  resourceRequest?.abort()
  const request = new AbortController()
  resourceRequest = request
  resources.value = []
  total.value = 0
  resourceError.value = ''
  const owner = selected.value
  if (!owner || !can('resource', 'view')) { resourceLoading.value = false; return }
  resourceLoading.value = true
  try {
    const page = await fetchResources({ ...query, funcCode: owner.funcCode }, request.signal)
    if (disposed || request.signal.aborted) return
    resources.value = page.list ?? page.result ?? []
    total.value = page.total ?? page.topTotal ?? 0
  } catch (error: unknown) {
    if (!disposed && !request.signal.aborted) resourceError.value = errorText(error)
  } finally {
    if (!disposed && resourceRequest === request) resourceLoading.value = false
  }
}
watch(selectedId, () => { query.pageNum = 1; void loadResources() })
watch(treeKeyword, () => { expandedKeys.value = functions.value.map(row => row.funcId) })
function selectNode(keys: (string | number)[]) { selectedId.value = String(keys[0] ?? '') }
function searchResources() { query.pageNum = 1; void loadResources() }
function changePage(page: { current?: number; pageSize?: number }) {
  query.pageNum = page.current ?? 1
  query.pageSize = page.pageSize ?? 10
  void loadResources()
}
function openEditor(kind: 'function' | 'resource', record?: FunctionDefinition | FunctionResource, parent?: FunctionDefinition) {
  if (!can(kind, record ? 'update' : 'add')) return
  if (kind === 'resource' && !selected.value) return
  editorKind.value = kind
  editingId.value = record ? String(kind === 'function' ? record.funcId : record.resourceId) : ''
  Object.keys(form).forEach(key => delete form[key])
  Object.assign(form, record ?? (kind === 'function'
    ? { parentFuncCode: parent?.funcCode ?? '', funcType: 'MENU', isMenu: 0, isAuthorize: 1, isDisable: 0 }
    : { funcCode: selected.value?.funcCode, resourceType: 'BUTTON' }))
  modalOpen.value = true
  formRef.value?.clearValidate()
}
async function save() {
  if (saving.value || !can(editorKind.value, editingId.value ? 'update' : 'add')) return
  try { await formRef.value?.validate() } catch { return }
  saving.value = true
  try {
    const kind = editorKind.value
    const idField = kind === 'function' ? 'funcId' : 'resourceId'
    const payload = { ...form }
    if (kind === 'function') delete payload.funcCode
    if (editingId.value) payload[idField] = editingId.value
    await postRecord(`/auth/${kind}/${editingId.value ? 'update' : 'add'}`, payload)
    if (disposed) return
    message.success('保存成功')
    modalOpen.value = false
    if (kind === 'function') await loadTree()
    else await loadResources()
  } catch (error: unknown) { if (!disposed) message.error(errorText(error)) }
  finally { saving.value = false }
}
async function remove(kind: 'function' | 'resource', id: string) {
  if (deletingId.value || !can(kind, 'delete')) return
  deletingId.value = id
  try {
    await deleteById(`/auth/${kind}/delById`, kind === 'function' ? 'funcId' : 'resourceId', id)
    if (disposed) return
    message.success('删除成功')
    if (kind === 'function') await loadTree()
    else { if (resources.value.length === 1 && query.pageNum > 1) query.pageNum--; await loadResources() }
  } catch (error: unknown) { if (!disposed) message.error(errorText(error)) }
  finally { deletingId.value = '' }
}
onMounted(loadTree)
onBeforeUnmount(() => { disposed = true; treeRequest?.abort(); resourceRequest?.abort() })
</script>

<template>
  <AdminLayout responsive>
    <div class="definition-heading"><h2>功能定义</h2><p>维护功能层级及对应的按钮、API 资源。</p></div>
    <div class="definition-workspace">
      <a-card class="function-panel" title="功能">
        <template #extra><a-space><a-button size="small" :loading="treeLoading" @click="loadTree">刷新</a-button><a-button v-if="can('function', 'add')" type="primary" size="small" @click="openEditor('function')">新增根功能</a-button></a-space></template>
        <a-input-search v-model:value="treeKeyword" aria-label="筛选功能" placeholder="输入功能编码或名称" allow-clear />
        <a-alert v-if="treeError" type="error" :message="treeError" show-icon><template #action><a-button size="small" @click="loadTree">刷新</a-button></template></a-alert>
        <a-spin :spinning="treeLoading">
          <a-tree v-if="tree.length" v-model:expanded-keys="expandedKeys" class="function-tree" :tree-data="tree" :selected-keys="selectedId ? [selectedId] : []" block-node @select="selectNode">
            <template #title="node">
              <div class="function-node">
                <span class="function-label" :title="node.title">{{ node.title }} <a-tag v-if="node.anomaly" color="warning">{{ node.anomaly }}</a-tag></span>
                <a-space size="small" class="node-actions" @click.stop>
                  <a-button v-if="can('function', 'add')" type="link" size="small" :aria-label="`为${node.record.funcName}新增下级`" @click="openEditor('function', undefined, node.record)">新增下级</a-button>
                  <a-button v-if="can('function', 'update')" type="link" size="small" @click="openEditor('function', node.record)">修改</a-button>
                  <a-popconfirm v-if="can('function', 'delete')" title="确认删除此功能？" description="存在下级或关联引用时无法删除。" @confirm="remove('function', node.record.funcId)"><a-button type="link" danger size="small" :disabled="!!deletingId">删除</a-button></a-popconfirm>
                </a-space>
              </div>
            </template>
          </a-tree>
          <a-empty v-else-if="!treeError" :description="treeKeyword ? '没有匹配的功能，请调整关键词' : '暂无功能，可新增根功能'" />
        </a-spin>
      </a-card>
      <a-card class="resource-panel">
        <template #title><span>{{ selected ? `${selected.funcName} · 资源信息` : '资源信息' }}</span></template>
        <template #extra><a-tag v-if="selected">{{ selected.funcCode }}</a-tag></template>
        <a-empty v-if="!selected" description="请先在左侧选择功能" />
        <a-result v-else-if="!can('resource', 'view')" status="403" title="暂无资源查看权限" />
        <template v-else>
          <a-form :model="query" layout="inline" class="resource-filters" @finish="searchResources">
            <a-form-item label="资源类型"><a-select v-model:value="query.resourceType" aria-label="资源类型" :options="resourceTypes" placeholder="全部类型" allow-clear class="type-filter" /></a-form-item>
            <a-form-item label="编码/名称"><a-input v-model:value="query.keyword" aria-label="资源编码或名称" placeholder="输入资源编码或名称" allow-clear /></a-form-item>
            <a-form-item><a-space><a-button html-type="submit" :loading="resourceLoading">查询</a-button><a-button @click="query.keyword = ''; query.resourceType = undefined; searchResources()">重置</a-button></a-space></a-form-item>
            <a-form-item class="resource-create"><a-button v-if="can('resource', 'add')" type="primary" @click="openEditor('resource')">＋ 新增资源</a-button></a-form-item>
          </a-form>
          <a-alert v-if="resourceError" type="error" :message="resourceError" show-icon><template #action><a-button size="small" @click="loadResources">重试</a-button></template></a-alert>
          <a-table :columns="columns" :data-source="resources" row-key="resourceId" :loading="resourceLoading" size="small" bordered :scroll="{ x: 600 }" :pagination="{ current: query.pageNum, pageSize: query.pageSize, total, showSizeChanger: true }" @change="changePage">
            <template #emptyText><a-empty :description="resourceError ? '资源加载失败，请重试' : '暂无资源，请新增资源或调整筛选条件'" /></template>
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'resourceType'">{{ resourceTypes.find(type => type.value === record.resourceType)?.label ?? record.resourceType }}</template>
              <a-space v-else-if="column.dataIndex === 'actions'">
                <a-button v-if="can('resource', 'update')" type="link" size="small" @click="openEditor('resource', record)">修改</a-button>
                <a-popconfirm v-if="can('resource', 'delete')" title="确认删除此资源？" description="已有授权引用时需先解除授权。" @confirm="remove('resource', record.resourceId)"><a-button type="link" danger size="small" :disabled="!!deletingId">删除</a-button></a-popconfirm>
              </a-space>
            </template>
          </a-table>
        </template>
      </a-card>
    </div>
    <a-modal v-model:open="modalOpen" :title="modalTitle" :confirm-loading="saving" :closable="!saving" :mask-closable="false" :keyboard="!saving" :cancel-button-props="{ disabled: saving }" @ok="save">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item v-for="field in fields" :key="field.name" :name="field.name" :label="field.label" :required="field.required">
          <a-select v-if="field.name === 'resourceType'" v-model:value="form[field.name] as string | undefined" :options="resourceTypes" />
          <a-select v-else-if="field.name === 'parentFuncCode'" v-model:value="form[field.name] as string | undefined" :options="parentOptions" show-search allow-clear option-filter-prop="label" placeholder="不选择则为根功能" @clear="form.parentFuncCode = ''" />
          <a-input v-else v-model:value="form[field.name] as string | undefined" :maxlength="field.max" :placeholder="editorKind === 'function' && field.name === 'funcCode' ? '保存后自动生成' : undefined" :disabled="field.name === 'funcCode'" />
        </a-form-item>
        <template v-if="editorKind === 'function'">
          <a-form-item label="是否菜单"><a-switch :checked="form.isMenu === 1" @change="(value: boolean | string | number) => form.isMenu = value ? 1 : 0" /></a-form-item>
          <a-form-item label="需要授权"><a-switch :checked="form.isAuthorize === 1" @change="(value: boolean | string | number) => form.isAuthorize = value ? 1 : 0" /></a-form-item>
          <a-form-item label="禁用"><a-switch :checked="form.isDisable === 1" @change="(value: boolean | string | number) => form.isDisable = value ? 1 : 0" /></a-form-item>
        </template>
      </a-form>
    </a-modal>
  </AdminLayout>
</template>

<style scoped>
.definition-heading { margin-bottom: 20px; }
.definition-heading h2 { margin: 0 0 6px; font-size: 22px; }
.definition-heading p { margin: 0; color: var(--color-text-muted); }
.definition-workspace { display: grid; grid-template-columns: minmax(380px, 36%) minmax(0, 1fr); gap: 20px; align-items: stretch; }
.function-panel, .resource-panel { min-width: 0; min-height: 560px; }
.function-tree { margin-top: 16px; max-height: 65vh; overflow: auto; }
.function-node { display: flex; align-items: center; gap: 8px; min-width: 0; }
.function-label { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; }
.node-actions { flex-shrink: 0; }
.node-actions :deep(.ant-btn) { padding-inline: 2px; font-size: 12px; }
.function-tree :deep(.ant-tree-node-content-wrapper) { min-width: 0; }
.resource-filters { gap: 12px 0; margin-bottom: 16px; }
.type-filter { width: 130px; }
.resource-create { margin-inline-start: auto; margin-inline-end: 0; }
@media (max-width: 1100px) {
  .definition-workspace { grid-template-columns: 1fr; }
  .function-panel, .resource-panel { min-height: 0; }
  .function-tree { max-height: 340px; }
}
@media (max-width: 640px) {
  .resource-panel :deep(.ant-card-head-wrapper) { flex-wrap: wrap; gap: 8px; padding-block: 12px; }
}
@media (max-width: 560px) {
  .function-node { flex-wrap: wrap; }
  .function-label { flex-basis: 100%; }
  .resource-create { margin-inline-start: 0; }
  .definition-workspace { gap: 12px; }
}
</style>
