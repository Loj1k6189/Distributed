<!-- src/App.vue -->
<template>
  <div>
    <h1>现场投票系统</h1>
    <nav style="margin: 20px 0">
      <router-link to="/create">创建投票</router-link> |
      <router-link to="/vote/1">去投票（测试ID=1）</router-link> |
      <router-link to="/result/1">查看结果（测试ID=1）</router-link> |
      <router-link to="/admin">管理</router-link>
    </nav>
    
    <!-- 路由页面展示在这里 -->
    <router-view />
  </div>
</template>

<script setup>
</script>