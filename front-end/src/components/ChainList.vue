<template>
  <div class="chain-list">
    <div class="header-row">
      <div>
        <h2>接龙列表</h2>
        <p class="subtitle">查看当前进行中的接龙，并快速进入参与或管理页面。</p>
      </div>
      <button type="button" @click="loadChains" :disabled="loading">
        {{ loading ? '刷新中...' : '刷新列表' }}
      </button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="chains.length === 0" class="empty">暂无进行中的接龙</div>
    <div v-else class="chain-container">
      <div v-for="chain in chains" :key="chain.id" class="chain-item">
        <div class="chain-header">
          <h3>{{ chain.title }}</h3>
          <span :class="['status', chain.isActive ? 'active' : 'inactive']">
            {{ chain.isActive ? '进行中' : '已停止' }}
          </span>
        </div>

        <p class="description">{{ chain.description || '（无描述）' }}</p>

        <div class="chain-meta">
          <span>创建者：{{ chain.createdBy }}</span>
          <span>参与人数：{{ chain.participantCount ?? 0 }}<template v-if="chain.maxParticipants"> / {{ chain.maxParticipants }}</template></span>
          <span>重复参与：{{ chain.allowMultiple ? '允许' : '不允许' }}</span>
        </div>

        <div class="chain-meta">
          <span>创建时间：{{ formatTime(chain.createdAt) }}</span>
          <span v-if="chain.startTime">开始时间：{{ formatTime(chain.startTime) }}</span>
          <span v-if="chain.endTime">结束时间：{{ formatTime(chain.endTime) }}</span>
        </div>

        <div class="preview" v-if="chain.entries?.length">
          <strong>最新接龙：</strong>
          <span>#{{ chain.entries[chain.entries.length - 1].sequenceNo }} {{ chain.entries[chain.entries.length - 1].content }}</span>
        </div>

        <div class="chain-actions">
          <router-link :to="`/chain/join/${chain.id}`" class="btn btn-primary">参与接龙</router-link>
          <router-link :to="`/chain/manage/${chain.id}`" class="btn btn-secondary">管理接龙</router-link>
        </div>
      </div>
    </div>

    <div v-if="error" class="error-msg">{{ error }}</div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { http } from '../lib/http'

const chains = ref([])
const loading = ref(false)
const error = ref('')

const loadChains = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await http.get('/api/chains/active')
    chains.value = Array.isArray(res?.data) ? res.data : []
  } catch (e) {
    chains.value = []
    error.value = '加载接龙列表失败：' + (e.response?.data?.message || e.message)
  } finally {
    loading.value = false
  }
}

const formatTime = (value) => {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN')
}

onMounted(loadChains)
</script>

<style scoped>
.chain-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.header-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.subtitle,
.loading,
.empty {
  color: #666;
}

.chain-container {
  display: grid;
  gap: 16px;
}

.chain-item {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;
}

.chain-header,
.chain-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.chain-header h3 {
  margin: 0;
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
  margin: 12px 0;
}

.chain-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  color: #666;
  margin-bottom: 8px;
  font-size: 13px;
}

.preview {
  margin-top: 12px;
  color: #3d4a66;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  text-decoration: none;
  display: inline-block;
  font-size: 14px;
}

.btn-primary {
  background-color: #1890ff;
  color: white;
}

.btn-secondary {
  background-color: #f0f0f0;
  color: #333;
  border: 1px solid #d9d9d9;
}

.error-msg {
  color: #c62828;
  background-color: #ffebee;
  padding: 12px;
  border-radius: 4px;
}
</style>
