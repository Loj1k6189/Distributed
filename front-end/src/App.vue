<!-- src/App.vue -->
<template>
  <div>
    <h1>现场投票系统</h1>
    <nav style="margin: 20px 0">
      <a href="/create">创建投票</a> |
      <a href="/vote/1">去投票（测试ID=1）</a> |
      <a href="/result/1">查看结果（测试ID=1）</a>
    </nav>

    <!-- 路由页面展示在这里 -->
    <router-view />
  </div>
</template>

<script setup>
</script>