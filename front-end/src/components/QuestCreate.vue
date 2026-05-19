<template>
  <div class="quest-create">
    <h2>创建问卷</h2>
    <form class="create-form" @submit.prevent="submit">
      <div class="form-row">
        <label>问卷标题</label>
        <input v-model="form.title" required maxlength="128" />
      </div>
      
      <div class="form-row">
        <label>问卷描述</label>
        <textarea v-model="form.description" rows="3"></textarea>
      </div>
      
      <div class="form-row checkbox-row">
        <label>
          <input type="checkbox" v-model="form.allowMultiple" />
          允许多选
        </label>
      </div>
      
      <div class="form-row">
        <label>选项</label>
        <div v-for="(opt, idx) in form.options" :key="idx" class="option-row">
          <input v-model="form.options[idx]" placeholder="输入选项" />
          <button type="button" @click="removeOption(idx)">删除</button>
        </div>
        <button type="button" class="add-option-btn" @click="addOption">+ 添加选项</button>
      </div>
      
      <button type="submit" :disabled="loading">创建</button>
    </form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'

const form = ref({
  title: '',
  description: '',
  allowMultiple: false,
  options: ['', '']
})
const loading = ref(false)

const addOption = () => {
  form.value.options.push('')
}

const removeOption = (idx) => {
  form.value.options.splice(idx, 1)
}

const submit = async () => {
  loading.value = true
  try {
    const res = await axios.post('/api/v1/question', {
      title: form.value.title,
      description: form.value.description,
      allowMultiple: form.value.allowMultiple,
      options: form.value.options.filter(o => o.trim())
    })
    alert('问卷创建成功！ID：' + res.data.id)
    // 重置表单或跳转
  } catch (e) {
    alert('创建失败：' + (e.response?.data?.message || e.message))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.quest-create {
  max-width: 560px;
}

.create-form {
  display: flex;
  flex-direction: column;
  gap: 4px;
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

.option-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}

.add-option-btn {
  width: fit-content;
}
</style>