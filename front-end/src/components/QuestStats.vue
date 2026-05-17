<template>
  <div class="quest-stats">
    <h2>问卷统计</h2>
    <div v-if="loading">加载中...</div>
    <div v-else>
      <h3>{{ stats.questionTitle }}</h3>
      <p>总提交数：{{ stats.totalSubmissions }}</p>
      <p>最后更新：{{ stats.lastUpdated }}</p>
      
      <table border="1" cellpadding="8">
        <thead>
          <tr>
            <th>选项</th>
            <th>票数</th>
            <th>占比</th>
            <th>排名</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="opt in stats.options" :key="opt.optionId">
            <td>{{ opt.optionText }}</td>
            <td>{{ opt.votes }}</td>
            <td>{{ (opt.percentage * 100).toFixed(2) }}%</td>
            <td>{{ opt.rank }}</td>
          </tr>
        </tbody>
      </table>
      
      <button @click="refresh">刷新</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const questionId = route.params.id

const stats = ref({ options: [], totalSubmissions: 0 })
const loading = ref(true)

const loadStats = async () => {
  try {
    const res = await axios.get(`/api/v1/question/stats/${questionId}`)
    stats.value = res.data
  } catch (e) {
    alert('加载统计失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)

const refresh = async () => {
  loading.value = true
  await loadStats()
}
</script>