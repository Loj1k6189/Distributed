<template>
  <div>
    <h1>现场活动系统</h1>
    <nav style="margin: 20px 0">
      <!-- 投票系统 -->
      <strong>投票：</strong>
      <router-link to="/create">创建投票</router-link> |
      <router-link to="/vote/1">参与投票</router-link> |
      <router-link to="/result/1">查看结果</router-link> |
      <router-link to="/admin">投票管理</router-link> |
      
      <!-- 问卷系统 -->
      <strong>问卷：</strong>
      <router-link to="/quest/create">创建问卷</router-link> |
      <router-link to="/quest/list">问卷列表</router-link> |
      
      <!-- 抽奖系统 -->
      <strong>抽奖：</strong>
      <router-link to="/lottery/draw">执行抽奖</router-link>
    </nav>
    
    <router-view />
  </div>
</template>