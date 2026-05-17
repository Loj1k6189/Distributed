<template>
  <div class="quest-submit">
    <h2>参与问卷</h2>
    <div v-if="loading">加载中...</div>
    <div v-else>
      <h3>{{ quest.title }}</h3>
      <p>{{ quest.description }}</p>
      
      <div style="margin: 10px 0">
        <label>用户ID</label>
        <input v-model="userId" />
      </div>
      
      <div v-if="alreadySubmitted" class="warning">
        您已提交过此问卷，无法再次提交
      </div>
      <div v-else>
        <div v-if="quest.allowMultiple">
          <div v-for="opt in quest.options" :key="opt.id">
            <label>
              <input
                type="checkbox"
                v-model="selectedIds"
                :value="opt.id"
              />
              {{ opt.optionValue }}
            </label>
          </div>
        </div>
        <div v-else>
          <div v-for="opt in quest.options" :key="opt.id">
            <label>
              <input
                type="radio"
                v-model="selectedSingle"
                :value="opt.id"
              />
              {{ opt.optionValue }}
            </label>
          </div>
        </div>
        
        <button @click="submit" :disabled="submitting">提交问卷</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const questionId = route.params.id

const quest = ref({ title: '', description: '', allowMultiple: false, options: [] })
const userId = ref('')
const selectedIds = ref([])
const selectedSingle = ref(null)
const loading = ref(true)
const submitting = ref(false)
const alreadySubmitted = ref(false)

onMounted(async () => {
  try {
    const res = await axios.get(`/api/v1/question/${questionId}`)
    quest.value = res.data
  } catch (e) {
    alert('加载问卷失败')
  } finally {
    loading.value = false
  }
})

const checkSubmitted = async () => {
  if (!userId.value.trim()) return
  try {
    const res = await axios.get(
      `/api/v1/question/submitted?userId=${userId.value}&questionId=${questionId}`
    )
    alreadySubmitted.value = res.data.submitted
  } catch (e) {
    console.error(e)
  }
}

const getOptionIds = () => {
  return quest.value.allowMultiple ? selectedIds.value : [selectedSingle.value]
}

const submit = async () => {
  if (!userId.value.trim()) {
    alert('请输入用户ID')
    return
  }
  const optionIds = getOptionIds()
  if (optionIds.length === 0) {
    alert('请至少选择一个选项')
    return
  }
  
  submitting.value = true
  try {
    await axios.post('/api/v1/question/submit', {
      questionId: Number(questionId),
      userId: userId.value.trim(),
      optionIds
    })
    alert('问卷提交成功')
    alreadySubmitted.value = true
  } catch (e) {
    alert('提交失败：' + (e.response?.data?.message || e.message))
  } finally {
    submitting.value = false
  }
}
</script>