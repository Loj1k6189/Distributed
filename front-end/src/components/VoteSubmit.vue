<!-- src/components/VoteSubmit.vue -->
<template>
  <div>
    <h2>参与投票</h2>
    <div v-if="loading">加载中...</div>
    <div v-else>
      <h3>{{ poll.title }}</h3>

      <div v-for="opt in poll.options" :key="opt.id">
        <label>
          <input
            :type="poll.type === 'SINGLE' ? 'radio' : 'checkbox'"
            v-model="selected"
            :value="opt.id"
            name="opt"
          />
          {{ opt.content }}
        </label>
      </div>

      <button @click="submitVote" style="margin-top:10px">提交投票</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const pollId = route.params.id

const poll = ref({})
const selected = ref([])
const loading = ref(true)

onMounted(async () => {
  const res = await axios.get(`/api/votes/polls/${pollId}`)
  poll.value = res.data
  loading.value = false
})

const submitVote = async () => {
  try {
    await axios.post('/api/votes/submit', {
      pollId: pollId,
      optionIds: selected.value
    })
    alert('投票成功！')
  } catch (e) {
    alert('投票失败：' + e.response?.data?.message)
  }
}
</script>