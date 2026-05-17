<template>
  <div class="lottery-draw">
    <h2>执行抽奖</h2>
    <div>
      <div>
        <label>活动ID</label>
        <input v-model="activityId" />
      </div>
      <div>
        <label>轮次</label>
        <input v-model.number="round" type="number" min="1" />
      </div>
      <div>
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