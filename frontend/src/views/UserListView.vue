<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { batchDeleteUsers, pageUsers } from '../api/user'
import { clearToken } from '../api/http'

const router = useRouter()
const loading = ref(false)
const tableData = ref<{ id: number; username: string; createdAt: string }[]>([])
const total = ref(0)
const selectedIds = ref<number[]>([])
const query = reactive({
  page: 1,
  size: 10,
  username: '',
})

async function load() {
  loading.value = true
  try {
    const { data } = await pageUsers({
      page: query.page,
      size: query.size,
      username: query.username || undefined,
    })
    tableData.value = data.data.records
    total.value = data.data.total
  } finally {
    loading.value = false
  }
}

function onSelectionChange(rows: { id: number }[]) {
  selectedIds.value = rows.map((r) => r.id)
}

async function onBatchDelete() {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先勾选用户')
    return
  }
  await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 个用户？`, '提示', {
    type: 'warning',
  })
  await batchDeleteUsers(selectedIds.value)
  ElMessage.success('删除成功')
  await load()
}

async function onDeleteRow(row: { id: number; username: string }) {
  await ElMessageBox.confirm(`确认删除用户「${row.username}」？`, '提示', {
    type: 'warning',
  })
  await batchDeleteUsers([row.id])
  ElMessage.success('删除成功')
  await load()
}

function logout() {
  clearToken()
  router.push('/login')
}

onMounted(load)
</script>

<template>
  <div class="page">
    <header class="header">
      <h1>用户管理</h1>
      <el-button link type="primary" @click="logout">退出登录</el-button>
    </header>

    <div class="toolbar">
      <el-input
        v-model="query.username"
        placeholder="按用户名搜索"
        clearable
        style="width: 220px"
        @keyup.enter="query.page = 1; load()"
      />
      <el-button type="primary" @click="query.page = 1; load()">查询</el-button>
      <el-button type="danger" :disabled="!selectedIds.length" @click="onBatchDelete">批量删除</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border @selection-change="onSelectionChange">
      <el-table-column type="selection" width="48" />
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="createdAt" label="创建时间" width="200" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" @click="onDeleteRow(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        layout="total, prev, pager, next, sizes"
        :total="total"
        :page-sizes="[10, 20, 50]"
        @current-change="load"
        @size-change="() => { query.page = 1; load() }"
      />
    </div>
  </div>
</template>

<style scoped>
.page {
  max-width: 960px;
  margin: 0 auto;
  padding: 32px 24px 48px;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
h1 {
  margin: 0;
  font-size: 24px;
}
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
