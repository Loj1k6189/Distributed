<template>
  <div class="chain-join">
    <h2>参与接龙</h2>

    <div class="toolbar">
      <label for="chain-select">选择接龙</label>
      <select id="chain-select" v-model="selectedId" @change="handleSelectionChange">
        <option value="">请选择接龙</option>
        <option v-for="item in chains" :key="item.id" :value="String(item.id)">
          {{ item.title }}
        </option>
      </select>
      <button type="button" @click="loadChains" :disabled="listLoading">
        {{ listLoading ? '刷新中...' : '刷新列表' }}
      </button>
    </div>

    <div v-if="listError" class="error-msg">{{ listError }}</div>
    <div v-if="listLoading && chains.length === 0" class="loading">加载接龙列表中...</div>
    <div v-else-if="chains.length === 0" class="empty">暂无可参与的接龙</div>
    <div v-else-if="!selectedId" class="empty">请选择一个接龙后参与</div>
    <div v-else-if="detailLoading" class="loading">加载接龙详情中...</div>
    <div v-else-if="detailError" class="error-msg">{{ detailError }}</div>
    <div v-else-if="chain">
      <h3>{{ chain.title }}</h3>
      <p class="description">{{ chain.description || '（无描述）' }}</p>

      <div class="meta-grid">
        <div>创建者：{{ chain.createdBy }}</div>
        <div>参与人数：{{ chain.participantCount ?? 0 }}<template v-if="chain.maxParticipants"> / {{ chain.maxParticipants }}</template></div>
        <div>重复参与：{{ chain.allowMultiple ? '允许' : '不允许' }}</div>
        <div>创建时间：{{ formatTime(chain.createdAt) }}</div>
      </div>

      <div class="join-panel">
        <div class="form-row">
          <label>用户 ID</label>
          <input v-model="userId" placeholder="请输入用户 ID" />
        </div>

        <div class="form-row">
          <label>接龙内容</label>
          <textarea v-model="content" rows="3" placeholder="请输入您的接龙内容" />
        </div>

        <div class="form-row">
          <label>回复某条接龙（可选）</label>
          <select v-model="parentEntryId">
            <option value="">直接追加到末尾</option>
            <option v-for="entry in chain.entries || []" :key="entry.id" :value="String(entry.id)">
              #{{ entry.sequenceNo }} {{ entry.userId }}：{{ shorten(entry.content) }}
            </option>
          </select>
        </div>

        <button type="button" @click="joinChain" :disabled="submitting">
          {{ submitting ? '提交中...' : '提交接龙' }}
        </button>
      </div>

      <div class="entries">
        <div class="entries-header">
          <h4>接龙内容</h4>
          <button type="button" class="link-btn" @click="loadChain(selectedId)" :disabled="detailLoading">
            刷新详情
          </button>
        </div>

        <div v-if="(chain.entries || []).length === 0" class="empty">还没有人参与接龙</div>
        <div v-else class="entry-list">
          <div v-for="entry in chain.entries" :key="entry.id" class="entry-item">
            <div class="entry-main">
              <strong>#{{ entry.sequenceNo }}</strong>
              <span>{{ entry.userId }}</span>
              <span v-if="entry.parentEntryId" class="reply-tag">回复 #{{ findParentSequence(entry.parentEntryId) }}</span>
            </div>
            <div class="entry-content">{{ entry.content }}</div>
            <div class="entry-time">{{ formatTime(entry.createdAt) }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { http } from '../lib/http'

const route = useRoute()
const router = useRouter()

const chains = ref([])
const selectedId = ref(route.params.id ? String(route.params.id) : '')
const chain = ref(null)
const userId = ref('')
const content = ref('')
const parentEntryId = ref('')
const listLoading = ref(false)
const detailLoading = ref(false)
const submitting = ref(false)
const listError = ref('')
const detailError = ref('')

const unwrapData = (payload) => payload?.data ?? payload

const loadChain = async (id) => {
  if (!id) {
    chain.value = null
    return
  }

  detailLoading.value = true
  detailError.value = ''
  try {
    const payload = await http.get(`/api/chains/${id}`)
    chain.value = unwrapData(payload)
  } catch (e) {
    chain.value = null
    detailError.value = '加载接龙详情失败：' + (e.response?.data?.message || e.message)
  } finally {
    detailLoading.value = false
  }
}

const syncSelection = async () => {
  if (chains.value.length === 0) {
    selectedId.value = ''
    chain.value = null
    return
  }

  const routeId = route.params.id ? String(route.params.id) : ''
  const currentId = routeId || selectedId.value
  const exists = chains.value.some((item) => String(item.id) === currentId)
  const nextId = exists ? currentId : String(chains.value[0].id)

  if (String(route.params.id || '') !== nextId) {
    await router.replace(`/chain/join/${nextId}`)
    return
  }

  selectedId.value = nextId
  await loadChain(nextId)
}

const loadChains = async () => {
  listLoading.value = true
  listError.value = ''
  try {
    const payload = await http.get('/api/chains/active')
    const data = unwrapData(payload)
    chains.value = Array.isArray(data) ? data : []
    await syncSelection()
  } catch (e) {
    chains.value = []
    listError.value = '加载接龙列表失败：' + (e.response?.data?.message || e.message)
  } finally {
    listLoading.value = false
  }
}

const handleSelectionChange = async () => {
  if (!selectedId.value) {
    await router.replace('/chain/join')
    chain.value = null
    return
  }
  await router.replace(`/chain/join/${selectedId.value}`)
}

const joinChain = async () => {
  if (!selectedId.value) {
    alert('请先选择接龙')
    return
  }
  if (!userId.value.trim()) {
    alert('请输入用户 ID')
    return
  }
  if (!content.value.trim()) {
    alert('请输入接龙内容')
    return
  }

  submitting.value = true
  try {
    await http.post(
      `/api/chains/${selectedId.value}/join`,
      {
        content: content.value.trim(),
        parentEntryId: parentEntryId.value ? Number(parentEntryId.value) : null
      },
      {
        headers: {
          'X-User-Id': userId.value.trim()
        },
        idempotencyKey: `chain-${selectedId.value}-${userId.value.trim()}-${content.value.trim()}`
      }
    )
    alert('接龙成功')
    content.value = ''
    parentEntryId.value = ''
    await loadChain(selectedId.value)
  } catch (e) {
    alert('接龙失败：' + (e.response?.data?.message || e.message))
  } finally {
    submitting.value = false
  }
}

const formatTime = (value) => {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN')
}

const shorten = (value) => {
  if (!value) return ''
  return value.length > 24 ? `${value.slice(0, 24)}...` : value
}

const findParentSequence = (entryId) => {
  const target = (chain.value?.entries || []).find((item) => item.id === entryId)
  return target?.sequenceNo ?? '?'
}

watch(
  () => route.params.id,
  async (id) => {
    selectedId.value = id ? String(id) : ''
    if (!selectedId.value) {
      chain.value = null
      return
    }
    await loadChain(selectedId.value)
  }
)

onMounted(loadChains)
</script>

<style scoped>
.chain-join {
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

.toolbar select,
.form-row input,
.form-row textarea,
.form-row select {
  min-width: 240px;
  padding: 8px 10px;
}

.loading,
.empty,
.description {
  color: #666;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 8px 16px;
  color: #666;
}

.join-panel,
.entries {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
}

.entries-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}

.entries-header h4 {
  margin: 0;
}

.entry-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.entry-item {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  background: white;
}

.entry-main {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.reply-tag {
  color: #2f3cb5;
  background: #eef2ff;
  border-radius: 999px;
  padding: 2px 8px;
  font-size: 12px;
}

.entry-content {
  margin: 8px 0;
  white-space: pre-wrap;
}

.entry-time {
  color: #666;
  font-size: 12px;
}

.link-btn {
  background: transparent;
  color: #2f3cb5;
  border: none;
  padding: 0;
}

.error-msg {
  color: #c62828;
  background: #ffebee;
  padding: 12px;
  border-radius: 4px;
}
</style>
