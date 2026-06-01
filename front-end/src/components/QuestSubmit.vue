<template>
  <div class="quest-submit">
    <h2>参与问卷</h2>

    <div class="toolbar">
      <label for="questionnaire-select">选择问卷</label>
      <select id="questionnaire-select" v-model="selectedId" @change="handleSelectionChange">
        <option value="">请选择问卷</option>
        <option v-for="item in questionnaires" :key="item.id" :value="String(item.id)">
          {{ item.title }}
        </option>
      </select>
      <button type="button" @click="loadQuestionnaires" :disabled="listLoading">刷新列表</button>
    </div>

    <div v-if="listError" class="error-msg">{{ listError }}</div>
    <div v-if="listLoading && questionnaires.length === 0" class="loading">加载问卷列表中...</div>
    <div v-else-if="questionnaires.length === 0" class="empty">暂无可参与的问卷</div>
    <div v-else-if="!selectedId" class="empty">请选择一个问卷后参与</div>
    <div v-else-if="detailLoading" class="loading">加载问卷详情中...</div>
    <div v-else-if="detailError" class="error-msg">{{ detailError }}</div>
    <div v-else-if="questionnaire">
      <h3>{{ questionnaire.title }}</h3>
      <p class="description">{{ questionnaire.description || '（无描述）' }}</p>

      <div class="field">
        <label for="quest-user-id">用户ID</label>
        <input id="quest-user-id" v-model="userId" placeholder="请输入用户ID" />
      </div>

      <div
        v-for="question in questionnaire.questions || []"
        :key="question.id"
        class="question-card"
      >
        <div class="question-title">
          {{ question.content }}
          <span v-if="question.isRequired" class="required">*</span>
        </div>

        <div v-if="question.questionType === 'SINGLE_CHOICE'" class="options">
          <label v-for="option in question.options || []" :key="option.id" class="option-item">
            <input
              v-model="answers[question.id]"
              type="radio"
              :name="`question-${question.id}`"
              :value="String(option.id)"
            />
            {{ option.content }}
          </label>
        </div>

        <div v-else-if="question.questionType === 'MULTIPLE_CHOICE'" class="options">
          <label v-for="option in question.options || []" :key="option.id" class="option-item">
            <input
              v-model="answers[question.id]"
              type="checkbox"
              :value="String(option.id)"
            />
            {{ option.content }}
          </label>
        </div>

        <textarea
          v-else-if="question.questionType === 'TEXT_ANSWER'"
          v-model="answers[question.id]"
          rows="4"
          placeholder="请输入您的回答"
        />

        <input
          v-else-if="question.questionType === 'DATE'"
          v-model="answers[question.id]"
          type="date"
        />

        <input
          v-else
          v-model="answers[question.id]"
          type="number"
          min="0"
          step="1"
          placeholder="请输入数值"
        />
      </div>

      <button type="button" class="submit-btn" @click="submit" :disabled="submitting">
        {{ submitting ? '提交中...' : '提交问卷' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { http } from '../lib/http'

const route = useRoute()
const router = useRouter()

const questionnaires = ref([])
const selectedId = ref(route.params.id ? String(route.params.id) : '')
const questionnaire = ref(null)
const answers = ref({})
const userId = ref('')
const listLoading = ref(false)
const detailLoading = ref(false)
const submitting = ref(false)
const listError = ref('')
const detailError = ref('')
const startedAt = ref(Date.now())

const unwrapData = (payload) => payload?.data ?? payload

const resetAnswers = (questions = []) => {
  const nextAnswers = {}
  for (const question of questions) {
    if (question.questionType === 'MULTIPLE_CHOICE') {
      nextAnswers[question.id] = []
    } else {
      nextAnswers[question.id] = ''
    }
  }
  answers.value = nextAnswers
}

const loadQuestionnaire = async (id) => {
  if (!id) {
    questionnaire.value = null
    resetAnswers()
    return
  }

  detailLoading.value = true
  detailError.value = ''
  try {
    const payload = await http.get(`/api/questionnaires/${id}`)
    const data = unwrapData(payload)
    questionnaire.value = data
    resetAnswers(data?.questions || [])
    startedAt.value = Date.now()
  } catch (e) {
    questionnaire.value = null
    detailError.value = '加载问卷详情失败：' + (e.response?.data?.message || e.message)
  } finally {
    detailLoading.value = false
  }
}

const syncSelection = async (forceCurrent = false) => {
  if (questionnaires.value.length === 0) {
    selectedId.value = ''
    questionnaire.value = null
    return
  }

  const routeId = route.params.id ? String(route.params.id) : ''
  const currentId = forceCurrent ? selectedId.value : routeId || selectedId.value
  const exists = questionnaires.value.some((item) => String(item.id) === currentId)
  const nextId = exists ? currentId : String(questionnaires.value[0].id)

  if (String(route.params.id || '') !== nextId) {
    await router.replace(`/quest/submit/${nextId}`)
    return
  }

  selectedId.value = nextId
  await loadQuestionnaire(nextId)
}

const loadQuestionnaires = async () => {
  listLoading.value = true
  listError.value = ''
  try {
    const payload = await http.get('/api/questionnaires/active')
    const data = unwrapData(payload)
    questionnaires.value = Array.isArray(data) ? data : []
    await syncSelection()
  } catch (e) {
    questionnaires.value = []
    listError.value = '加载问卷列表失败：' + (e.response?.data?.message || e.message)
  } finally {
    listLoading.value = false
  }
}

const handleSelectionChange = async () => {
  if (!selectedId.value) {
    await router.replace('/quest/submit')
    questionnaire.value = null
    resetAnswers()
    return
  }
  await router.replace(`/quest/submit/${selectedId.value}`)
}

const buildAnswerPayload = (question) => {
  const value = answers.value[question.id]

  if (question.questionType === 'SINGLE_CHOICE') {
    const selectedOptionIds = value ? [Number(value)] : []
    return { questionId: Number(question.id), selectedOptionIds }
  }

  if (question.questionType === 'MULTIPLE_CHOICE') {
    const selectedOptionIds = Array.isArray(value) ? value.map((item) => Number(item)) : []
    return { questionId: Number(question.id), selectedOptionIds }
  }

  return {
    questionId: Number(question.id),
    textAnswer: typeof value === 'string' ? value.trim() : String(value ?? '').trim()
  }
}

const validateQuestion = (question, answer) => {
  if (!question.isRequired) return true

  if (question.questionType === 'SINGLE_CHOICE' || question.questionType === 'MULTIPLE_CHOICE') {
    return Array.isArray(answer.selectedOptionIds) && answer.selectedOptionIds.length > 0
  }

  return Boolean(answer.textAnswer)
}

const submit = async () => {
  if (!questionnaire.value) {
    alert('请先选择问卷')
    return
  }

  if (!userId.value.trim()) {
    alert('请输入用户ID')
    return
  }

  const payloadAnswers = (questionnaire.value.questions || []).map(buildAnswerPayload)
  const invalidQuestion = (questionnaire.value.questions || []).find((question, index) => {
    return !validateQuestion(question, payloadAnswers[index])
  })

  if (invalidQuestion) {
    alert(`请完成必答题：${invalidQuestion.content}`)
    return
  }

  submitting.value = true
  try {
    await http.post(
      '/api/questionnaires/submit',
      {
        questionnaireId: Number(questionnaire.value.id),
        isAnonymous: false,
        startTime: startedAt.value,
        answers: payloadAnswers
      },
      {
        headers: { 'X-User-Id': userId.value.trim() },
        idempotencyKey: `questionnaire-${questionnaire.value.id}-${userId.value.trim()}`
      }
    )
    alert('问卷提交成功')
    await loadQuestionnaire(selectedId.value)
  } catch (e) {
    alert('提交失败：' + (e.response?.data?.message || e.message))
  } finally {
    submitting.value = false
  }
}

watch(
  () => route.params.id,
  async (id) => {
    selectedId.value = id ? String(id) : ''
    if (!selectedId.value) {
      questionnaire.value = null
      resetAnswers()
      return
    }
    await loadQuestionnaire(selectedId.value)
  }
)

onMounted(loadQuestionnaires)
</script>

<style scoped>
.quest-submit {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.toolbar select,
.field input,
textarea {
  min-width: 240px;
  padding: 8px 10px;
}

.loading,
.empty {
  color: #666;
}

.description {
  color: #666;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.question-card {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 16px;
  margin-top: 12px;
}

.question-title {
  margin-bottom: 12px;
  font-weight: 600;
}

.required {
  color: #c62828;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-item {
  display: flex;
  gap: 8px;
  align-items: center;
}

.submit-btn {
  width: fit-content;
}

.error-msg {
  color: #c62828;
  background: #ffebee;
  padding: 12px;
  border-radius: 4px;
}
</style>