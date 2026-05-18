<!-- src/components/VoteSubmit.vue -->
<template>
  <div>
    <h2>参与投票</h2>
    <div v-if="loading">加载中...</div>
    <div v-else>
      <h3>投票活动 #{{ pollId }}</h3>

      <div style="margin: 10px 0">
        <label>投票人ID：</label>
        <input v-model="voterId" placeholder="输入 voterId" />
      </div>

      <div v-if="poll.allowMultiple">
        <div v-for="opt in poll.options" :key="opt.optionId">
          <label>
            <input
              type="checkbox"
              v-model="selectedMulti"
              :value="opt.optionId"
            />
            {{ opt.optionText }}
          </label>
        </div>
      </div>
      <div v-else>
        <div v-for="opt in poll.options" :key="opt.optionId">
          <label>
            <input
              type="radio"
              v-model="selectedSingle"
              :value="opt.optionId"
              name="option"
            />
            {{ opt.optionText }}
          </label>
        </div>
      </div>

      <button @click="submitVote" style="margin-top:10px">提交投票</button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const pollId = route.params.id

const poll = ref({ name: '', allowMultiple: false, options: [] })
const voterId = ref('')
const selectedSingle = ref(null)
const selectedMulti = ref([])
const loading = ref(true)

const optionIds = computed(() => {
  if (poll.value.allowMultiple) {
    return Array.isArray(selectedMulti.value) ? selectedMulti.value : []
  }
  return selectedSingle.value ? [selectedSingle.value] : []
})

onMounted(async () => {
  try {
    const res = await axios.get(`/api/votes/polls/${pollId}/result`)
    poll.value = res.data
  } catch (e) {
    alert('加载投票信息失败：' + (e.response?.data?.message || e.message))
    console.error(e)
  } finally {
    loading.value = false
  }
})

const submitVote = async () => {
  if (!voterId.value.trim()) {
    alert('请输入 voterId')
    return
  }
  if (optionIds.value.length === 0) {
    alert('请至少选择一个选项')
    return
  }

  try {
    await axios.post('/api/votes/submit', {
      pollId: Number(pollId),
      voterId: voterId.value.trim(),
      optionIds: optionIds.value
    })
    alert('投票成功！')
  } catch (e) {
    alert('投票失败：' + (e.response?.data?.message || e.message))
    console.error(e)
  }
}
</script>