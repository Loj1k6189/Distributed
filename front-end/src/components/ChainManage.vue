<template>
  <div class="chain-manage">
    <h2>接龙管理</h2>

    <div class="toolbar">
      <label for="manage-chain-select">选择接龙</label>
      <select id="manage-chain-select" v-model="selectedId" @change="handleSelectionChange">
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
    <div v-else-if="chains.length === 0" class="empty">暂无可管理的接龙</div>
    <div v-else-if="!selectedId" class="empty">请选择一个接龙查看详情</div>
    <div v-else-if="detailLoading" class="loading">加载接龙详情中...</div>
    <div v-else-if="detailError" class="error-msg">{{ detailError }}</div>
    <div v-else-if="chain" class="manage-panel">
      <div class="panel-head">
        <div>
          <h3>{{ chain.title }}</h3>
          <p class="description">{{ chain.description || '（无描述）' }}</p>
        </div>
        <span :class="['status', chain.isActive ? 'active' : 'inactive']">
          {{ chain.isActive ? '进行中' : '已停止' }}
        </span>
      </div>

      <div class="meta-grid">
        <div>创建者：{{ chain.createdBy }}</div>
        <div>参与人数：{{ chain.participantCount ?? 0 }}<template v-if="chain.maxParticipants"> / {{ chain.maxParticipants }}</template></div>
        <div>重复参与：{{ chain.allowMultiple ? '允许' : '不允许' }}</div>
        <div>创建时间：{{ formatTime(chain.createdAt) }}</div>
        <div v-if="chain.startTime">开始时间：{{ formatTime(chain.startTime) }}</div>
        <div v-if="chain.endTime">结束时间：{{ formatTime(chain.endTime) }}</div>
      </div>

      <div class="delete-box">
        <div class="form-row">
          <label>删除操作的用户 ID</label>
          <input v-model="operatorUserId" placeholder="必须填写创建者 ID 才能删除" />
        </div>
        <button
          type="button"
          class="danger-btn"
          @click="deleteChain"
          :disabled="deleting || !chain.isActive"
        >
          {{ deleting ? '删除中...' : '停止并删除接龙' }}
        </button>
      </div>

      <div class="entries">
        <h4>接龙记录</h4>
        <div v-if="(chain.entries || []).length === 0" class="empty">暂无接龙记录</div>
        <table v-else>
          <thead>
            <tr>
              <th>序号</th>
              <th>用户</th>
              <th>内容</th>
              <th>回复</th>
              <th>时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="entry in chain.entries" :key="entry.id">
              <td>#{{ entry.sequenceNo }}</td>
              <td>{{ entry.userId }}</td>
              <td>{{ entry.content }}</td>
              <td>{{ entry.parentEntryId ? `#${findParentSequence(entry.parentEntryId)}` : '-' }}</td>
              <td>{{ formatTime(entry.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
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
const operatorUserId = ref('')
const listLoading = ref(false)
const detailLoading = ref(false)
const deleting = ref(false)
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
    if (!operatorUserId.value && chain.value?.createdBy) {
      operatorUserId.value = chain.value.createdBy
    }
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
    await router.replace(`/chain/manage/${nextId}`)
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
    await router.replace('/chain/manage')
    chain.value = null
    return
  }
  await router.replace(`/chain/manage/${selectedId.value}`)
}

const deleteChain = async () => {
  if (!selectedId.value || !chain.value) {
    alert('请先选择接龙')
    return
  }
  if (!operatorUserId.value.trim()) {
    alert('请输入操作用户 ID')
    return
  }

  const confirmed = window.confirm(`确定要删除接龙「${chain.value.title}」吗？`)
  if (!confirmed) return

  deleting.value = true
  try {
    await http.request({
      method: 'delete',
      url: `/api/chains/${selectedId.value}`,
      headers: {
        'X-User-Id': operatorUserId.value.trim()
      }
    })
    alert('接龙已删除')
    operatorUserId.value = ''
    await loadChains()
    if (chains.value.length === 0) {
      await router.replace('/chain/manage')
    }
  } catch (e) {
    alert('删除失败：' + (e.response?.data?.message || e.message))
  } finally {
    deleting.value = false
  }
}

const formatTime = (value) => {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN')
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
.chain-manage {
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
.form-row input {
  min-width: 240px;
  padding: 8px 10px;
}

.loading,
.empty,
.description {
  color: #666;
}

.manage-panel,
.entries {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.panel-head h3,
.entries h4 {
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

.meta-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 8px 16px;
  color: #666;
}

.delete-box,
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
}

.danger-btn {
  width: fit-content;
  background: #fff1f0;
  color: #c62828;
  border: 1px solid #ffcdd2;
}

table {
  width: 100%;
  border-collapse: collapse;
  background: white;
}

th,
td {
  border: 1px solid #e5e7eb;
  padding: 10px;
  text-align: left;
  vertical-align: top;
}

.error-msg {
  color: #c62828;
  background: #ffebee;
  padding: 12px;
  border-radius: 4px;
}
</style>
