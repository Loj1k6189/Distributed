<!-- src/components/CreatePoll.vue -->
<template>
  <div>
    <h2>创建投票活动</h2>

    <div>
      <label>标题：</label>
      <input v-model="form.name" placeholder="输入投票标题" />
    </div>

    <div style="margin: 10px 0">
      <label>选项（每行一个）：</label>
      <textarea v-model="optionsText" rows="4" placeholder="选项1&#10;选项2&#10;选项3"></textarea>
    </div>

    <div>
      <label>类型：</label>
      <select v-model="form.type">
        <option value="SINGLE">单选</option>
        <option value="MULTIPLE">多选</option>
      </select>
    </div>

    <button @click="submit" style="margin-top:10px">创建投票</button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const form = ref({
  name: '',
  type: 'SINGLE'
})

const optionsText = ref('')

const submit = async () => {
  const options = optionsText.value.split('\n').map(s => s.trim()).filter(Boolean)

  try {
    const res = await axios.post('/api/votes/polls', {
      name: form.value.name,
      allowMultiple: form.value.type === 'MULTIPLE',
      options
    })
    const pollId = res.data.pollId
    alert('创建成功！ID：' + pollId)
    router.push(`/vote/${pollId}`)
  } catch (e) {
    alert('创建失败：' + (e.response?.data?.message || e.message))
    console.error(e)
  }
}
</script>