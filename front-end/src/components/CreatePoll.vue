<!-- src/components/CreatePoll.vue -->
<template>
  <div>
    <h2>创建投票活动</h2>

    <div>
      <label>标题：</label>
      <input v-model="form.title" placeholder="输入投票标题" />
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
import axios from 'axios'

const form = ref({
  title: '',
  type: 'SINGLE'
})

const optionsText = ref('')

const submit = async () => {
  const options = optionsText.value.split('\n').map(s => s.trim()).filter(Boolean)

  try {
    const res = await axios.post('/api/votes/polls', {
      title: form.value.title,
      type: form.value.type,
      options: options
    })
    alert('创建成功！ID：' + res.data.id)
  } catch (e) {
    alert('创建失败')
    console.error(e)
  }
}
</script>