<template>
  <div class="lottery-draw">
    <h2>执行抽奖</h2>
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
    <div class="draw-form">
      <div class="form-row">
        <label>活动ID</label>
        <input v-model="activityId" />
      </div>
      <div class="form-row">
        <label>轮次</label>
        <input v-model.number="round" type="number" min="1" />
      </div>
      <div class="form-row">
        <label>抽奖人数</label>
        <input v-model.number="count" type="number" min="1" />
      </div>
      <button @click="draw" :disabled="drawing">执行抽奖</button>
    </div>
    
    <div v-if="winners.length > 0" class="winners">
      <h3>本轮中奖者</h3>
      <ul>
        <li v-for="(winner, idx) in winners" :key="idx">{{ winner }}</li>
      </ul>
    </div>
    
    <p v-if="error" class="error">{{ error }}</p>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { http } from '../lib/http'

const activityId = ref('')
const round = ref(1)
const count = ref(5)
const drawing = ref(false)
const winners = ref([])
const error = ref('')
const activities = ref([])
const currentActivityId = ref('')

const draw = async () => {
  if (!activityId.value.trim()) {
    error.value = '请输入活动ID'
    return
  }
  
  drawing.value = true
  error.value = ''
  winners.value = []
  
  try {
    const res = await http.post(
      `/api/lottery/${activityId.value}/draw?round=${round.value}&count=${count.value}`
    )
    winners.value = res
  } catch (e) {
    error.value = '抽奖失败：' + (e.response?.data?.message || e.message)
  } finally {
    drawing.value = false
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
    // ignore activity list errors for draw flow
  }
}

onMounted(loadActivities)
</script>

<style scoped>
.lottery-draw {
  max-width: 560px;
}

.draw-form {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.form-row {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 6px;
  margin-bottom: 14px;
}

.form-row :deep(input) {
  max-width: 100%;
}

.winners ul {
  margin-top: 8px;
}

.winners li {
  margin-bottom: 6px;
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