<template>
  <div class="lottery-winners">
    <h2>查看中奖者</h2>
    <div class="search-bar">
      <div class="field">
        <label>活动ID</label>
        <input v-model="activityId" placeholder="输入活动ID" />
      </div>
      <div class="field">
        <label>轮次</label>
        <input v-model.number="round" type="number" min="1" placeholder="1" />
      </div>
      <button @click="fetchWinners" :disabled="loading">查询</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-if="!loading && winners.length > 0" class="winners">
      <h3>第 {{ round }} 轮中奖者（共 {{ winners.length }} 人）</h3>
      <ul>
        <li v-for="(user, idx) in winners" :key="idx">{{ user }}</li>
      </ul>
    </div>

    <div v-if="!loading && fetched && winners.length === 0" class="empty">
      暂无中奖记录
    </div>

    <p v-if="error" class="error">{{ error }}</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'

const activityId = ref('')
const round = ref(1)
const loading = ref(false)
const winners = ref([])
const error = ref('')
const fetched = ref(false) // 是否已经执行过查询

const fetchWinners = async () => {
  if (!activityId.value.trim()) {
    error.value = '请输入活动ID'
    return
  }
  if (round.value < 1) {
    error.value = '轮次必须大于0'
    return
  }

  loading.value = true
  error.value = ''
  winners.value = []
  fetched.value = false

  try {
    const res = await axios.get(
      `/api/lottery/${activityId.value.trim()}/winners/latest?round=${round.value}`
    )
    winners.value = res.data
    fetched.value = true
  } catch (e) {
    error.value = '查询失败：' + (e.response?.data?.message || e.message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.lottery-winners {
  max-width: 500px;
  margin: 0 auto;
}
.search-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: flex-end;
  margin-bottom: 20px;
}
.field {
  display: flex;
  flex-direction: column;
}
.winners ul {
  list-style: none;
  padding: 0;
}
.winners li {
  padding: 4px 0;
  font-weight: 500;
}
.error {
  color: red;
}
.loading {
  color: #666;
}
.empty {
  color: #888;
}
</style>