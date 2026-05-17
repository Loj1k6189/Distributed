<template>
  <div class="admin-container">
    <h2>投票管理</h2>

    <div class="action-group">
      <h3>数据恢复</h3>
      <button @click="rebuild" :disabled="loading.rebuild">
        {{ loading.rebuild ? '执行中...' : '重建 Redis 计数' }}
      </button>
      <p v-if="results.rebuild" class="result-msg">{{ results.rebuild }}</p>
      <p v-if="errors.rebuild" class="error-msg">{{ errors.rebuild }}</p>
    </div>

    <div class="action-group">
      <h3>数据快照</h3>
      <button @click="snapshot" :disabled="loading.snapshot">
        {{ loading.snapshot ? '执行中...' : '生成快照' }}
      </button>
      <p v-if="results.snapshot" class="result-msg">{{ results.snapshot }}</p>
      <p v-if="errors.snapshot" class="error-msg">{{ errors.snapshot }}</p>
    </div>

    <div class="action-group">
      <h3>DLQ 重试</h3>
      <div class="dlq-controls">
        <label>重试条数：</label>
        <input v-model.number="dlqLimit" type="number" min="1" max="1000" />
      </div>
      <button @click="dlqRetry" :disabled="loading.dlq">
        {{ loading.dlq ? '执行中...' : '重试 DLQ 消息' }}
      </button>
      <p v-if="results.dlq" class="result-msg">{{ results.dlq }}</p>
      <p v-if="errors.dlq" class="error-msg">{{ errors.dlq }}</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import axios from 'axios'

const loading = reactive({
  rebuild: false,
  snapshot: false,
  dlq: false
})

const results = reactive({
  rebuild: '',
  snapshot: '',
  dlq: ''
})

const errors = reactive({
  rebuild: '',
  snapshot: '',
  dlq: ''
})

const dlqLimit = ref(100)

async function rebuild() {
  loading.rebuild = true
  errors.rebuild = ''
  results.rebuild = ''
  try {
    const res = await axios.post('/api/votes/admin/recovery/rebuild')
    // 后端返回 int（重建影响数或状态），优先显示返回值
    const body = res.data
    results.rebuild = `重建成功：${body ?? '操作已完成'}`
  } catch (e) {
    errors.rebuild = `重建失败：${e.response?.data?.message || e.message}`
  } finally {
    loading.rebuild = false
  }
}

async function snapshot() {
  loading.snapshot = true
  errors.snapshot = ''
  results.snapshot = ''
  try {
    // snapshot 接口无返回 body，调用成功则提示已触发
    await axios.post('/api/votes/admin/snapshot')
    results.snapshot = `快照已触发`;
  } catch (e) {
    errors.snapshot = `快照失败：${e.response?.data?.message || e.message}`
  } finally {
    loading.snapshot = false
  }
}

async function dlqRetry() {
  loading.dlq = true
  errors.dlq = ''
  results.dlq = ''
  try {
    const res = await axios.post(`/api/votes/admin/dlq/retry?limit=${dlqLimit.value}`)
    // 后端返回 { retriedCount }
    const retried = res.data?.retriedCount ?? res.data
    results.dlq = `DLQ 重试成功：已重试 ${retried} 条`;
  } catch (e) {
    errors.dlq = `DLQ 重试失败：${e.response?.data?.message || e.message}`
  } finally {
    loading.dlq = false
  }
}
</script>

<style scoped>
.admin-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 24px;
}

.action-group {
  margin-bottom: 32px;
  padding: 16px;
  border: 1px solid #eee;
  border-radius: 8px;
}

.action-group h3 {
  margin-top: 0;
}

button {
  margin-top: 8px;
  padding: 8px 20px;
  cursor: pointer;
}

.result-msg {
  color: #2e7d32;
  background: #e8f5e9;
  padding: 8px;
  border-radius: 4px;
}

.error-msg {
  color: #c62828;
  background: #ffebee;
  padding: 8px;
  border-radius: 4px;
}

.dlq-controls {
  margin: 8px 0;
}

input {
  width: 80px;
  padding: 4px;
}
</style>