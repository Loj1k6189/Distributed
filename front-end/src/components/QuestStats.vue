<template>
  <div class="quest-stats">
    <h2>问卷统计</h2>

    <div class="toolbar">
      <label for="stats-questionnaire-select">选择问卷</label>
      <select id="stats-questionnaire-select" v-model="selectedId" @change="handleSelectionChange">
        <option value="">请选择问卷</option>
        <option v-for="item in questionnaires" :key="item.id" :value="String(item.id)">
          {{ item.title }}
        </option>
      </select>
      <button type="button" @click="refreshQuestionnaires" :disabled="listLoading">刷新列表</button>
    </div>

    <div v-if="listError" class="error-msg">{{ listError }}</div>
    <div v-if="listLoading && questionnaires.length === 0" class="loading">加载问卷列表中...</div>
    <div v-else-if="questionnaires.length === 0" class="empty">暂无可统计的问卷</div>
    <div v-else-if="!selectedId" class="empty">请选择一个问卷查看统计</div>
    <div v-else-if="loading" class="loading">加载统计中...</div>
    <div v-else-if="error" class="error-msg">{{ error }}</div>
    <div v-else>
      <h3>{{ selectedQuestionnaire?.title || `问卷 #${selectedId}` }}</h3>
      <p class="description">{{ selectedQuestionnaire?.description || '查看当前问卷的汇总统计数据。' }}</p>

      <div class="stats-grid">
        <div class="stat-card">
          <span class="label">总提交数</span>
          <strong>{{ stats.totalSubmissions ?? 0 }}</strong>
        </div>
        <div class="stat-card">
          <span class="label">完整提交</span>
          <strong>{{ stats.completedSubmissions ?? 0 }}</strong>
        </div>
        <div class="stat-card">
          <span class="label">部分提交</span>
          <strong>{{ stats.partialSubmissions ?? 0 }}</strong>
        </div>
        <div class="stat-card">
          <span class="label">唯一用户数</span>
          <strong>{{ stats.uniqueUsers ?? 0 }}</strong>
        </div>
        <div class="stat-card">
          <span class="label">匿名提交数</span>
          <strong>{{ stats.anonymousSubmissions ?? 0 }}</strong>
        </div>
        <div class="stat-card">
          <span class="label">平均完成时长</span>
          <strong>{{ formatDuration(stats.averageCompletionTime) }}</strong>
        </div>
      </div>

      <div class="meta">
        <div>最后提交时间：{{ formatTime(stats.lastSubmissionAt) }}</div>
        <div>快照时间：{{ formatTime(stats.snapshotDate) }}</div>
      </div>

      <button type="button" @click="refresh" :disabled="loading">刷新统计</button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { http } from '../lib/http'

const route = useRoute()
const router = useRouter()

const questionnaires = ref([])
const selectedId = ref(route.params.id ? String(route.params.id) : '')
const stats = ref({})
const listLoading = ref(false)
const loading = ref(false)
const listError = ref('')
const error = ref('')

const unwrapData = (payload) => payload?.data ?? payload

const selectedQuestionnaire = computed(() => {
  return questionnaires.value.find((item) => String(item.id) === selectedId.value) ?? null
})

const syncSelection = async () => {
  if (questionnaires.value.length === 0) {
    selectedId.value = ''
    stats.value = {}
    return
  }

  const routeId = route.params.id ? String(route.params.id) : ''
  const currentId = routeId || selectedId.value
  const exists = questionnaires.value.some((item) => String(item.id) === currentId)
  const nextId = exists ? currentId : String(questionnaires.value[0].id)

  if (String(route.params.id || '') !== nextId) {
    await router.replace(`/quest/stats/${nextId}`)
    return
  }

  selectedId.value = nextId
  await loadStats(nextId)
}

const refreshQuestionnaires = async () => {
  listLoading.value = true
  listError.value = ''
  try {
    const payload = await http.get('/api/questionnaires/active')
    const data = unwrapData(payload)
    questionnaires.value = Array.isArray(data) ? data : []
    await syncSelection()
  } catch (e) {
    questionnaires.value = []
    listError.value = '加载问卷列表失败：' + (e.response?.data?.message || e.message)
  } finally {
    listLoading.value = false
  }
}

const loadStats = async (id = selectedId.value) => {
  if (!id) {
    stats.value = {}
    return
  }

  loading.value = true
  error.value = ''
  try {
    const payload = await http.get(`/api/questionnaires/${id}/statistics`)
    stats.value = unwrapData(payload) || {}
  } catch (e) {
    stats.value = {}
    error.value = '加载统计失败：' + (e.response?.data?.message || e.message)
  } finally {
    loading.value = false
  }
}

const handleSelectionChange = async () => {
  if (!selectedId.value) {
    await router.replace('/quest/stats')
    stats.value = {}
    return
  }
  await router.replace(`/quest/stats/${selectedId.value}`)
}

const refresh = async () => {
  await loadStats()
}

const formatTime = (value) => {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN')
}

const formatDuration = (milliseconds) => {
  if (milliseconds == null) return '-'
  if (milliseconds < 1000) return `${Math.round(milliseconds)} ms`
  return `${(milliseconds / 1000).toFixed(2)} s`
}

watch(
  () => route.params.id,
  async (id) => {
    selectedId.value = id ? String(id) : ''
    if (!selectedId.value) {
      stats.value = {}
      return
    }
    await loadStats(selectedId.value)
  }
)

onMounted(refreshQuestionnaires)
</script>

<style scoped>
.quest-stats {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.toolbar select {
  min-width: 240px;
  padding: 8px 10px;
}

.loading,
.empty,
.description,
.meta {
  color: #666;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
}

.stat-card {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;
}

.label {
  display: block;
  margin-bottom: 8px;
  color: #666;
}

.meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.error-msg {
  color: #c62828;
  background: #ffebee;
  padding: 12px;
  border-radius: 4px;
}
</style>