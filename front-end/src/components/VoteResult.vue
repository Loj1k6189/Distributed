<!-- src/components/VoteResult.vue -->
<template>
  <div>
    <h2>投票结果</h2>
    <div v-if="loading">加载中...</div>
    <div v-else>
      <h3>{{ result.name || '投票活动 #' + pollId }}</h3>
      <p>总票数：{{ result.ballots }}</p>
      <p>投票模式：{{ result.allowMultiple ? '多选' : '单选' }}</p>
      <div v-for="item in result.options" :key="item.optionId">
        <p>{{ item.optionText }} —— 票数：{{ item.votes }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const pollId = route.params.id

const result = ref({ options: [] })
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await axios.get(`/api/votes/polls/${pollId}/result`)
    result.value = res.data
  } catch (e) {
    alert('加载结果失败：' + (e.response?.data?.message || e.message))
    console.error(e)
  } finally {
    loading.value = false
  }
})
</script>