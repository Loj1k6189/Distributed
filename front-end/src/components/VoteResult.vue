<!-- src/components/VoteResult.vue -->
<template>
  <div>
    <h2>投票结果</h2>
    <div v-if="loading">加载中...</div>
    <div v-else>
      <h3>{{ result.title }}</h3>
      <div v-for="item in result.options" :key="item.id">
        <p>{{ item.content }} —— 票数：{{ item.voteCount }}</p>
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

const result = ref({})
const loading = ref(true)

onMounted(async () => {
  const res = await axios.get(`/api/votes/polls/${pollId}/result`)
  result.value = res.data
  loading.value = false
})
</script>