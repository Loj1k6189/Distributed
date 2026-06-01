<template>
  <div class="chain-create">
    <h2>创建接龙</h2>

    <form class="create-form" @submit.prevent="submit">
      <div class="form-row">
        <label>创建者 ID</label>
        <input v-model="form.userId" placeholder="请输入创建者 ID" required />
      </div>

      <div class="form-row">
        <label>接龙标题</label>
        <input v-model="form.title" placeholder="例如：周五午餐接龙" required maxlength="200" />
      </div>

      <div class="form-row">
        <label>接龙描述</label>
        <textarea v-model="form.description" rows="4" placeholder="说明本次接龙的规则或用途" />
      </div>

      <div class="form-grid">
        <div class="form-row">
          <label>最大参与人数</label>
          <input v-model.number="form.maxParticipants" type="number" min="1" placeholder="留空表示不限制" />
        </div>

        <div class="form-row checkbox-row">
          <label>
            <input v-model="form.allowMultiple" type="checkbox" />
            允许同一用户重复参与
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

      <p v-if="error" class="error-msg">{{ error }}</p>
      <button type="submit" :disabled="loading">{{ loading ? '创建中...' : '创建接龙' }}</button>
    </form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { http } from '../lib/http'

const router = useRouter()

const form = ref({
  userId: '',
  title: '',
  description: '',
  maxParticipants: null,
  allowMultiple: false,
  startTime: '',
  endTime: ''
})
const loading = ref(false)
const error = ref('')

const normalizeDateTime = (value) => {
  if (!value) return null
  return value.length === 16 ? `${value}:00` : value
}

const validate = () => {
  if (!form.value.userId.trim()) return '请输入创建者 ID'
  if (!form.value.title.trim()) return '请输入接龙标题'
  if (form.value.startTime && form.value.endTime && form.value.startTime > form.value.endTime) {
    return '结束时间不能早于开始时间'
  }
  return ''
}

const submit = async () => {
  error.value = validate()
  if (error.value) return

  loading.value = true
  try {
    const res = await http.post(
      '/api/chains',
      {
        title: form.value.title.trim(),
        description: form.value.description.trim() || null,
        maxParticipants: form.value.maxParticipants ? Number(form.value.maxParticipants) : null,
        allowMultiple: Boolean(form.value.allowMultiple),
        startTime: normalizeDateTime(form.value.startTime),
        endTime: normalizeDateTime(form.value.endTime)
      },
      {
        headers: {
          'X-User-Id': form.value.userId.trim()
        }
      }
    )

    const chainId = res?.data?.id
    alert('接龙创建成功！ID：' + chainId)
    if (chainId) {
      router.push(`/chain/join/${chainId}`)
    }
  } catch (e) {
    error.value = '创建失败：' + (e.response?.data?.message || e.message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.chain-create {
  max-width: 720px;
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

.error-msg {
  color: #c62828;
  background: #ffebee;
  padding: 12px;
  border-radius: 4px;
}
</style>
