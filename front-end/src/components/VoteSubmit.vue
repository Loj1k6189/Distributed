<template>
  <div class="vote-submit">
    <h2>参与投票</h2>

    <div v-if="loadingPolls">加载投票列表中...</div>
    <div v-else-if="activePolls.length === 0" class="warning">当前没有进行中的投票</div>
    <div v-else>
      <p class="poll-meta">
        当前共有 <strong>{{ activePolls.length }}</strong> 个进行中的投票
      </p>

      <div class="poll-switcher">
        <button
          v-for="pollItem in activePolls"
          :key="pollItem.pollId"
          type="button"
          :class="{ active: selectedPollId === pollItem.pollId }"
          @click="switchPoll(pollItem.pollId)"
        >
          #{{ pollItem.pollId }} {{ pollItem.name }}
        </button>
      </div>

      <div v-if="loadingDetail">加载投票详情中...</div>
      <div v-else-if="poll">
        <h3>{{ poll.name || `投票活动 #${poll.pollId}` }}</h3>
        <p>投票模式：{{ poll.allowMultiple ? '多选' : '单选' }}</p>

        <div class="form-row">
          <label>投票人 ID</label>
          <input v-model="voterId" placeholder="输入 voterId" @blur="checkSubmitted" />
        </div>

        <p v-if="alreadySubmitted" class="warning">该用户已投过该活动，不能重复投票</p>

        <div v-if="poll.allowMultiple">
          <label v-for="opt in poll.options" :key="opt.optionId" class="option-row">
            <input
              type="checkbox"
              v-model="selectedMulti"
              :value="opt.optionId"
              :disabled="alreadySubmitted"
            />
            {{ opt.optionText }}
          </label>
        </div>
        <div v-else>
          <label v-for="opt in poll.options" :key="opt.optionId" class="option-row">
            <input
              type="radio"
              v-model="selectedSingle"
              :value="opt.optionId"
              name="vote-option"
              :disabled="alreadySubmitted"
            />
            {{ opt.optionText }}
          </label>
        </div>

        <button @click="submitVote" :disabled="submitting || alreadySubmitted" class="submit-btn">
          {{ submitting ? '提交中...' : '提交投票' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
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

type PollDetail = {
  pollId: number
  name: string
  allowMultiple: boolean
  ballots: number
  options: PollOption[]
}

const route = useRoute()

const activePolls = ref<PollSummary[]>([])
const selectedPollId = ref<number | null>(null)
const poll = ref<PollDetail | null>(null)
const voterId = ref('')
const selectedSingle = ref<number | null>(null)
const selectedMulti = ref<number[]>([])
const loadingPolls = ref(true)
const loadingDetail = ref(false)
const submitting = ref(false)
const alreadySubmitted = ref(false)

const optionIds = computed<number[]>(() => {
  if (!poll.value) return []
  if (poll.value.allowMultiple) return selectedMulti.value
  return selectedSingle.value ? [selectedSingle.value] : []
})

const resolvePollIdFromRoute = () => {
  const paramId = Number(route.params.id)
  if (Number.isFinite(paramId) && paramId > 0) return paramId
  return null
}

const loadPollDetail = async (pollId: number) => {
  loadingDetail.value = true
  selectedSingle.value = null
  selectedMulti.value = []
  alreadySubmitted.value = false
  try {
    const res = await http.get(`/api/votes/polls/${pollId}/result`)
    poll.value = res
    await checkSubmitted()
  } catch (e: any) {
    alert('加载投票信息失败：' + (e.response?.data?.message || e.message))
  } finally {
    loadingDetail.value = false
  }
}

const loadActivePolls = async () => {
  loadingPolls.value = true
  try {
    const res = await http.get('/api/votes/polls?activeOnly=true')
    activePolls.value = Array.isArray(res) ? res : []
    if (activePolls.value.length === 0) {
      selectedPollId.value = null
      poll.value = null
      return
    }
    const expected = resolvePollIdFromRoute()
    const matched = expected && activePolls.value.some((item) => item.pollId === expected)
      ? expected
      : activePolls.value[0].pollId
    selectedPollId.value = matched
    await loadPollDetail(matched)
  } catch (e: any) {
    alert('加载进行中投票失败：' + (e.response?.data?.message || e.message))
  } finally {
    loadingPolls.value = false
  }
}

const switchPoll = async (pollId: number) => {
  if (selectedPollId.value === pollId) return
  selectedPollId.value = pollId
  await loadPollDetail(pollId)
}

const checkSubmitted = async () => {
  const currentPollId = selectedPollId.value
  const currentVoter = voterId.value.trim()
  if (!currentPollId || !currentVoter) {
    alreadySubmitted.value = false
    return
  }
  try {
    const res = await http.get(`/api/votes/polls/${currentPollId}/submitted?voterId=${encodeURIComponent(currentVoter)}`)
    alreadySubmitted.value = Boolean(res?.submitted)
  } catch (e: any) {
    alreadySubmitted.value = false
    alert('校验投票状态失败：' + (e.response?.data?.message || e.message))
  }
}

const submitVote = async () => {
  const currentPollId = selectedPollId.value
  const currentVoter = voterId.value.trim()
  if (!currentPollId) {
    alert('请选择投票活动')
    return
  }
  if (!currentVoter) {
    alert('请输入 voterId')
    return
  }
  await checkSubmitted()
  if (alreadySubmitted.value) {
    alert('该用户已参与当前投票，不能重复投票')
    return
  }
  if (optionIds.value.length === 0) {
    alert('请至少选择一个选项')
    return
  }

  submitting.value = true
  // 乐观更新：本地先增加票数并标记已投
  const prevPoll = poll.value ? JSON.parse(JSON.stringify(poll.value)) : null
  try {
    if (poll.value) {
      // 增加总票数
      poll.value.ballots = (poll.value.ballots ?? 0) + optionIds.value.length
      // 增加各选项票数
      for (const optId of optionIds.value) {
        const opt = poll.value.options.find(o => o.optionId === optId)
        if (opt) opt.votes = (opt.votes ?? 0) + 1
      }
    }
    alreadySubmitted.value = true

    await http.post('/api/votes/submit', {
      pollId: currentPollId,
      voterId: currentVoter,
      optionIds: optionIds.value
    }, { idempotencyKey: `${currentPollId}-${currentVoter}` })

    alert('投票成功！')
    // 后端确认后刷新详情以保证最终一致性
    await loadPollDetail(currentPollId)
  } catch (e: any) {
    // 回滚本地状态
    if (prevPoll) poll.value = prevPoll
    alreadySubmitted.value = false
    alert('投票失败：' + (e.response?.data?.message || e.message))
  } finally {
    submitting.value = false
  }
}

onMounted(loadActivePolls)
</script>

<style scoped>
.vote-submit {
  max-width: 720px;
}

.poll-meta {
  margin: 4px 0 10px;
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

.form-row {
  max-width: 420px;
  margin: 10px 0 14px;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.submit-btn {
  margin-top: 8px;
}
</style>
