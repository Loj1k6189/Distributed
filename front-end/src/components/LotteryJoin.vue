<template>
  <div class="lottery-join">
    <h2>参与抽奖</h2>
    <div>
      <div class="field">
        <label>活动ID</label>
        <input v-model="activityId" placeholder="请输入活动ID" />
      </div>
      <div v-if="activities.length" class="activity-list">
        <span class="label">当前活动：</span>
        <button
          v-for="item in activities"
          :key="item"
          class="activity-chip"
          @click="activityId = item"
        >
          {{ item }}<span v-if="item === currentActivityId">（进行中）</span>
        </button>
      </div>
      <input v-model="userId" placeholder="请输入用户ID" />
      <button @click="join" :disabled="joining">参与抽奖</button>
      <p v-if="result" class="success">{{ result }}</p>
      <p v-if="error" class="error">{{ error }}</p>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { http } from '../lib/http'

const route = useRoute()
const activityId = ref(String(route.params.activityId ?? ''))

const userId = ref('')
const joining = ref(false)
const result = ref('')
const error = ref('')
const activities = ref([])
const currentActivityId = ref('')

const join = async () => {
  if (!activityId.value.trim()) {
    error.value = '请输入活动ID'
    return
  }
  if (!userId.value.trim()) {
    error.value = '请输入用户ID'
    return
  }
  
  joining.value = true
  error.value = ''
  const prevResult = result.value
  // 乐观更新：先展示参与成功，后端失败则回滚
  result.value = '已加入抽奖池（等待确认）'
  try {
    await http.post(`/api/lottery/${activityId.value.trim()}/join?userId=${encodeURIComponent(userId.value.trim())}`, null, { idempotencyKey: `${activityId.value.trim()}-${userId.value.trim()}` })
    result.value = '参与成功！已加入抽奖池'
    userId.value = ''
  } catch (e) {
    result.value = prevResult
    error.value = '参与失败：' + (e.response?.data?.message || e.message)
  } finally {
    joining.value = false
  }
}

const loadActivities = async () => {
  try {
    const res = await http.get('/api/lottery/activities')
    activities.value = res.activities ?? []
    currentActivityId.value = res.currentActivityId ?? ''
    if (!activityId.value && currentActivityId.value) {
      activityId.value = currentActivityId.value
    }
  } catch (e) {
    // ignore activity list errors for join flow
  }
}

onMounted(loadActivities)
</script>

<style scoped>
.field {
  display: flex;
  flex-direction: column;
  margin-bottom: 10px;
}

.activity-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  align-items: center;
}

.activity-list .label {
  color: black;
}

.activity-chip {
  border: 1px solid #ddd;
  padding: 4px 8px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--primary), #06b6d4);
}
</style>