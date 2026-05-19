<template>
  <div class="lottery-history">
    <h2>中奖历史</h2>
    <div>
      <input v-model="activityId" placeholder="输入活动ID" />
      <button @click="loadHistory" :disabled="loading">查询</button>
    </div>
    
    <div v-if="!loading && history.content">
      <table border="1" cellpadding="8">
        <thead>
          <tr>
            <th>序号</th>
            <th>用户ID</th>
            <th>轮次</th>
            <th>中奖时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, idx) in history.content" :key="item.id">
            <td>{{ idx + 1 + page * size }}</td>
            <td>{{ item.userId }}</td>
            <td>{{ item.round }}</td>
            <td>{{ formatTime(item.wonAt) }}</td>
          </tr>
        </tbody>
      </table>
      
      <div class="pagination">
        <button @click="prevPage" :disabled="page === 0">上一页</button>
        <span>第 {{ page + 1 }} / {{ history.totalPages }} 页</span>
        <button @click="nextPage" :disabled="page >= history.totalPages - 1">下一页</button>
      </div>
    </div>
    
    <p v-if="error" class="error">{{ error }}</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { http } from '../lib/http'

const activityId = ref('')
const page = ref(0)
const size = ref(10)
const loading = ref(false)
const history = ref({ content: [], totalPages: 0 })
const error = ref('')

const loadHistory = async () => {
  if (!activityId.value.trim()) {
    error.value = '请输入活动ID'
    return
  }
  
  loading.value = true
  error.value = ''
  
  try {
    const res = await http.get(
      `/api/lottery/${activityId.value}/history?page=${page.value}&size=${size.value}`
    )
    history.value = res
  } catch (e) {
    error.value = '查询失败：' + (e.response?.data?.message || e.message)
  } finally {
    loading.value = false
  }
}

const prevPage = () => {
  if (page.value > 0) {
    page.value--
    loadHistory()
  }
}

const nextPage = () => {
  if (page.value < history.value.totalPages - 1) {
    page.value++
    loadHistory()
  }
}

const formatTime = (isoString) => {
  return new Date(isoString).toLocaleString('zh-CN')
}
</script>