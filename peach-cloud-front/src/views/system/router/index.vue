<script setup lang="ts">
import { onMounted, onBeforeUnmount, reactive, ref } from 'vue'
import { message, type FormInstance } from 'ant-design-vue'
import AdminLayout from '../../../layouts/admin/index.vue'
import { usePermission } from '../../../composables/usePermission'
import { fetchRouters, saveRouter, type RouterRecord } from '../../../api/router'
import { fetchFunctions } from '../../../api/function'
type PageChange = { current?: number; pageSize?: number }
const { hasPermission } = usePermission()
const query = reactive({ pageNum: 1, pageSize: 20, moduleCode: '', routerCode: '', routerUrl: '' })
const rows = ref<RouterRecord[]>([])
const total = ref(0)
const loading = ref(false)
const errorMessage = ref('')
const saving = ref(false)
const open = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<Partial<RouterRecord>>({})
const functions = ref<{ value: string; label: string }[]>([])
const optionsError = ref('')
const componentOptions = Object.keys(import.meta.glob('../../**/index.vue')).map(path => path.replace('../../', '').replace('/index.vue', '')).filter(path => !/^(auth|error)\//.test(path)).sort().map(value => ({ value, label: value }))
const columns = [
  { title: '模块代码', dataIndex: 'moduleCode', width: 110 },
  { title: '路由代码', dataIndex: 'routerCode', width: 210, ellipsis: true },
  { title: '路由名称', dataIndex: 'routerName', width: 170, ellipsis: true },
  { title: '路由 URL', dataIndex: 'routerUrl', width: 230, ellipsis: true },
  { title: '文件路径', dataIndex: 'filePath', width: 240, ellipsis: true },
  { title: '路由级次', dataIndex: 'routerLevel', width: 90 },
  { title: '操作', dataIndex: 'actions', width: 90, fixed: 'right' as const },
]
const fields = [{ name: 'moduleCode', label: '模块代码', max: 10 },  { name: 'routerName', label: '路由名称', max: 100 }, { name: 'routerUrl', label: '路由 URL', max: 100 }]
const rules = Object.fromEntries([...fields, { name: 'filePath', label: '文件路径' }, { name: 'funcCode', label: '关联功能' }].map(field => [field.name, [{ required: true, whitespace: true, message: `请填写${field.label}` }]]))
let request: AbortController | undefined
let optionsRequest: AbortController | undefined
let disposed = false
const errorText = (error: unknown) => error instanceof Error ? error.message : '请求失败，请重试'
async function load() {
  request?.abort(); const current = new AbortController(); request = current
  loading.value = true; errorMessage.value = ''; rows.value = []; total.value = 0
  try { const page = await fetchRouters({ ...query }, current.signal); if (!disposed && !current.signal.aborted) { rows.value = page.list ?? page.result ?? []; total.value = page.total ?? page.topTotal ?? 0 } }
  catch (error: unknown) { if (!disposed && !current.signal.aborted) errorMessage.value = errorText(error) }
  finally { if (!disposed && request === current) loading.value = false }
}
async function loadOptions() {
  optionsRequest?.abort(); const current = new AbortController(); optionsRequest = current; optionsError.value = ''
  try { const items = await fetchFunctions(current.signal); if (!disposed && !current.signal.aborted) functions.value = items.map(row => ({ value: row.funcCode, label: `${row.funcCode} ${row.funcName}` })) }
  catch (error: unknown) { if (!disposed && !current.signal.aborted) optionsError.value = errorText(error) }
}
function search() { query.pageNum = 1; void load() }
function reset() { Object.assign(query, { moduleCode: '', routerCode: '', routerUrl: '', pageNum: 1 }); void load() }
function edit(record?: RouterRecord) {
  if (!hasPermission(record ? 'router:update' : 'router:add')) return
  Object.keys(form).forEach(key => delete form[key])
  Object.assign(form, record ?? { moduleCode: 'system', routerLevel: 2 })
  open.value = true; formRef.value?.clearValidate()
}
async function save() {
  if (saving.value || !hasPermission(form.routerId ? 'router:update' : 'router:add')) return
  try { await formRef.value?.validate() } catch { return }
  saving.value = true
  try {
    await saveRouter({ routerId: form.routerId, moduleCode: form.moduleCode, routerName: form.routerName, routerUrl: form.routerUrl, filePath: form.filePath, routerLevel: form.routerLevel, funcCode: form.funcCode })
    if (!disposed) { message.success('路由保存成功'); open.value = false; await load() }
  } catch (error: unknown) { if (!disposed) message.error(errorText(error)) }
  finally { saving.value = false }
}
onMounted(() => { void load(); void loadOptions() })
onBeforeUnmount(() => { disposed = true; request?.abort(); optionsRequest?.abort() })
</script>

<template>
  <AdminLayout responsive>
    <a-card class="admin-page-card">
      <template #title><div class="page-heading"><div><h2>路由列表</h2><p>维护页面地址、组件路径和关联功能。</p></div><a-button v-if="hasPermission('router:add')" type="primary" @click="edit()">＋ 新增</a-button></div></template>
      <a-form name="router-query" :model="query" layout="inline" class="route-filters" @finish="search">
        <a-form-item label="模块代码" name="moduleCode"><a-input v-model:value="query.moduleCode" placeholder="输入模块代码" allow-clear /></a-form-item>
        <a-form-item label="路由代码" name="routerCode"><a-input v-model:value="query.routerCode" placeholder="输入路由代码" allow-clear /></a-form-item>
        <a-form-item label="路由 URL" name="routerUrl"><a-input v-model:value="query.routerUrl" placeholder="输入路由 URL" allow-clear /></a-form-item>
        <a-form-item><a-space><a-button html-type="submit" type="primary" :loading="loading">查询</a-button><a-button @click="reset">重置</a-button></a-space></a-form-item>
      </a-form>
      <a-alert v-if="errorMessage" :message="errorMessage" type="error" show-icon><template #action><a-button size="small" @click="load">重试</a-button></template></a-alert>
      <a-table :columns="columns" :data-source="rows" row-key="routerId" :loading="loading" size="small" bordered :scroll="{ x: 1140 }" :pagination="{ current: query.pageNum, pageSize: query.pageSize, total, showSizeChanger: true }" @change="(page: PageChange) => { query.pageNum = page.current ?? 1; query.pageSize = page.pageSize ?? 20; load() }">
        <template #emptyText><a-empty :description="errorMessage ? '路由加载失败，请重试' : '暂无匹配路由，可调整筛选或新增路由'" /></template>
        <template #bodyCell="{ column, record }"><a-button v-if="column.dataIndex === 'actions' && hasPermission('router:update')" type="link" @click="edit(record)">修改</a-button></template>
      </a-table>
    </a-card>
    <a-modal v-model:open="open" :title="form.routerId ? '修改路由' : '新增路由'" :confirm-loading="saving" :mask-closable="false" :closable="!saving" :cancel-button-props="{ disabled: saving }" @ok="save">
      <a-form name="router-editor" ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="路由代码"><a-input :value="form.routerCode" disabled :placeholder="form.routerId ? '历史编码' : '保存后自动生成'" /></a-form-item>
        <a-form-item v-for="field in fields" :key="field.name" :label="field.label" :name="field.name"><a-input v-model:value="form[field.name] as string | undefined" :maxlength="field.max" /></a-form-item>
        <a-form-item label="文件路径" name="filePath"><a-select v-model:value="form.filePath" :options="componentOptions" show-search option-filter-prop="label" placeholder="选择已有页面组件" /></a-form-item>
        <a-form-item label="关联功能" name="funcCode"><a-select v-model:value="form.funcCode" :options="functions" show-search option-filter-prop="label" placeholder="选择路由所属功能" /></a-form-item>
        <a-alert v-if="optionsError" :message="optionsError" type="error"><template #action><a-button size="small" @click="loadOptions">重试</a-button></template></a-alert>
        <a-form-item label="路由级次" name="routerLevel"><a-input-number v-model:value="form.routerLevel" :min="1" :max="20" /></a-form-item>
      </a-form>
    </a-modal>
  </AdminLayout>
</template>
<style scoped>
.route-filters { gap: 12px 0; margin-bottom: 20px; }
</style>
