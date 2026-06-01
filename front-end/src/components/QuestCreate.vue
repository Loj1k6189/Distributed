<template>
  <div class="quest-create">
    <h2>创建问卷</h2>
    <form class="create-form" @submit.prevent="submit">
      <div class="form-row">
        <label>创建者 ID</label>
        <input v-model="form.userId" placeholder="请输入创建者 ID" required />
      </div>

      <div class="form-row">
        <label>问卷标题</label>
        <input v-model="form.title" required maxlength="128" />
      </div>

      <div class="form-row">
        <label>问卷描述</label>
        <textarea v-model="form.description" rows="3" />
      </div>

      <div class="form-grid">
        <div class="form-row">
          <label>最大提交次数</label>
          <input v-model.number="form.maxSubmissions" type="number" min="1" placeholder="留空表示不限制" />
        </div>

        <div class="form-row checkbox-row">
          <label>
            <input type="checkbox" v-model="form.allowAnonymous" />
            允许匿名提交
          </label>
        </div>

        <div class="form-row">
          <label>开始时间</label>
          <input v-model="form.startTime" type="datetime-local" />
        </div>

        <div class="form-row">
          <label>结束时间</label>
          <input v-model="form.endTime" type="datetime-local" />
        </div>
      </div>

      <div class="section-header">
        <h3>题目设置</h3>
        <button type="button" class="add-btn" @click="addQuestion">+ 添加题目</button>
      </div>

      <div
        v-for="(question, index) in form.questions"
        :key="question.key"
        class="question-card"
      >
        <div class="question-head">
          <h4>题目 {{ index + 1 }}</h4>
          <button
            type="button"
            class="danger-btn"
            @click="removeQuestion(index)"
            :disabled="form.questions.length === 1"
          >
            删除题目
          </button>
        </div>

        <div class="form-row">
          <label>题目内容</label>
          <input v-model="question.content" placeholder="请输入题目内容" required />
        </div>

        <div class="form-grid">
          <div class="form-row">
            <label>题目类型</label>
            <select v-model="question.questionType">
              <option value="SINGLE_CHOICE">单选题</option>
              <option value="MULTIPLE_CHOICE">多选题</option>
              <option value="TEXT_ANSWER">问答题</option>
              <option value="RATING">评分题</option>
              <option value="DATE">日期题</option>
            </select>
          </div>

          <div class="form-row checkbox-row">
            <label>
              <input type="checkbox" v-model="question.isRequired" />
              必答
            </label>
          </div>
        </div>

        <div v-if="hasOptions(question)" class="form-row">
          <label>选项</label>
          <div v-for="(option, optionIndex) in question.options" :key="option.key" class="option-row">
            <input
              v-model="option.content"
              placeholder="输入选项内容"
            />
            <button
              type="button"
              @click="removeOption(index, optionIndex)"
              :disabled="question.options.length <= 2"
            >
              删除
            </button>
          </div>
          <button type="button" class="add-option-btn" @click="addOption(index)">+ 添加选项</button>
        </div>
      </div>

      <p v-if="error" class="error-msg">{{ error }}</p>
      <button type="submit" :disabled="loading">{{ loading ? '创建中...' : '创建问卷' }}</button>
    </form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { http } from '../lib/http'

const router = useRouter()

const createOption = () => ({
  key: `${Date.now()}-${Math.random()}`,
  content: ''
})

const createQuestion = () => ({
  key: `${Date.now()}-${Math.random()}`,
  content: '',
  questionType: 'SINGLE_CHOICE',
  isRequired: true,
  options: [createOption(), createOption()]
})

const form = ref({
  userId: '',
  title: '',
  description: '',
  maxSubmissions: null,
  allowAnonymous: false,
  startTime: '',
  endTime: '',
  questions: [createQuestion()]
})
const loading = ref(false)
const error = ref('')

const hasOptions = (question) => {
  return question.questionType === 'SINGLE_CHOICE' || question.questionType === 'MULTIPLE_CHOICE'
}

const addQuestion = () => {
  form.value.questions.push(createQuestion())
}

const removeQuestion = (index) => {
  if (form.value.questions.length === 1) return
  form.value.questions.splice(index, 1)
}

const addOption = (questionIndex) => {
  form.value.questions[questionIndex].options.push(createOption())
}

const removeOption = (questionIndex, optionIndex) => {
  const question = form.value.questions[questionIndex]
  if (question.options.length <= 2) return
  question.options.splice(optionIndex, 1)
}

const normalizeDateTime = (value) => {
  if (!value) return null
  return value.length === 16 ? `${value}:00` : value
}

const buildPayload = () => {
  return {
    title: form.value.title.trim(),
    description: form.value.description.trim() || null,
    maxSubmissions: form.value.maxSubmissions ? Number(form.value.maxSubmissions) : null,
    allowAnonymous: Boolean(form.value.allowAnonymous),
    startTime: normalizeDateTime(form.value.startTime),
    endTime: normalizeDateTime(form.value.endTime),
    questions: form.value.questions.map((question, index) => {
      const payload = {
        content: question.content.trim(),
        questionType: question.questionType,
        sortOrder: index,
        isRequired: Boolean(question.isRequired)
      }

      if (hasOptions(question)) {
        payload.options = question.options
          .map((option, optionIndex) => ({
            content: option.content.trim(),
            sortOrder: optionIndex
          }))
          .filter((option) => option.content)
      }

      return payload
    })
  }
}

const validateForm = () => {
  if (!form.value.userId.trim()) return '请输入创建者 ID'
  if (!form.value.title.trim()) return '请输入问卷标题'
  if (form.value.questions.length === 0) return '请至少添加一个题目'

  for (const [index, question] of form.value.questions.entries()) {
    if (!question.content.trim()) return `题目 ${index + 1} 不能为空`
    if (hasOptions(question)) {
      const validOptions = question.options.filter((option) => option.content.trim())
      if (validOptions.length < 2) return `题目 ${index + 1} 至少需要两个选项`
    }
  }

  if (form.value.startTime && form.value.endTime && form.value.startTime > form.value.endTime) {
    return '结束时间不能早于开始时间'
  }

  return ''
}

const resetForm = () => {
  form.value = {
    userId: form.value.userId,
    title: '',
    description: '',
    maxSubmissions: null,
    allowAnonymous: false,
    startTime: '',
    endTime: '',
    questions: [createQuestion()]
  }
}

const submit = async () => {
  error.value = validateForm()
  if (error.value) return

  loading.value = true
  try {
    const res = await http.post('/api/questionnaires', buildPayload(), {
      headers: {
        'X-User-Id': form.value.userId.trim()
      }
    })
    const questionnaireId = res?.data?.id
    alert('问卷创建成功！ID：' + questionnaireId)
    resetForm()
    if (questionnaireId) {
      router.push(`/quest/submit/${questionnaireId}`)
    }
  } catch (e) {
    error.value = '创建失败：' + (e.response?.data?.message || e.message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.quest-create {
  max-width: 820px;
}

.create-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.form-row {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 6px;
  margin-bottom: 14px;
}

.form-row :deep(input),
.form-row :deep(textarea),
.form-row :deep(select) {
  max-width: 100%;
}

.checkbox-row label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.section-header,
.question-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.section-header {
  margin-top: 8px;
}

.question-card {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;
}

.question-head h4,
.section-header h3 {
  margin: 0;
}

.option-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}

.add-option-btn,
.add-btn {
  width: fit-content;
}

.danger-btn {
  background: #fff1f0;
  color: #c62828;
  border: 1px solid #ffcdd2;
}

.error-msg {
  color: #c62828;
  background: #ffebee;
  padding: 12px;
  border-radius: 4px;
}
</style>