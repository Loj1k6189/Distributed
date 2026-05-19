<template>
  <div class="lottery-join">
    <h2>参与抽奖</h2>
    <div>
      <input v-model="userId" placeholder="请输入用户ID" />
      <button @click="join" :disabled="joining">参与抽奖</button>
      <p v-if="result" class="success">{{ result }}</p>
      <p v-if="error" class="error">{{ error }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { http } from '../lib/http'

const route = useRoute()
const activityId = route.params.activityId

const userId = ref('')
const joining = ref(false)
const result = ref('')
const error = ref('')

const join = async () => {
  if (!userId.value.trim()) {
    error.value = '请输入用户ID'
    return
  }
  
  joining.value = true
  error.value = ''
  const prevResult = result.value
  // 乐观更新：先展示参与成功，后端失败则回滚
  result.value = '已加入抽奖池（等待确认）'
  try {
    await http.post(`/api/lottery/${activityId}/join?userId=${encodeURIComponent(userId.value.trim())}`, null, { idempotencyKey: `${activityId}-${userId.value.trim()}` })
    result.value = '参与成功！已加入抽奖池'
    userId.value = ''
  } catch (e) {
    result.value = prevResult
    error.value = '参与失败：' + (e.response?.data?.message || e.message)
  } finally {
    joining.value = false
  }
}
</script>