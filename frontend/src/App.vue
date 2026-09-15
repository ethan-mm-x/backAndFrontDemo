<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from './api/http'

const loading = ref(false)
const result = ref('')

async function checkHealth() {
  loading.value = true
  result.value = ''
  try {
    const { data } = await http.get<{ status: string; redis?: string }>('/health')
    result.value = data.status
    ElMessage.success(`健康检查成功：${data.status}，Redis：${data.redis ?? '-'}`)
  } catch (e) {
    result.value = 'error'
    ElMessage.error('健康检查失败，请确认后端与 Docker 已启动')
    console.error(e)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page">
    <h1>backAndFrontDemo</h1>
    <p class="hint">空骨架联调：点击按钮请求后端 /api/health</p>
    <el-button type="primary" :loading="loading" @click="checkHealth">健康检查</el-button>
    <p v-if="result" class="result">结果：{{ result }}</p>
  </div>
</template>

<style scoped>
.page {
  max-width: 480px;
  margin: 80px auto;
  padding: 0 24px;
  font-family: "PingFang SC", "Helvetica Neue", sans-serif;
}
h1 {
  margin: 0 0 8px;
  font-size: 28px;
}
.hint {
  margin: 0 0 24px;
  color: #666;
}
.result {
  margin-top: 16px;
  font-size: 18px;
}
</style>
