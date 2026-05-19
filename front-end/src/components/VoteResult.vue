<template>
  <div class="vote-result">
    <h2>投票结果</h2>

    <div v-if="loadingPolls">加载投票列表中...</div>
    <div v-else-if="polls.length === 0" class="warning">暂无投票活动</div>
    <div v-else>
      <div class="poll-switcher">
        <button
          v-for="poll in polls"
          :key="poll.pollId"
          type="button"
          :class="{ active: selectedPollId === poll.pollId }"
          @click="switchPoll(poll.pollId)"
        >
          #{{ poll.pollId }} {{ poll.name }}
        </button>
      </div>

      <div v-if="loadingResult">加载中...</div>
      <div v-else-if="result">
        <h3>{{ result.name || `投票活动 #${result.pollId}` }}</h3>
        <p>总票数：{{ result.ballots }}</p>
        <p>投票模式：{{ result.allowMultiple ? '多选' : '单选' }}</p>

        <table>
          <thead>
            <tr>
              <th>选项</th>
              <th>票数</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in result.options" :key="item.optionId">
              <td>{{ item.optionText }}</td>
              <td>{{ item.votes }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { http } from '../lib/http'

type PollSummary = {
  pollId: number
  name: string
  allowMultiple: boolean
  status: string
  createdAt: string
}

type PollOption = {
  optionId: number
  optionText: string
  votes: number
}

type PollResult = {
  pollId: number
  name: string
  allowMultiple: boolean
  ballots: number
  options: PollOption[]
}

const route = useRoute()

const polls = ref<PollSummary[]>([])
const selectedPollId = ref<number | null>(null)
const result = ref<PollResult | null>(null)
const loadingPolls = ref(true)
const loadingResult = ref(false)

const resolvePollIdFromRoute = () => {
  const paramId = Number(route.params.id)
  if (Number.isFinite(paramId) && paramId > 0) return paramId
  return null
}

const loadResult = async (pollId: number) => {
  loadingResult.value = true
  try {
    const res = await http.get(`/api/votes/polls/${pollId}/result`)
    result.value = res
  } catch (e: any) {
    alert('加载结果失败：' + (e.response?.data?.message || e.message))
  } finally {
    loadingResult.value = false
  }
}

const loadPolls = async () => {
  loadingPolls.value = true
  try {
    const res = await http.get('/api/votes/polls')
    polls.value = Array.isArray(res) ? res : []
    if (polls.value.length === 0) {
      selectedPollId.value = null
      result.value = null
      return
    }
    const expected = resolvePollIdFromRoute()
    const matched = expected && polls.value.some((item) => item.pollId === expected)
      ? expected
      : polls.value[0].pollId
    selectedPollId.value = matched
    await loadResult(matched)
  } catch (e: any) {
    alert('加载投票列表失败：' + (e.response?.data?.message || e.message))
  } finally {
    loadingPolls.value = false
  }
}

const switchPoll = async (pollId: number) => {
  if (selectedPollId.value === pollId) return
  selectedPollId.value = pollId
  await loadResult(pollId)
}

onMounted(loadPolls)
</script>

<style scoped>
.vote-result {
  max-width: 760px;
}

.poll-switcher {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 8px 0 16px;
}

.poll-switcher button {
  padding: 6px 12px;
  border-radius: 10px;
  border: 1px solid #cad3f7;
  background: #ffffff;
  color: #3d4a66;
}

.poll-switcher button.active {
  background: #e9edff;
  color: #2f3cb5;
  border-color: #9ba9ef;
}
</style>
