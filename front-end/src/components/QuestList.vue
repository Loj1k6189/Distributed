<template>
  <div class="quest-list">
    <h2>问卷列表</h2>
    
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="quests.length === 0" class="empty">暂无问卷</div>
    <div v-else class="quest-container">
      <div v-for="quest in quests" :key="quest.id" class="quest-item">
        <div class="quest-header">
          <h3>{{ quest.title }}</h3>
          <span :class="['status', quest.isActive ? 'active' : 'inactive']">
            {{ quest.isActive ? '进行中' : '已结束' }}
          </span>
        </div>
        
        <p class="description">{{ quest.description || '（无描述）' }}</p>
        
        <div class="quest-meta">
          <span>创建时间：{{ formatTime(quest.createdAt) }}</span>
          <span v-if="quest.expiresAt">过期时间：{{ formatTime(quest.expiresAt) }}</span>
        </div>
        
        <div class="quest-actions">
          <router-link :to="`/quest/submit/${quest.id}`" class="btn btn-primary">
            参与问卷
          </router-link>
          <router-link :to="`/quest/stats/${quest.id}`" class="btn btn-secondary">
            查看统计
          </router-link>
        </div>
      </div>
    </div>
    
    <div v-if="error" class="error-msg">{{ error }}</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { http } from '../lib/http'

const quests = ref([])
const loading = ref(true)
const error = ref('')

const loadQuests = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await http.get('/api/v1/question')
    quests.value = Array.isArray(res) ? res : []
  } catch (e) {
    error.value = '加载问卷列表失败：' + (e.response?.data?.message || e.message)
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(loadQuests)

const formatTime = (isoString) => {
  if (!isoString) return '-'
  return new Date(isoString).toLocaleString('zh-CN')
}
</script>

<style scoped>
.quest-list {
  padding: 20px;
}

.loading, .empty {
  text-align: center;
  padding: 40px;
  color: #666;
}

.quest-container {
  display: grid;
  gap: 16px;
}

.quest-item {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 16px;
  background-color: #fafafa;
  transition: box-shadow 0.3s;
}

.quest-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.quest-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.quest-header h3 {
  margin: 0;
  color: #333;
  font-size: 18px;
}

.status {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
}

.status.active {
  background-color: #e6f7ff;
  color: #0050b3;
}

.status.inactive {
  background-color: #f5f5f5;
  color: #999;
}

.description {
  color: #666;
  margin: 8px 0;
  line-height: 1.5;
}

.quest-meta {
  display: flex;
  gap: 20px;
  margin: 12px 0;
  font-size: 12px;
  color: #999;
}

.quest-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  text-decoration: none;
  display: inline-block;
  font-size: 14px;
  transition: all 0.3s;
}

.btn-primary {
  background-color: #1890ff;
  color: white;
}

.btn-primary:hover {
  background-color: #0050b3;
}

.btn-secondary {
  background-color: #f0f0f0;
  color: #333;
  border: 1px solid #d9d9d9;
}

.btn-secondary:hover {
  background-color: #e6e6e6;
}

.error-msg {
  color: #c62828;
  background-color: #ffebee;
  padding: 12px;
  border-radius: 4px;
  margin-top: 16px;
}
</style>
