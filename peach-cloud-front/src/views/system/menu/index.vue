<script setup lang="ts">
import { onMounted, ref } from 'vue'
import CrudPage from '../../../components/admin/CrudPage.vue'
import { fetchFunctions } from '../../../api/function'

const functionOptions = ref<Array<{ value: string; label: string }>>([])
const functionError = ref('')
async function loadFunctions() {
  functionError.value = ''
  try {
    functionOptions.value = (await fetchFunctions()).map(row => ({ value: row.funcCode, label: `${row.funcCode} ${row.funcName}` }))
  } catch (error: unknown) {
    functionError.value = error instanceof Error ? error.message : '功能列表加载失败'
  }
}
onMounted(loadFunctions)
</script>

<template>
  <CrudPage
    title="菜单管理"
    description="维护后端登录返回的菜单数据，前端按菜单 URL 组装导航。"
    row-key="menuId"
    permission-prefix="menu"
    :endpoint="{ pageList: '/auth/menu/pageList', add: '/auth/menu/add', update: '/auth/menu/update', delById: '/auth/menu/delById', idField: 'menuId' }"
    :columns="[
      { title: '菜单ID', dataIndex: 'menuId', width: 180 },
      { title: '菜单编码', dataIndex: 'menuCode' },
      { title: '菜单名称', dataIndex: 'menuName' },
      { title: '菜单路径', dataIndex: 'menuUrl' },
      { title: '功能编码', dataIndex: 'funcCode' },
    ]"
    :fields="[
      { label: '菜单编码', name: 'menuCode', generated: true },
      { label: '菜单名称', name: 'menuName', required: true },
      { label: '菜单路径', name: 'menuUrl', required: true },
      { label: '所属功能', name: 'funcCode', required: true, type: 'select', options: functionOptions },
      { label: '父级菜单ID', name: 'parentMenuId' },
      { label: '排序', name: 'sortNo', type: 'number' },
    ]"
  >
    <template #editor-before><a-alert v-if="functionError" :message="functionError" type="error" show-icon><template #action><a-button @click="loadFunctions">重试</a-button></template></a-alert></template>
  </CrudPage>
</template>
