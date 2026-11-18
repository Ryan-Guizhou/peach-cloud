<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'

import {
  fetchCurrentDataPolicies,
  fetchCurrentFieldPolicies,
  fetchDelegableFieldCatalog,
  replaceDataPolicies,
  replaceFieldPolicies,
} from '../../../api/saas-iam'
import type {
  SaasCatalogEntry,
  DataScopeRule,
  FieldPolicyRule,
  DataScopeType,
  ReadEffect,
  WriteEffect,
} from '../../../api/saas-iam'

const props = defineProps<{
  roleRefId: string
  permissionRefs: string[]
  permissionCatalog: SaasCatalogEntry[]
  organizationCatalog: SaasCatalogEntry[]
  permissionsComplete: boolean
  organizationsComplete: boolean
  disabled: boolean
}>()

const emit = defineEmits<{
  (event: 'committed'): void
}>()

const selectedPermissionId = ref('')
const dataRules = ref<DataScopeRule[]>([])
const fieldRules = ref<FieldPolicyRule[]>([])
const availableFields = ref<SaasCatalogEntry[]>([])
const currentFieldPage = ref(0)
const fieldsHasMore = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const saving = ref(false)
const dataLoaded = ref(false)
const fieldLoaded = ref(false)
let currentLoad = 0

const availableActions = computed(() => props.permissionCatalog
  .filter(item => props.permissionRefs.includes(item.id)))
const actionOptions = computed(() => availableActions.value.map(item => ({
  label: `${item.name || item.code || item.id} (${item.code || '动作'})`,
  value: item.id,
})))
const currentAction = computed(() => availableActions.value
  .find(item => item.id === selectedPermissionId.value))
const resourceCode = computed(() => {
  const code = currentAction.value?.code || ''
  if (currentAction.value?.resourceType === 'API'
    || !/^[A-Z][A-Z0-9_]{0,63}\.[A-Z][A-Z0-9_.-]{0,63}$/.test(code)) {
    return ''
  }
  return code.substring(0, code.indexOf('.'))
})
const fieldOptions = computed(() => availableFields.value.map(item => ({
  label: `${item.code || item.id} (${item.name || '资源'}，脱敏：${item.resourceType || 'FULL'})`,
  value: item.id,
})))
const orgOptions = computed(() => props.organizationCatalog.map(item => ({
  label: `${item.name || item.code || item.id} (${item.code || ''})`,
  value: item.id,
})))
const dataScopeOptions: { label: string; value: DataScopeType }[] = [
  { label: '仅本人数据', value: 'SELF' },
  { label: '指定机构', value: 'ORG' },
  { label: '指定机构及下级', value: 'ORG_TREE' },
  { label: '整个租户', value: 'TENANT_ALL' },
]
const readEffects: { label: string; value: ReadEffect }[] = [
  { label: '允许查看', value: 'ALLOW' },
  { label: '服务端脱敏', value: 'MASK' },
  { label: '拒绝查看', value: 'DENY' },
]
const writeEffects: { label: string; value: WriteEffect }[] = [
  { label: '允许更新', value: 'ALLOW' },
  { label: '禁止更新', value: 'DENY' },
]

const visibleFieldIds = computed(() => new Set(availableFields.value.map(item => item.id)))
const invisibleExistingFields = computed(() =>
  fieldRules.value.filter(item => !visibleFieldIds.value.has(item.fieldRefId)))
const validDataRules = computed(() => {
  if (dataRules.value.length > 32 || !props.organizationsComplete) return false
  const unique = new Set<string>()
  return dataRules.value.every(rule => {
    if (!dataScopeOptions.some(item => item.value === rule.dataScope)) return false
    const needsOrg = rule.dataScope === 'ORG' || rule.dataScope === 'ORG_TREE'
    const orgId = needsOrg ? rule.orgRefId || '' : ''
    if (needsOrg && !orgOptions.value.some(o => o.value === orgId)) return false
    if (!needsOrg && rule.orgRefId) return false
    const key = `${rule.dataScope}:${orgId}`
    if (unique.has(key)) return false
    unique.add(key)
    return true
  })
})
const validFieldRules = computed(() => {
  if (fieldRules.value.length > 128) return false
  const unique = new Set<string>()
  return fieldRules.value.every(rule => {
    if (!visibleFieldIds.value.has(rule.fieldRefId)) return false
    if (!readEffects.some(effect => effect.value === rule.readEffect)
      || !writeEffects.some(effect => effect.value === rule.writeEffect)) return false
    if (unique.has(rule.fieldRefId)) return false
    unique.add(rule.fieldRefId)
    return true
  })
})
const canSaveData = computed(() => !props.disabled && !saving.value && !loading.value
  && Boolean(selectedPermissionId.value) && dataLoaded.value
  && props.permissionsComplete && validDataRules.value)
const canSaveFields = computed(() => canSaveData.value && fieldLoaded.value
  && Boolean(resourceCode.value) && !fieldsHasMore.value
  && invisibleExistingFields.value.length === 0 && validFieldRules.value)

watch(() => props.roleRefId, () => {
  selectedPermissionId.value = ''
  reset()
})
watch(() => props.permissionRefs.join(','), () => {
  if (!props.permissionRefs.includes(selectedPermissionId.value)) {
    selectedPermissionId.value = ''
    reset()
  }
})
watch(selectedPermissionId, () => {
  void loadCurrent()
})

function reset() {
  currentLoad += 1
  dataRules.value = []
  fieldRules.value = []
  availableFields.value = []
  currentFieldPage.value = 0
  fieldsHasMore.value = false
  dataLoaded.value = false
  fieldLoaded.value = false
  loading.value = false
}

async function loadCurrent() {
  reset()
  if (!props.roleRefId || !selectedPermissionId.value || props.disabled) return
  const version = ++currentLoad
  const action = resourceCode.value
  loading.value = true
  try {
    const rows = await fetchCurrentDataPolicies(props.roleRefId, selectedPermissionId.value)
    let existingFields: FieldPolicyRule[] = []
    let directory: SaasCatalogEntry[] = []
    let fieldHasMore = false
    if (action) {
      const results = await Promise.all([
        fetchCurrentFieldPolicies(props.roleRefId, selectedPermissionId.value, action),
        fetchDelegableFieldCatalog(action, 1, 50),
      ])
      existingFields = results[0]
      directory = results[1].items
      fieldHasMore = results[1].hasMore
    }
    if (version !== currentLoad) return
    dataRules.value = rows.map(row => ({
      dataScope: row.dataScope,
      orgRefId: row.orgRefId,
    }))
    fieldRules.value = existingFields.map(row => ({ ...row }))
    availableFields.value = directory
    currentFieldPage.value = action ? 1 : 0
    fieldsHasMore.value = fieldHasMore
    dataLoaded.value = true
    fieldLoaded.value = Boolean(action)
  } catch (error: unknown) {
    if (version === currentLoad) {
      message.error(error instanceof Error ? error.message : '未能完整读取当前策略，禁止修改')
    }
  } finally {
    if (version === currentLoad) loading.value = false
  }
}

async function loadMoreFields() {
  if (loadingMore.value || !fieldsHasMore.value || !resourceCode.value) return
  const version = currentLoad
  loadingMore.value = true
  try {
    const result = await fetchDelegableFieldCatalog(resourceCode.value, currentFieldPage.value + 1)
    if (version !== currentLoad) return
    const mapped = new Map(availableFields.value.map(item => [item.id, item]))
    result.items.forEach(item => mapped.set(item.id, item))
    availableFields.value = [...mapped.values()]
    currentFieldPage.value += 1
    fieldsHasMore.value = result.hasMore
  } catch (error: unknown) {
    if (version === currentLoad) {
      message.error(error instanceof Error ? error.message : '字段目录尚未完整加载')
    }
  } finally {
    loadingMore.value = false
  }
}

function addData() {
  if (dataRules.value.length >= 32) return
  dataRules.value.push({ dataScope: 'SELF' })
}
function removeData(index: number) {
  dataRules.value.splice(index, 1)
}
function addField() {
  if (fieldRules.value.length >= 128) return
  fieldRules.value.push({ fieldRefId: '', readEffect: 'DENY', writeEffect: 'DENY' })
}
function removeField(index: number) {
  fieldRules.value.splice(index, 1)
}

async function saveData() {
  if (!canSaveData.value) return
  saving.value = true
  try {
    await replaceDataPolicies(props.roleRefId, selectedPermissionId.value,
      dataRules.value.map(rule => ({
        dataScope: rule.dataScope,
        orgRefId: rule.dataScope === 'ORG' || rule.dataScope === 'ORG_TREE'
          ? rule.orgRefId : undefined,
      })))
    message.success('数据范围保存成功，旧登录会话已失效')
    emit('committed')
  } catch (error: unknown) {
    message.error(error instanceof Error ? error.message : '保存数据范围失败')
  } finally {
    saving.value = false
  }
}

async function saveFields() {
  if (!canSaveFields.value) return
  saving.value = true
  try {
    await replaceFieldPolicies(props.roleRefId, selectedPermissionId.value,
      resourceCode.value, fieldRules.value.map(rule => ({ ...rule })))
    message.success('字段策略保存成功，旧登录会话已失效')
    emit('committed')
  } catch (error: unknown) {
    message.error(error instanceof Error ? error.message : '保存字段策略失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section>
    <a-form layout="vertical">
      <a-form-item label="当前角色的目标动作权限">
        <a-select
          v-model:value="selectedPermissionId"
          :options="actionOptions"
          :disabled="disabled || !roleRefId || !permissionsComplete"
          show-search
          option-filter-prop="label"
          placeholder="先选择角色，再从该角色已授权的动作中选择"
        />
      </a-form-item>
      <a-alert
        v-if="selectedPermissionId && !dataLoaded && !loading"
        type="warning"
        show-icon
        message="策略尚未完整读取，禁止替换"
      />
      <template v-if="selectedPermissionId">
        <a-divider orientation="left">当前动作的数据范围策略</a-divider>
        <a-spin :spinning="loading">
          <div v-for="(rule, index) in dataRules" :key="index" style="display: flex; gap: 12px; margin-bottom: 12px">
            <a-select
              v-model:value="rule.dataScope"
              :options="dataScopeOptions"
              style="width: 210px"
              :disabled="disabled || !dataLoaded"
              @change="rule.orgRefId = undefined"
            />
            <a-select
              v-if="rule.dataScope === 'ORG' || rule.dataScope === 'ORG_TREE'"
              v-model:value="rule.orgRefId"
              :options="orgOptions"
              style="width: 260px"
              :disabled="disabled || !dataLoaded || !organizationsComplete"
              show-search
              option-filter-prop="label"
              placeholder="选择受信机构"
            />
            <a-button danger :disabled="disabled || !dataLoaded" @click="removeData(index)">删除</a-button>
          </div>
          <a-space>
            <a-button :disabled="disabled || !dataLoaded || dataRules.length >= 32" @click="addData">
              添加数据范围
            </a-button>
            <a-button type="primary" :loading="saving" :disabled="!canSaveData" @click="saveData">
              全量保存数据范围
            </a-button>
          </a-space>
        </a-spin>

        <a-divider orientation="left">当前动作的字段查看与更新策略</a-divider>
        <a-alert
          v-if="!resourceCode"
          type="info"
          show-icon
          message="该动作未登记可解析的资源类型，不能编辑字段权限"
          description="例如 DOC.VIEW 对应资源 DOC，API 路由动作不会自动产生可委派字段。"
        />
        <template v-else>
          <p>当前资源：{{ resourceCode }}。只有受信字段目录完整且原有策略全部可见时才允许替换。</p>
          <a-alert
            v-if="fieldsHasMore || invisibleExistingFields.length > 0"
            type="warning"
            show-icon
            message="字段目录不完整，禁止全量覆盖已有策略"
            style="margin-bottom: 12px"
          />
          <div v-for="(rule, index) in fieldRules" :key="index" style="display: flex; gap: 12px; margin-bottom: 12px">
            <a-select
              v-model:value="rule.fieldRefId"
              :options="fieldOptions"
              style="width: 310px"
              :disabled="disabled || !fieldLoaded || fieldsHasMore"
              show-search
              option-filter-prop="label"
              placeholder="选择受信字段"
            />
            <a-select
              v-model:value="rule.readEffect"
              :options="readEffects"
              style="width: 140px"
              :disabled="disabled || !fieldLoaded"
            />
            <a-select
              v-model:value="rule.writeEffect"
              :options="writeEffects"
              style="width: 140px"
              :disabled="disabled || !fieldLoaded"
            />
            <a-button danger :disabled="disabled || !fieldLoaded" @click="removeField(index)">删除</a-button>
          </div>
          <a-space>
            <a-button
              v-if="fieldsHasMore"
              :loading="loadingMore"
              :disabled="disabled || loadingMore"
              @click="loadMoreFields"
            >加载更多受控字段</a-button>
            <a-button
              :disabled="disabled || !fieldLoaded || fieldsHasMore || fieldRules.length >= 128"
              @click="addField"
            >添加字段策略</a-button>
            <a-button
              type="primary"
              :loading="saving"
              :disabled="!canSaveFields"
              @click="saveFields"
            >全量保存字段权限</a-button>
          </a-space>
        </template>
      </template>
    </a-form>
    <a-alert
      type="info"
      show-icon
      style="margin-top: 16px"
      message="写入成功后需要重新登录"
      description="权限写入会递增整个租户的授权版本。写入成功会立即清空当前 Token、旧动态路由及 WebSocket 会话。行级策略约束必须由业务 SQL 强制执行，不能仅凭页面设置认定生效。"
    />
  </section>
</template>
