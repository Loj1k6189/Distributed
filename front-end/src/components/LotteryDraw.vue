<template>
  <div class="lottery-draw">
    <h2>执行抽奖</h2>
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
import { ref } from 'vue'
import axios from 'axios'

const activityId = ref('')
const round = ref(1)
const count = ref(5)
const drawing = ref(false)
const winners = ref([])
const error = ref('')

const draw = async () => {
  if (!activityId.value.trim()) {
    error.value = '请输入活动ID'
    return
  }
  
  drawing.value = true
  error.value = ''
  winners.value = []
  
  try {
    const res = await axios.post(
      `/api/lottery/${activityId.value}/draw?round=${round.value}&count=${count.value}`
    )
    winners.value = res.data
  } catch (e) {
    error.value = '抽奖失败：' + (e.response?.data?.message || e.message)
  } finally {
    drawing.value = false
  }
}
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
</style>